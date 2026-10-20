import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';
import { WebhookDispatcher } from '../services/webhook-dispatcher.service';

export const tadaRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/tada-claims
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated list of Travel & Daily Allowance (TADA) claims with variance audit flags',
        tags: ['TADA Claims & Allowances'],
        querystring: {
          type: 'object',
          properties: {
            user_id: { type: 'string' },
            status: { type: 'string', enum: ['SUBMITTED', 'VERIFIED_FLAGGED', 'APPROVED', 'REJECTED', 'DISBURSED'] },
            flagged_only: { type: 'boolean' },
            from_date: { type: 'string' },
            to_date: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const { page, limit } = parsePagination(request);
      const queryParams = request.query as any;

      let whereConditions = ['1=1'];
      let values: any[] = [];
      let pIdx = 1;

      if (queryParams.user_id) {
        whereConditions.push(`t.user_id = $${pIdx++}`);
        values.push(queryParams.user_id);
      }
      if (queryParams.status) {
        whereConditions.push(`t.status = $${pIdx++}`);
        values.push(queryParams.status);
      }
      if (queryParams.flagged_only === 'true' || queryParams.flagged_only === true) {
        whereConditions.push(`(t.variance_percentage > 15.00 OR t.status = 'VERIFIED_FLAGGED')`);
      }
      if (queryParams.from_date) {
        whereConditions.push(`t.claim_date >= $${pIdx++}`);
        values.push(queryParams.from_date);
      }
      if (queryParams.to_date) {
        whereConditions.push(`t.claim_date <= $${pIdx++}`);
        values.push(queryParams.to_date);
      }

      // Reps only see their claims
      if (request.user!.role === 'SALES_REP') {
        whereConditions.push(`t.user_id = $${pIdx++}`);
        values.push(request.user!.id);
      } else if (request.user!.role === 'ASM' || request.user!.role === 'ZSM') {
        whereConditions.push(`u.hierarchy_path LIKE $${pIdx++}`);
        values.push(`${request.user!.hierarchy_path}%`);
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(
        `SELECT COUNT(*) FROM tada_claims t 
         JOIN users u ON u.id = t.user_id 
         WHERE ${whereClause}`,
        values
      );
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT t.id, t.attendance_id, t.user_id, u.full_name as user_name, u.role as user_role,
                u.vehicle_type, u.vehicle_number,
                t.claim_date, t.start_odometer, t.end_odometer,
                t.odometer_delta_km, t.gps_distance_km, t.variance_percentage,
                t.fuel_rate_per_km, t.fuel_claim_amount, t.daily_allowance_amount,
                t.toll_parking_amount, t.total_claim_amount, t.status,
                t.approved_by, app.full_name as approved_by_name, t.approved_at,
                t.rejection_reason, t.created_at, t.updated_at
         FROM tada_claims t
         JOIN users u ON u.id = t.user_id
         LEFT JOIN users app ON app.id = t.approved_by
         WHERE ${whereClause}
         ORDER BY t.claim_date DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/tada-claims/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get TADA claim detail with audit comparison between odometer delta and GPS distance',
        tags: ['TADA Claims & Allowances'],
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
        `SELECT t.*, u.full_name as user_name, u.vehicle_type, u.vehicle_number,
                app.full_name as approved_by_name
         FROM tada_claims t
         JOIN users u ON u.id = t.user_id
         LEFT JOIN users app ON app.id = t.approved_by
         WHERE t.id = $1`,
        [id]
      );

      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'TADA claim not found' });
      }

      return reply.send({ success: true, data: result.rows[0] });
    }
  );

  // POST /api/v1/tada-claims
  fastify.post(
    '/',
    {
      schema: {
        description: 'Submit daily fuel & TADA claim with odometer readings and variance calculation against GPS',
        tags: ['TADA Claims & Allowances'],
        body: {
          type: 'object',
          required: ['attendance_id', 'claim_date', 'start_odometer', 'end_odometer', 'gps_distance_km'],
          properties: {
            attendance_id: { type: 'string' },
            claim_date: { type: 'string' },
            start_odometer: { type: 'number', minimum: 0 },
            end_odometer: { type: 'number', minimum: 0 },
            gps_distance_km: { type: 'number', minimum: 0 },
            toll_parking_amount: { type: 'number', default: 0 },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;
      const userId = request.user!.id;

      // Fetch user fuel rate & daily allowance
      const uRes = await query(
        `SELECT fuel_rate_per_km, daily_allowance FROM users WHERE id = $1`,
        [userId]
      );
      const fuelRate = parseFloat(uRes.rows[0]?.fuel_rate_per_km || 7.50);
      const dailyAllowance = parseFloat(uRes.rows[0]?.daily_allowance || 500.00);

      const startOdo = parseFloat(body.start_odometer);
      const endOdo = parseFloat(body.end_odometer);
      const odoDelta = Math.max(0, endOdo - startOdo);
      const gpsDistance = parseFloat(body.gps_distance_km);

      const variancePct = gpsDistance > 0 ? Math.round(((odoDelta - gpsDistance) / gpsDistance) * 10000) / 100 : 0;
      const fuelAmount = Math.round(odoDelta * fuelRate * 100) / 100;
      const tollParking = parseFloat(body.toll_parking_amount || 0);
      const totalClaim = Math.round((fuelAmount + dailyAllowance + tollParking) * 100) / 100;

      // Fraud / audit flag if variance > 15%
      const status = variancePct > 15.00 ? 'VERIFIED_FLAGGED' : 'SUBMITTED';

      const result = await query(
        `INSERT INTO tada_claims (
           attendance_id, user_id, claim_date, start_odometer, end_odometer,
           odometer_delta_km, gps_distance_km, variance_percentage,
           fuel_rate_per_km, fuel_claim_amount, daily_allowance_amount,
           toll_parking_amount, total_claim_amount, status
         ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
         RETURNING *`,
        [
          body.attendance_id,
          userId,
          body.claim_date,
          startOdo,
          endOdo,
          odoDelta,
          gpsDistance,
          variancePct,
          fuelRate,
          fuelAmount,
          dailyAllowance,
          tollParking,
          totalClaim,
          status,
        ]
      );

      const claim = result.rows[0];

      // Dispatch Webhook event `tada.submitted`
      WebhookDispatcher.dispatch('tada.submitted', claim);

      return reply.status(201).send({
        success: true,
        message:
          status === 'VERIFIED_FLAGGED'
            ? `TADA claim submitted with AUDIT WARNING (odometer vs GPS variance is ${variancePct}%)`
            : 'TADA claim submitted successfully',
        data: claim,
      });
    }
  );

  // PUT /api/v1/tada-claims/:id/review
  fastify.put(
    '/:id/review',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM', 'ASM'])],
      schema: {
        description: 'Review, approve, reject, or disburse TADA expense claim',
        tags: ['TADA Claims & Allowances'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          required: ['status'],
          properties: {
            status: { type: 'string', enum: ['APPROVED', 'REJECTED', 'DISBURSED'] },
            rejection_reason: { type: 'string', nullable: true },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const { status, rejection_reason } = request.body as any;

      const result = await query(
        `UPDATE tada_claims
         SET status = $1,
             approved_by = $2,
             approved_at = CURRENT_TIMESTAMP,
             rejection_reason = $3
         WHERE id = $4
         RETURNING *`,
        [status, request.user!.id, rejection_reason || null, id]
      );

      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'TADA claim not found' });
      }

      return reply.send({
        success: true,
        message: `TADA claim marked as ${status}`,
        data: result.rows[0],
      });
    }
  );
};
