import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';

export const kpiRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/kpis
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated list of sales performance targets and KPI scorecards',
        tags: ['Targets & KPIs'],
        querystring: {
          type: 'object',
          properties: {
            user_id: { type: 'string' },
            period_type: { type: 'string', enum: ['MONTHLY', 'QUARTERLY', 'ANNUAL'] },
            period_start: { type: 'string' },
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
        whereConditions.push(`k.user_id = $${pIdx++}`);
        values.push(queryParams.user_id);
      }
      if (queryParams.period_type) {
        whereConditions.push(`k.period_type = $${pIdx++}`);
        values.push(queryParams.period_type);
      }
      if (queryParams.period_start) {
        whereConditions.push(`k.period_start = $${pIdx++}`);
        values.push(queryParams.period_start);
      }

      // Reps only see their KPIs
      if (request.user!.role === 'SALES_REP') {
        whereConditions.push(`k.user_id = $${pIdx++}`);
        values.push(request.user!.id);
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM targets_kpis k WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT k.id, k.user_id, u.full_name as user_name, u.role as user_role,
                k.period_type, k.period_start, k.period_end,
                k.target_volume_liters, k.achieved_volume_liters,
                k.target_revenue, k.achieved_revenue,
                k.target_visits, k.achieved_visits,
                k.target_new_clients, k.achieved_new_clients,
                k.calculated_kpi_score, k.created_at, k.updated_at
         FROM targets_kpis k
         JOIN users u ON u.id = k.user_id
         WHERE ${whereClause}
         ORDER BY k.period_start DESC, k.calculated_kpi_score DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/kpis/user/:userId
  fastify.get(
    '/user/:userId',
    {
      schema: {
        description: 'Get comprehensive KPI scorecard for a specific sales rep or manager',
        tags: ['Targets & KPIs'],
        params: {
          type: 'object',
          required: ['userId'],
          properties: { userId: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { userId } = request.params as { userId: string };

      const userRes = await query(`SELECT id, full_name, email, role FROM users WHERE id = $1`, [userId]);
      if (userRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'User not found' });
      }

      const kpiRes = await query(
        `SELECT id, period_type, period_start, period_end,
                target_volume_liters, achieved_volume_liters,
                ROUND((achieved_volume_liters / NULLIF(target_volume_liters, 0)) * 100, 2) as volume_achievement_pct,
                target_revenue, achieved_revenue,
                ROUND((achieved_revenue / NULLIF(target_revenue, 0)) * 100, 2) as revenue_achievement_pct,
                target_visits, achieved_visits,
                target_new_clients, achieved_new_clients,
                calculated_kpi_score
         FROM targets_kpis
         WHERE user_id = $1
         ORDER BY period_start DESC
         LIMIT 6`,
        [userId]
      );

      return reply.send({
        success: true,
        user: userRes.rows[0],
        scorecards: kpiRes.rows,
      });
    }
  );

  // POST /api/v1/kpis
  fastify.post(
    '/',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM', 'ASM'])],
      schema: {
        description: 'Set performance targets for a sales rep or manager',
        tags: ['Targets & KPIs'],
        body: {
          type: 'object',
          required: ['user_id', 'period_type', 'period_start', 'period_end', 'target_volume_liters', 'target_revenue'],
          properties: {
            user_id: { type: 'string' },
            period_type: { type: 'string', enum: ['MONTHLY', 'QUARTERLY', 'ANNUAL'] },
            period_start: { type: 'string' },
            period_end: { type: 'string' },
            target_volume_liters: { type: 'number', minimum: 0 },
            target_revenue: { type: 'number', minimum: 0 },
            target_visits: { type: 'integer', default: 0 },
            target_new_clients: { type: 'integer', default: 0 },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;

      try {
        const result = await query(
          `INSERT INTO targets_kpis (
             user_id, period_type, period_start, period_end,
             target_volume_liters, target_revenue, target_visits, target_new_clients
           ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
           RETURNING *`,
          [
            body.user_id,
            body.period_type,
            body.period_start,
            body.period_end,
            body.target_volume_liters,
            body.target_revenue,
            body.target_visits || 0,
            body.target_new_clients || 0,
          ]
        );

        return reply.status(201).send({
          success: true,
          message: 'Target assigned successfully',
          data: result.rows[0],
        });
      } catch (err: any) {
        if (err.code === '23505') {
          return reply.status(409).send({ success: false, error: 'Target already exists for this user and period' });
        }
        throw err;
      }
    }
  );

  // PUT /api/v1/kpis/:id
  fastify.put(
    '/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM', 'ASM'])],
      schema: {
        description: 'Update target or actual achievements',
        tags: ['Targets & KPIs'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          properties: {
            achieved_volume_liters: { type: 'number' },
            achieved_revenue: { type: 'number' },
            achieved_visits: { type: 'integer' },
            achieved_new_clients: { type: 'integer' },
            calculated_kpi_score: { type: 'number' },
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
      const sql = `UPDATE targets_kpis SET ${fields.join(', ')} WHERE id = $${pIdx} RETURNING *`;

      const result = await query(sql, values);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Target record not found' });
      }

      return reply.send({ success: true, message: 'Target updated successfully', data: result.rows[0] });
    }
  );
};
