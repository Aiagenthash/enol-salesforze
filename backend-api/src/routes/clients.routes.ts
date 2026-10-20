import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query, withTransaction } from '../config/database';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';
import { WebhookDispatcher } from '../services/webhook-dispatcher.service';

export const clientRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/clients
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated list of retail and distributor clients with credit and location details',
        tags: ['Clients & Ledger'],
        querystring: {
          type: 'object',
          properties: {
            beat_id: { type: 'string' },
            district_id: { type: 'string' },
            credit_hold: { type: 'boolean' },
            approval_status: { type: 'string', enum: ['PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'] },
            search: { type: 'string' },
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

      if (queryParams.beat_id) {
        whereConditions.push(`c.beat_id = $${pIdx++}`);
        values.push(queryParams.beat_id);
      }
      if (queryParams.district_id) {
        whereConditions.push(`c.district_id = $${pIdx++}`);
        values.push(queryParams.district_id);
      }
      if (queryParams.credit_hold !== undefined) {
        whereConditions.push(`c.credit_hold = $${pIdx++}`);
        values.push(queryParams.credit_hold === 'true' || queryParams.credit_hold === true);
      }
      if (queryParams.approval_status) {
        whereConditions.push(`c.approval_status = $${pIdx++}`);
        values.push(queryParams.approval_status);
      }
      if (search) {
        whereConditions.push(`(c.business_name ILIKE $${pIdx} OR c.owner_name ILIKE $${pIdx} OR c.phone ILIKE $${pIdx} OR c.cnic ILIKE $${pIdx})`);
        values.push(`%${search}%`);
        pIdx++;
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM clients c WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT c.id, c.beat_id, b.name as beat_name,
                c.district_id, d.name as district_name,
                c.business_name, c.owner_name, c.phone, c.cnic, c.address,
                c.latitude, c.longitude, c.credit_limit, c.outstanding_balance,
                (c.credit_limit - c.outstanding_balance) as available_credit,
                c.credit_hold, c.approval_status, c.created_by, u.full_name as created_by_name,
                c.created_at, c.updated_at
         FROM clients c
         JOIN pakistan_districts d ON d.id = c.district_id
         LEFT JOIN territory_beats b ON b.id = c.beat_id
         LEFT JOIN users u ON u.id = c.created_by
         WHERE ${whereClause}
         ORDER BY c.business_name ASC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/clients/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get client profile, coordinates, credit limits, and recent ledger entries',
        tags: ['Clients & Ledger'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const clientRes = await query(
        `SELECT c.id, c.beat_id, b.name as beat_name,
                c.district_id, d.name as district_name,
                c.business_name, c.owner_name, c.phone, c.cnic, c.address,
                c.latitude, c.longitude, c.credit_limit, c.outstanding_balance,
                (c.credit_limit - c.outstanding_balance) as available_credit,
                c.credit_hold, c.approval_status, c.created_by, u.full_name as created_by_name,
                c.created_at, c.updated_at
         FROM clients c
         JOIN pakistan_districts d ON d.id = c.district_id
         LEFT JOIN territory_beats b ON b.id = c.beat_id
         LEFT JOIN users u ON u.id = c.created_by
         WHERE c.id = $1`,
        [id]
      );

      if (clientRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Client not found' });
      }

      // Fetch last 5 ledger entries
      const ledgerRes = await query(
        `SELECT id, entry_type, amount, balance_after, reference_type, reference_id, description, created_at
         FROM client_ledger_entries
         WHERE client_id = $1
         ORDER BY created_at DESC
         LIMIT 5`,
        [id]
      );

      return reply.send({
        success: true,
        data: {
          ...clientRes.rows[0],
          recent_ledger: ledgerRes.rows,
        },
      });
    }
  );

  // POST /api/v1/clients
  fastify.post(
    '/',
    {
      schema: {
        description: 'Register a new retail or workshop client',
        tags: ['Clients & Ledger'],
        body: {
          type: 'object',
          required: ['district_id', 'business_name', 'owner_name', 'phone', 'address', 'latitude', 'longitude'],
          properties: {
            district_id: { type: 'string' },
            beat_id: { type: 'string', nullable: true },
            business_name: { type: 'string' },
            owner_name: { type: 'string' },
            phone: { type: 'string' },
            cnic: { type: 'string', nullable: true },
            address: { type: 'string' },
            latitude: { type: 'number' },
            longitude: { type: 'number' },
            credit_limit: { type: 'number', default: 100000.00 },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;

      const result = await query(
        `INSERT INTO clients (
           district_id, beat_id, business_name, owner_name, phone, cnic,
           address, latitude, longitude, credit_limit, created_by
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
         RETURNING id, district_id, beat_id, business_name, owner_name, phone, cnic,
                   address, latitude, longitude, credit_limit, outstanding_balance,
                   credit_hold, approval_status, created_at`,
        [
          body.district_id,
          body.beat_id || null,
          body.business_name,
          body.owner_name,
          body.phone,
          body.cnic || null,
          body.address,
          body.latitude,
          body.longitude,
          body.credit_limit || 100000.00,
          request.user!.id,
        ]
      );

      const client = result.rows[0];

      // Dispatch Webhook event
      WebhookDispatcher.dispatch('client.created', client);

      return reply.status(201).send({
        success: true,
        message: 'Client created successfully',
        data: client,
      });
    }
  );

  // PUT /api/v1/clients/:id
  fastify.put(
    '/:id',
    {
      schema: {
        description: 'Update client details, coordinates, or credit terms',
        tags: ['Clients & Ledger'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          properties: {
            beat_id: { type: 'string', nullable: true },
            business_name: { type: 'string' },
            owner_name: { type: 'string' },
            phone: { type: 'string' },
            cnic: { type: 'string', nullable: true },
            address: { type: 'string' },
            latitude: { type: 'number' },
            longitude: { type: 'number' },
            credit_limit: { type: 'number' },
            credit_hold: { type: 'boolean' },
            approval_status: { type: 'string', enum: ['PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'] },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const body = request.body as any;

      const fields: string[] = [];
      const values: any[] = [];
      let pIdx = 1;

      for (const [key, val] of Object.entries(body)) {
        if (val !== undefined && key !== 'id') {
          fields.push(`${key} = $${pIdx++}`);
          values.push(val);
        }
      }

      if (fields.length === 0) {
        return reply.status(400).send({ success: false, error: 'No fields provided for update' });
      }

      values.push(id);
      const sql = `UPDATE clients SET ${fields.join(', ')} WHERE id = $${pIdx} RETURNING *`;

      const result = await query(sql, values);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Client not found' });
      }

      return reply.send({ success: true, message: 'Client updated successfully', data: result.rows[0] });
    }
  );

  // GET /api/v1/clients/:id/ledger
  fastify.get(
    '/:id/ledger',
    {
      schema: {
        description: 'Get complete statement of account ledger entries for a client',
        tags: ['Clients & Ledger'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const { page, limit } = parsePagination(request);

      const countRes = await query(`SELECT COUNT(*) FROM client_ledger_entries WHERE client_id = $1`, [id]);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const ledgerRes = await query(
        `SELECT l.id, l.client_id, l.entry_type, l.amount, l.balance_after,
                l.reference_type, l.reference_id, l.description,
                l.recorded_by, u.full_name as recorded_by_name, l.created_at
         FROM client_ledger_entries l
         LEFT JOIN users u ON u.id = l.recorded_by
         WHERE l.client_id = $1
         ORDER BY l.created_at DESC
         LIMIT $2 OFFSET $3`,
        [id, limit, offset]
      );

      return reply.send(formatPaginatedResponse(ledgerRes.rows, total, page, limit));
    }
  );

  // POST /api/v1/clients/:id/ledger
  fastify.post(
    '/:id/ledger',
    {
      schema: {
        description: 'Post a payment, invoice, or credit adjustment to client ledger and update outstanding balance',
        tags: ['Clients & Ledger'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          required: ['entry_type', 'amount'],
          properties: {
            entry_type: { type: 'string', enum: ['INVOICE', 'PAYMENT', 'CREDIT_NOTE', 'DEBIT_NOTE', 'ADJUSTMENT'] },
            amount: { type: 'number', minimum: 0.01 },
            reference_type: { type: 'string', enum: ['ORDER', 'RECOVERY_SLIP', 'BANK_TRANSFER', 'CHEQUE', 'MANUAL'], default: 'MANUAL' },
            reference_id: { type: 'string' },
            description: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const body = request.body as any;

      const ledgerEntry = await withTransaction(async (client) => {
        // Lock client row for update
        const cRes = await client.query(`SELECT outstanding_balance, credit_limit FROM clients WHERE id = $1 FOR UPDATE`, [id]);
        if (cRes.rows.length === 0) {
          throw new Error('Client not found');
        }

        const currentBalance = parseFloat(cRes.rows[0].outstanding_balance);
        const creditLimit = parseFloat(cRes.rows[0].credit_limit);
        const amount = parseFloat(body.amount);

        let newBalance = currentBalance;
        if (body.entry_type === 'INVOICE' || body.entry_type === 'DEBIT_NOTE') {
          newBalance += amount;
        } else if (body.entry_type === 'PAYMENT' || body.entry_type === 'CREDIT_NOTE') {
          newBalance -= amount;
        } else if (body.entry_type === 'ADJUSTMENT') {
          newBalance = amount;
        }

        const isCreditHold = newBalance > creditLimit;

        // Update client balance
        await client.query(
          `UPDATE clients SET outstanding_balance = $1, credit_hold = $2 WHERE id = $3`,
          [newBalance, isCreditHold, id]
        );

        // Insert ledger row
        const lRes = await client.query(
          `INSERT INTO client_ledger_entries (
             client_id, entry_type, amount, balance_after,
             reference_type, reference_id, description, recorded_by
           ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
           RETURNING *`,
          [
            id,
            body.entry_type,
            amount,
            newBalance,
            body.reference_type || 'MANUAL',
            body.reference_id || null,
            body.description || null,
            request.user!.id,
          ]
        );

        return lRes.rows[0];
      });

      return reply.status(201).send({
        success: true,
        message: 'Ledger entry recorded successfully',
        data: ledgerEntry,
      });
    }
  );
};
