import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import crypto from 'crypto';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';

export const mechanicRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/mechanics
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated list of mechanics in the loyalty influencer program',
        tags: ['Mechanics & Loyalty'],
        querystring: {
          type: 'object',
          properties: {
            specialty: {
              type: 'string',
              enum: ['4-STROKE_MOTORCYCLE', 'HEAVY_DIESEL', 'PASSENGER_CAR', 'TRACTOR_AGRI', 'GENERAL_AUTO'],
            },
            wallet_provider: {
              type: 'string',
              enum: ['EASYPAISA', 'JAZZCASH', 'NAYAPAY', 'SADAPAY', 'BANK_ACCOUNT'],
            },
            client_id: { type: 'string' },
            qr_code_id: { type: 'string' },
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

      if (queryParams.specialty) {
        whereConditions.push(`m.specialty = $${pIdx++}`);
        values.push(queryParams.specialty);
      }
      if (queryParams.wallet_provider) {
        whereConditions.push(`m.wallet_provider = $${pIdx++}`);
        values.push(queryParams.wallet_provider);
      }
      if (queryParams.client_id) {
        whereConditions.push(`m.client_id = $${pIdx++}`);
        values.push(queryParams.client_id);
      }
      if (queryParams.qr_code_id) {
        whereConditions.push(`m.qr_code_id = $${pIdx++}`);
        values.push(queryParams.qr_code_id);
      }
      if (search) {
        whereConditions.push(`(m.name ILIKE $${pIdx} OR m.phone ILIKE $${pIdx} OR m.wallet_account_number ILIKE $${pIdx})`);
        values.push(`%${search}%`);
        pIdx++;
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM mechanics m WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT m.id, m.client_id, c.business_name as linked_workshop_name,
                m.name, m.phone, m.cnic, m.specialty, m.wallet_provider,
                m.wallet_account_number, m.qr_code_id, m.points_balance,
                m.total_scans, m.is_verified, m.registered_by, u.full_name as registered_by_name,
                m.created_at, m.updated_at
         FROM mechanics m
         LEFT JOIN clients c ON c.id = m.client_id
         LEFT JOIN users u ON u.id = m.registered_by
         WHERE ${whereClause}
         ORDER BY m.points_balance DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/mechanics/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get mechanic details, wallet numbers, loyalty points, and workshop link',
        tags: ['Mechanics & Loyalty'],
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
        `SELECT m.id, m.client_id, c.business_name as linked_workshop_name,
                m.name, m.phone, m.cnic, m.specialty, m.wallet_provider,
                m.wallet_account_number, m.qr_code_id, m.points_balance,
                m.total_scans, m.is_verified, m.registered_by, u.full_name as registered_by_name,
                m.created_at, m.updated_at
         FROM mechanics m
         LEFT JOIN clients c ON c.id = m.client_id
         LEFT JOIN users u ON u.id = m.registered_by
         WHERE m.id = $1`,
        [id]
      );

      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Mechanic not found' });
      }

      return reply.send({ success: true, data: result.rows[0] });
    }
  );

  // POST /api/v1/mechanics
  fastify.post(
    '/',
    {
      schema: {
        description: 'Register a new mechanic in the loyalty network',
        tags: ['Mechanics & Loyalty'],
        body: {
          type: 'object',
          required: ['name', 'phone', 'specialty', 'wallet_provider', 'wallet_account_number'],
          properties: {
            client_id: { type: 'string', nullable: true },
            name: { type: 'string' },
            phone: { type: 'string' },
            cnic: { type: 'string', nullable: true },
            specialty: {
              type: 'string',
              enum: ['4-STROKE_MOTORCYCLE', 'HEAVY_DIESEL', 'PASSENGER_CAR', 'TRACTOR_AGRI', 'GENERAL_AUTO'],
            },
            wallet_provider: {
              type: 'string',
              enum: ['EASYPAISA', 'JAZZCASH', 'NAYAPAY', 'SADAPAY', 'BANK_ACCOUNT'],
            },
            wallet_account_number: { type: 'string' },
            qr_code_id: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;
      const qrCodeId = body.qr_code_id || `QR-MECH-${crypto.randomBytes(4).toString('hex').toUpperCase()}`;

      try {
        const result = await query(
          `INSERT INTO mechanics (
             client_id, name, phone, cnic, specialty,
             wallet_provider, wallet_account_number, qr_code_id, registered_by
           ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
           RETURNING *`,
          [
            body.client_id || null,
            body.name,
            body.phone,
            body.cnic || null,
            body.specialty,
            body.wallet_provider,
            body.wallet_account_number,
            qrCodeId,
            request.user!.id,
          ]
        );

        return reply.status(201).send({
          success: true,
          message: 'Mechanic registered successfully',
          data: result.rows[0],
        });
      } catch (err: any) {
        if (err.code === '23505') {
          return reply.status(409).send({ success: false, error: 'Mechanic with this phone or QR code already registered' });
        }
        throw err;
      }
    }
  );

  // PUT /api/v1/mechanics/:id
  fastify.put(
    '/:id',
    {
      schema: {
        description: 'Update mechanic profile or payout wallet account',
        tags: ['Mechanics & Loyalty'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          properties: {
            client_id: { type: 'string', nullable: true },
            name: { type: 'string' },
            specialty: {
              type: 'string',
              enum: ['4-STROKE_MOTORCYCLE', 'HEAVY_DIESEL', 'PASSENGER_CAR', 'TRACTOR_AGRI', 'GENERAL_AUTO'],
            },
            wallet_provider: {
              type: 'string',
              enum: ['EASYPAISA', 'JAZZCASH', 'NAYAPAY', 'SADAPAY', 'BANK_ACCOUNT'],
            },
            wallet_account_number: { type: 'string' },
            is_verified: { type: 'boolean' },
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
      const sql = `UPDATE mechanics SET ${fields.join(', ')} WHERE id = $${pIdx} RETURNING *`;

      const result = await query(sql, values);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Mechanic not found' });
      }

      return reply.send({ success: true, message: 'Mechanic updated successfully', data: result.rows[0] });
    }
  );

  // POST /api/v1/mechanics/:id/points
  fastify.post(
    '/:id/points',
    {
      schema: {
        description: 'Award loyalty points for cap coupon QR scan or promotion',
        tags: ['Mechanics & Loyalty'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          required: ['points'],
          properties: {
            points: { type: 'integer', minimum: 1 },
            reason: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const { points } = request.body as { points: number; reason?: string };

      const result = await query(
        `UPDATE mechanics 
         SET points_balance = points_balance + $1,
             total_scans = total_scans + 1
         WHERE id = $2
         RETURNING id, name, phone, points_balance, total_scans`,
        [points, id]
      );

      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Mechanic not found' });
      }

      return reply.send({
        success: true,
        message: `${points} loyalty points awarded successfully`,
        data: result.rows[0],
      });
    }
  );
};
