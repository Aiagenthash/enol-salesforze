import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import crypto from 'crypto';
import { query, withTransaction } from '../config/database';
import { authenticate } from '../middleware/auth';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';
import { WebhookDispatcher } from '../services/webhook-dispatcher.service';

export const orderRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/orders
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated list of sales orders with credit hold and status filtering',
        tags: ['Orders & Line Items'],
        querystring: {
          type: 'object',
          properties: {
            client_id: { type: 'string' },
            sales_rep_id: { type: 'string' },
            beat_id: { type: 'string' },
            status: {
              type: 'string',
              enum: ['PENDING', 'APPROVED', 'DISPATCHED', 'DELIVERED', 'CANCELLED', 'REJECTED_CREDIT_HOLD'],
            },
            credit_hold_flag: { type: 'boolean' },
            search: { type: 'string' },
            from_date: { type: 'string' },
            to_date: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const { page, limit, search } = parsePagination(request);
      const queryParams = request.query as any;

      let whereConditions = ['1=1'];
      let values: any[] = [];
      let pIdx = 1;

      if (queryParams.client_id) {
        whereConditions.push(`o.client_id = $${pIdx++}`);
        values.push(queryParams.client_id);
      }
      if (queryParams.sales_rep_id) {
        whereConditions.push(`o.sales_rep_id = $${pIdx++}`);
        values.push(queryParams.sales_rep_id);
      }
      if (queryParams.beat_id) {
        whereConditions.push(`o.beat_id = $${pIdx++}`);
        values.push(queryParams.beat_id);
      }
      if (queryParams.status) {
        whereConditions.push(`o.status = $${pIdx++}`);
        values.push(queryParams.status);
      }
      if (queryParams.credit_hold_flag !== undefined) {
        whereConditions.push(`o.credit_hold_flag = $${pIdx++}`);
        values.push(queryParams.credit_hold_flag === 'true' || queryParams.credit_hold_flag === true);
      }
      if (queryParams.from_date) {
        whereConditions.push(`o.created_at >= $${pIdx++}`);
        values.push(queryParams.from_date);
      }
      if (queryParams.to_date) {
        whereConditions.push(`o.created_at <= $${pIdx++}`);
        values.push(queryParams.to_date);
      }
      if (search) {
        whereConditions.push(`(o.order_number ILIKE $${pIdx} OR o.external_oms_reference ILIKE $${pIdx} OR c.business_name ILIKE $${pIdx})`);
        values.push(`%${search}%`);
        pIdx++;
      }

      // Hierarchy constraint: Reps only see orders they placed or their beat
      if (request.user!.role === 'SALES_REP') {
        whereConditions.push(`o.sales_rep_id = $${pIdx++}`);
        values.push(request.user!.id);
      } else if (request.user!.role === 'ASM' || request.user!.role === 'ZSM') {
        whereConditions.push(`u.hierarchy_path LIKE $${pIdx++}`);
        values.push(`${request.user!.hierarchy_path}%`);
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(
        `SELECT COUNT(*) FROM orders o 
         JOIN clients c ON c.id = o.client_id
         JOIN users u ON u.id = o.sales_rep_id
         WHERE ${whereClause}`,
        values
      );
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT o.id, o.order_number, o.client_id, c.business_name as client_name,
                o.sales_rep_id, u.full_name as sales_rep_name, o.beat_id, b.name as beat_name,
                o.total_cartons, o.total_liters, o.gross_amount, o.discount_amount, o.net_amount,
                o.payment_terms, o.credit_hold_flag, o.status, o.external_oms_reference,
                o.idempotency_key, o.notes, o.created_at, o.updated_at
         FROM orders o
         JOIN clients c ON c.id = o.client_id
         JOIN users u ON u.id = o.sales_rep_id
         LEFT JOIN territory_beats b ON b.id = o.beat_id
         WHERE ${whereClause}
         ORDER BY o.created_at DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/orders/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get order header and detailed line items',
        tags: ['Orders & Line Items'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const orderRes = await query(
        `SELECT o.id, o.order_number, o.client_id, c.business_name as client_name, c.phone as client_phone,
                c.credit_limit, c.outstanding_balance,
                o.sales_rep_id, u.full_name as sales_rep_name, o.beat_id, b.name as beat_name,
                o.total_cartons, o.total_liters, o.gross_amount, o.discount_amount, o.net_amount,
                o.payment_terms, o.credit_hold_flag, o.status, o.external_oms_reference,
                o.idempotency_key, o.notes, o.created_at, o.updated_at
         FROM orders o
         JOIN clients c ON c.id = o.client_id
         JOIN users u ON u.id = o.sales_rep_id
         LEFT JOIN territory_beats b ON b.id = o.beat_id
         WHERE o.id = $1`,
        [id]
      );

      if (orderRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Order not found' });
      }

      const itemsRes = await query(
        `SELECT oi.id, oi.order_id, oi.product_id, p.sku as product_sku, p.name as product_name,
                p.viscosity_grade, oi.cartons, oi.units, oi.pack_size_liters, oi.total_liters,
                oi.unit_price, oi.line_total, oi.scheme_discount, oi.net_line_total, oi.created_at
         FROM order_items oi
         JOIN products p ON p.id = oi.product_id
         WHERE oi.order_id = $1
         ORDER BY oi.created_at ASC`,
        [id]
      );

      return reply.send({
        success: true,
        data: {
          ...orderRes.rows[0],
          items: itemsRes.rows,
        },
      });
    }
  );

  // POST /api/v1/orders
  fastify.post(
    '/',
    {
      schema: {
        description: 'Create a new sales order with line items, idempotency check, and credit hold verification',
        tags: ['Orders & Line Items'],
        body: {
          type: 'object',
          required: ['client_id', 'items'],
          properties: {
            client_id: { type: 'string' },
            beat_id: { type: 'string', nullable: true },
            idempotency_key: { type: 'string' },
            payment_terms: {
              type: 'string',
              enum: ['CASH_ON_DELIVERY', 'CREDIT_7_DAYS', 'CREDIT_15_DAYS', 'CREDIT_30_DAYS', 'ADVANCE'],
              default: 'CREDIT_15_DAYS',
            },
            notes: { type: 'string', nullable: true },
            external_oms_reference: { type: 'string', nullable: true },
            items: {
              type: 'array',
              minItems: 1,
              items: {
                type: 'object',
                required: ['product_id'],
                properties: {
                  product_id: { type: 'string' },
                  cartons: { type: 'number', minimum: 0 },
                  units: { type: 'integer', minimum: 0 },
                  scheme_discount: { type: 'number', default: 0 },
                },
              },
            },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;
      const idempotencyKey = body.idempotency_key;

      // 1. Idempotency Key check
      if (idempotencyKey) {
        const existingOrder = await query(
          `SELECT o.*, (SELECT json_agg(oi.*) FROM order_items oi WHERE oi.order_id = o.id) as items
           FROM orders o WHERE o.idempotency_key = $1`,
          [idempotencyKey]
        );
        if (existingOrder.rows.length > 0) {
          return reply.status(200).send({
            success: true,
            message: 'Order already processed (idempotent replay)',
            data: existingOrder.rows[0],
          });
        }
      }

      const salesRepId = request.user!.id;

      // Transaction: create order + items + credit limit verification
      const createdOrder = await withTransaction(async (client) => {
        // Fetch client credit info
        const clientRes = await client.query(
          `SELECT credit_limit, outstanding_balance, credit_hold, beat_id FROM clients WHERE id = $1 FOR UPDATE`,
          [body.client_id]
        );
        if (clientRes.rows.length === 0) {
          throw new Error('Client not found');
        }

        const clientData = clientRes.rows[0];
        const beatId = body.beat_id || clientData.beat_id;

        // Generate Order Number
        const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
        const randomSuffix = crypto.randomBytes(3).toString('hex').toUpperCase();
        const orderNumber = `ORD-${dateStr}-${randomSuffix}`;

        // Insert initial Order header
        const orderInsertRes = await client.query(
          `INSERT INTO orders (
             order_number, client_id, sales_rep_id, beat_id,
             payment_terms, notes, external_oms_reference, idempotency_key, status
           ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'PENDING')
           RETURNING *`,
          [
            orderNumber,
            body.client_id,
            salesRepId,
            beatId || null,
            body.payment_terms || 'CREDIT_15_DAYS',
            body.notes || null,
            body.external_oms_reference || null,
            idempotencyKey || null,
          ]
        );

        const order = orderInsertRes.rows[0];

        // Process line items
        let sumCartons = 0;
        let sumLiters = 0;
        let sumGross = 0;
        let sumDiscounts = 0;
        let sumNet = 0;

        const insertedItems: any[] = [];

        for (const item of body.items) {
          const prodRes = await client.query(
            `SELECT sku, name, pack_size_liters, units_per_carton, trade_price FROM products WHERE id = $1`,
            [item.product_id]
          );
          if (prodRes.rows.length === 0) {
            throw new Error(`Product ${item.product_id} not found`);
          }
          const product = prodRes.rows[0];

          const packSize = parseFloat(product.pack_size_liters);
          const unitsPerCarton = parseInt(product.units_per_carton, 10);
          const tradePricePerUnit = parseFloat(product.trade_price) / unitsPerCarton;

          const cartons = parseFloat(item.cartons || 0);
          const looseUnits = parseInt(item.units || 0, 10);
          const totalUnits = cartons * unitsPerCarton + looseUnits;
          const totalLiters = totalUnits * packSize;

          const lineTotal = totalUnits * tradePricePerUnit;
          const discount = parseFloat(item.scheme_discount || 0);
          const netLineTotal = Math.max(0, lineTotal - discount);

          sumCartons += cartons + looseUnits / unitsPerCarton;
          sumLiters += totalLiters;
          sumGross += lineTotal;
          sumDiscounts += discount;
          sumNet += netLineTotal;

          const itemInsertRes = await client.query(
            `INSERT INTO order_items (
               order_id, product_id, cartons, units, pack_size_liters,
               total_liters, unit_price, line_total, scheme_discount, net_line_total
             ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
             RETURNING *`,
            [
              order.id,
              item.product_id,
              cartons,
              totalUnits,
              packSize,
              totalLiters,
              tradePricePerUnit,
              lineTotal,
              discount,
              netLineTotal,
            ]
          );
          insertedItems.push(itemInsertRes.rows[0]);
        }

        // Credit limit check
        const currentBalance = parseFloat(clientData.outstanding_balance);
        const creditLimit = parseFloat(clientData.credit_limit);
        const isCreditHold = clientData.credit_hold || currentBalance + sumNet > creditLimit;
        const finalStatus = isCreditHold ? 'REJECTED_CREDIT_HOLD' : 'PENDING';

        // Update Order totals & credit hold status
        const updatedOrderRes = await client.query(
          `UPDATE orders
           SET total_cartons = $1,
               total_liters = $2,
               gross_amount = $3,
               discount_amount = $4,
               net_amount = $5,
               credit_hold_flag = $6,
               status = $7
           WHERE id = $8
           RETURNING *`,
          [sumCartons, sumLiters, sumGross, sumDiscounts, sumNet, isCreditHold, finalStatus, order.id]
        );

        return {
          ...updatedOrderRes.rows[0],
          items: insertedItems,
        };
      });

      // Dispatch Webhook Event `order.created`
      WebhookDispatcher.dispatch('order.created', createdOrder);

      return reply.status(201).send({
        success: true,
        message: createdOrder.credit_hold_flag
          ? 'Order created but placed on CREDIT HOLD (client exceeded credit limit)'
          : 'Order placed successfully',
        data: createdOrder,
      });
    }
  );

  // PUT /api/v1/orders/:id/status
  fastify.put(
    '/:id/status',
    {
      schema: {
        description: 'Update order lifecycle status (approve, dispatch, deliver, cancel)',
        tags: ['Orders & Line Items'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          required: ['status'],
          properties: {
            status: {
              type: 'string',
              enum: ['PENDING', 'APPROVED', 'DISPATCHED', 'DELIVERED', 'CANCELLED', 'REJECTED_CREDIT_HOLD'],
            },
            notes: { type: 'string' },
            external_oms_reference: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const { status, notes, external_oms_reference } = request.body as any;

      const updatedOrder = await withTransaction(async (client) => {
        const orderRes = await client.query(`SELECT * FROM orders WHERE id = $1 FOR UPDATE`, [id]);
        if (orderRes.rows.length === 0) {
          throw new Error('Order not found');
        }

        const currentOrder = orderRes.rows[0];

        // Update order status
        const updateRes = await client.query(
          `UPDATE orders
           SET status = $1,
               notes = COALESCE($2, notes),
               external_oms_reference = COALESCE($3, external_oms_reference)
           WHERE id = $4
           RETURNING *`,
          [status, notes || null, external_oms_reference || null, id]
        );

        const updated = updateRes.rows[0];

        // If newly DELIVERED, post invoice entry to client ledger and update balance
        if (status === 'DELIVERED' && currentOrder.status !== 'DELIVERED') {
          const netAmount = parseFloat(updated.net_amount);
          const cRes = await client.query(`SELECT outstanding_balance FROM clients WHERE id = $1 FOR UPDATE`, [
            updated.client_id,
          ]);
          const newBal = parseFloat(cRes.rows[0].outstanding_balance) + netAmount;

          await client.query(`UPDATE clients SET outstanding_balance = $1 WHERE id = $2`, [newBal, updated.client_id]);

          await client.query(
            `INSERT INTO client_ledger_entries (
               client_id, entry_type, amount, balance_after,
               reference_type, reference_id, description, recorded_by
             ) VALUES ($1, 'INVOICE', $2, $3, 'ORDER', $4, $5, $6)`,
            [
              updated.client_id,
              netAmount,
              newBal,
              updated.order_number,
              `Delivery completed for order ${updated.order_number}`,
              request.user!.id,
            ]
          );
        }

        return updated;
      });

      // Dispatch Webhooks
      if (status === 'APPROVED') {
        WebhookDispatcher.dispatch('order.approved', updatedOrder);
      } else if (status === 'DELIVERED') {
        WebhookDispatcher.dispatch('order.delivered', updatedOrder);
      }

      return reply.send({
        success: true,
        message: `Order status updated to ${status}`,
        data: updatedOrder,
      });
    }
  );

  // GET /api/v1/orders/:id/items
  fastify.get(
    '/:id/items',
    {
      schema: {
        description: 'Get list of line items for a specific order',
        tags: ['Orders & Line Items'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const itemsRes = await query(
        `SELECT oi.*, p.sku as product_sku, p.name as product_name, p.category
         FROM order_items oi
         JOIN products p ON p.id = oi.product_id
         WHERE oi.order_id = $1
         ORDER BY oi.created_at ASC`,
        [id]
      );

      return reply.send({
        success: true,
        count: itemsRes.rows.length,
        items: itemsRes.rows,
      });
    }
  );
};
