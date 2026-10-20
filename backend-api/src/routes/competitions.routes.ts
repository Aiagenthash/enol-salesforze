import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';

export const competitionRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/competitions
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get list of field sales competitions, leaderboards, and incentive campaigns',
        tags: ['Sales Competitions & Gamification'],
        querystring: {
          type: 'object',
          properties: {
            is_active: { type: 'boolean' },
            target_metric: {
              type: 'string',
              enum: ['VOLUME_LITERS', 'REVENUE_PKR', 'NEW_CLIENTS', 'SYNTHETIC_RATIO', 'COLLECTION_RATE'],
            },
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

      if (queryParams.is_active !== undefined) {
        whereConditions.push(`is_active = $${pIdx++}`);
        values.push(queryParams.is_active === 'true' || queryParams.is_active === true);
      }
      if (queryParams.target_metric) {
        whereConditions.push(`target_metric = $${pIdx++}`);
        values.push(queryParams.target_metric);
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM competitions WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT id, title, description, start_date, end_date, target_metric,
                reward_details, is_active, created_at, updated_at,
                (SELECT COUNT(*) FROM competition_scores cs WHERE cs.competition_id = competitions.id) as participants_count
         FROM competitions
         WHERE ${whereClause}
         ORDER BY start_date DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/competitions/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get competition details and incentive reward tiers',
        tags: ['Sales Competitions & Gamification'],
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
        `SELECT id, title, description, start_date, end_date, target_metric,
                reward_details, is_active, created_at, updated_at
         FROM competitions
         WHERE id = $1`,
        [id]
      );

      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Competition not found' });
      }

      return reply.send({ success: true, data: result.rows[0] });
    }
  );

  // GET /api/v1/competitions/:id/leaderboard
  fastify.get(
    '/:id/leaderboard',
    {
      schema: {
        description: 'Get real-time ranked leaderboard for a sales competition',
        tags: ['Sales Competitions & Gamification'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const compRes = await query(`SELECT id, title, target_metric, reward_details FROM competitions WHERE id = $1`, [id]);
      if (compRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Competition not found' });
      }

      const leaderboardRes = await query(
        `SELECT cs.id, cs.competition_id, cs.user_id, u.full_name as user_name, u.role as user_role,
                cs.current_metric_value,
                RANK() OVER (ORDER BY cs.current_metric_value DESC) as rank,
                cs.points, cs.last_calculated_at
         FROM competition_scores cs
         JOIN users u ON u.id = cs.user_id
         WHERE cs.competition_id = $1
         ORDER BY rank ASC
         LIMIT 100`,
        [id]
      );

      return reply.send({
        success: true,
        competition: compRes.rows[0],
        total_participants: leaderboardRes.rows.length,
        leaderboard: leaderboardRes.rows,
      });
    }
  );

  // POST /api/v1/competitions
  fastify.post(
    '/',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Create a new sales competition with metric targets & reward structures',
        tags: ['Sales Competitions & Gamification'],
        body: {
          type: 'object',
          required: ['title', 'start_date', 'end_date', 'target_metric'],
          properties: {
            title: { type: 'string' },
            description: { type: 'string' },
            start_date: { type: 'string' },
            end_date: { type: 'string' },
            target_metric: {
              type: 'string',
              enum: ['VOLUME_LITERS', 'REVENUE_PKR', 'NEW_CLIENTS', 'SYNTHETIC_RATIO', 'COLLECTION_RATE'],
            },
            reward_details: { type: 'object' },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;

      const result = await query(
        `INSERT INTO competitions (
           title, description, start_date, end_date, target_metric, reward_details
         ) VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING *`,
        [
          body.title,
          body.description || null,
          body.start_date,
          body.end_date,
          body.target_metric,
          JSON.stringify(body.reward_details || {}),
        ]
      );

      return reply.status(201).send({
        success: true,
        message: 'Competition created successfully',
        data: result.rows[0],
      });
    }
  );

  // POST /api/v1/competitions/:id/scores
  fastify.post(
    '/:id/scores',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM'])],
      schema: {
        description: 'Update or record rep progress metrics and points in a competition',
        tags: ['Sales Competitions & Gamification'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          required: ['user_id', 'metric_value'],
          properties: {
            user_id: { type: 'string' },
            metric_value: { type: 'number', minimum: 0 },
            points: { type: 'integer' },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const { user_id, metric_value, points } = request.body as any;

      const result = await query(
        `INSERT INTO competition_scores (
           competition_id, user_id, current_metric_value, points, last_calculated_at
         ) VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP)
         ON CONFLICT (competition_id, user_id)
         DO UPDATE SET
           current_metric_value = EXCLUDED.current_metric_value,
           points = COALESCE(EXCLUDED.points, competition_scores.points),
           last_calculated_at = CURRENT_TIMESTAMP
         RETURNING *`,
        [id, user_id, metric_value, points ?? Math.floor(metric_value)]
      );

      return reply.send({
        success: true,
        message: 'Competition score recorded successfully',
        data: result.rows[0],
      });
    }
  );
};
