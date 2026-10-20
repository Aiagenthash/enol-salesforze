import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';

export const productRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/products
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated catalog of automotive lubricants, pack sizes, carton units, and schemes',
        tags: ['Product Catalog'],
        querystring: {
          type: 'object',
          properties: {
            category: {
              type: 'string',
              enum: [
                'PASSENGER_CAR_MOTOR_OIL',
                'HEAVY_DUTY_DIESEL',
                'MOTORCYCLE_4T',
                'GEAR_OIL',
                'TRANSMISSION_FLUID',
                'COOLANT',
                'GREASE',
                'SPECIALTY',
              ],
            },
            viscosity_grade: { type: 'string' },
            is_active: { type: 'boolean' },
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

      if (queryParams.category) {
        whereConditions.push(`category = $${pIdx++}`);
        values.push(queryParams.category);
      }
      if (queryParams.viscosity_grade) {
        whereConditions.push(`viscosity_grade = $${pIdx++}`);
        values.push(queryParams.viscosity_grade);
      }
      if (queryParams.is_active !== undefined) {
        whereConditions.push(`is_active = $${pIdx++}`);
        values.push(queryParams.is_active === 'true' || queryParams.is_active === true);
      }
      if (search) {
        whereConditions.push(`(sku ILIKE $${pIdx} OR name ILIKE $${pIdx} OR viscosity_grade ILIKE $${pIdx})`);
        values.push(`%${search}%`);
        pIdx++;
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM products WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT id, sku, name, description, category, viscosity_grade,
                pack_size_liters, units_per_carton, trade_price, retail_price,
                active_schemes, stock_quantity, is_active, created_at, updated_at
         FROM products
         WHERE ${whereClause}
         ORDER BY category ASC, sku ASC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/products/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get product SKU detail with pricing, carton packing, and active trade schemes',
        tags: ['Product Catalog'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const result = await query(
        `SELECT id, sku, name, description, category, viscosity_grade,
                pack_size_liters, units_per_carton, trade_price, retail_price,
                active_schemes, stock_quantity, is_active, created_at, updated_at
         FROM products
         WHERE id = $1`,
        [id]
      );

      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Product not found' });
      }

      return reply.send({ success: true, data: result.rows[0] });
    }
  );

  // POST /api/v1/products
  fastify.post(
    '/',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Add a new automotive lubricant SKU to the catalog',
        tags: ['Product Catalog'],
        body: {
          type: 'object',
          required: ['sku', 'name', 'category', 'viscosity_grade', 'pack_size_liters', 'trade_price', 'retail_price'],
          properties: {
            sku: { type: 'string' },
            name: { type: 'string' },
            description: { type: 'string' },
            category: {
              type: 'string',
              enum: [
                'PASSENGER_CAR_MOTOR_OIL',
                'HEAVY_DUTY_DIESEL',
                'MOTORCYCLE_4T',
                'GEAR_OIL',
                'TRANSMISSION_FLUID',
                'COOLANT',
                'GREASE',
                'SPECIALTY',
              ],
            },
            viscosity_grade: { type: 'string' },
            pack_size_liters: { type: 'number' },
            units_per_carton: { type: 'integer', default: 1 },
            trade_price: { type: 'number' },
            retail_price: { type: 'number' },
            active_schemes: { type: 'array', items: { type: 'object' } },
            stock_quantity: { type: 'integer', default: 1000 },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;

      try {
        const result = await query(
          `INSERT INTO products (
             sku, name, description, category, viscosity_grade,
             pack_size_liters, units_per_carton, trade_price, retail_price,
             active_schemes, stock_quantity
           ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
           RETURNING *`,
          [
            body.sku,
            body.name,
            body.description || null,
            body.category,
            body.viscosity_grade,
            body.pack_size_liters,
            body.units_per_carton || 1,
            body.trade_price,
            body.retail_price,
            JSON.stringify(body.active_schemes || []),
            body.stock_quantity ?? 1000,
          ]
        );

        return reply.status(201).send({
          success: true,
          message: 'Product created successfully',
          data: result.rows[0],
        });
      } catch (err: any) {
        if (err.code === '23505') {
          return reply.status(409).send({ success: false, error: 'Product SKU already exists' });
        }
        throw err;
      }
    }
  );

  // PUT /api/v1/products/:id
  fastify.put(
    '/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Update lubricant prices, schemes, or inventory details',
        tags: ['Product Catalog'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            description: { type: 'string' },
            trade_price: { type: 'number' },
            retail_price: { type: 'number' },
            active_schemes: { type: 'array', items: { type: 'object' } },
            stock_quantity: { type: 'integer' },
            is_active: { type: 'boolean' },
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
          values.push(key === 'active_schemes' ? JSON.stringify(val) : val);
        }
      }

      if (fields.length === 0) {
        return reply.status(400).send({ success: false, error: 'No fields provided for update' });
      }

      values.push(id);
      const sql = `UPDATE products SET ${fields.join(', ')} WHERE id = $${pIdx} RETURNING *`;

      const result = await query(sql, values);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Product not found' });
      }

      return reply.send({ success: true, message: 'Product updated successfully', data: result.rows[0] });
    }
  );

  // DELETE /api/v1/products/:id
  fastify.delete(
    '/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Deactivate a product SKU',
        tags: ['Product Catalog'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const result = await query(
        `UPDATE products SET is_active = FALSE WHERE id = $1 RETURNING id, sku, is_active`,
        [id]
      );
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Product not found' });
      }
      return reply.send({ success: true, message: 'Product deactivated successfully' });
    }
  );
};
