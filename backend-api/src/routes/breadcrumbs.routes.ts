import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';

export const breadcrumbRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/breadcrumbs
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated GPS location breadcrumbs for a sales rep',
        tags: ['Location Breadcrumbs & GPS'],
        querystring: {
          type: 'object',
          properties: {
            user_id: { type: 'string' },
            attendance_id: { type: 'string' },
            from_time: { type: 'string' },
            to_time: { type: 'string' },
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
        whereConditions.push(`b.user_id = $${pIdx++}`);
        values.push(queryParams.user_id);
      }
      if (queryParams.attendance_id) {
        whereConditions.push(`b.attendance_id = $${pIdx++}`);
        values.push(queryParams.attendance_id);
      }
      if (queryParams.from_time) {
        whereConditions.push(`b.recorded_at >= $${pIdx++}`);
        values.push(queryParams.from_time);
      }
      if (queryParams.to_time) {
        whereConditions.push(`b.recorded_at <= $${pIdx++}`);
        values.push(queryParams.to_time);
      }

      // Reps only see their breadcrumbs
      if (request.user!.role === 'SALES_REP') {
        whereConditions.push(`b.user_id = $${pIdx++}`);
        values.push(request.user!.id);
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM location_breadcrumbs b WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT b.id, b.user_id, u.full_name as user_name, b.attendance_id,
                b.latitude, b.longitude, b.accuracy_meters, b.speed, b.bearing,
                b.battery_level, b.is_mock_gps, b.recorded_at, b.created_at
         FROM location_breadcrumbs b
         JOIN users u ON u.id = b.user_id
         WHERE ${whereClause}
         ORDER BY b.recorded_at DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // POST /api/v1/breadcrumbs
  fastify.post(
    '/',
    {
      schema: {
        description: 'Batch ingest GPS breadcrumbs from mobile field app (single or bulk array)',
        tags: ['Location Breadcrumbs & GPS'],
        body: {
          type: 'object',
          properties: {
            attendance_id: { type: 'string', nullable: true },
            breadcrumbs: {
              type: 'array',
              items: {
                type: 'object',
                required: ['latitude', 'longitude', 'recorded_at'],
                properties: {
                  latitude: { type: 'number' },
                  longitude: { type: 'number' },
                  accuracy_meters: { type: 'number', default: 5.0 },
                  speed: { type: 'number', nullable: true },
                  bearing: { type: 'number', nullable: true },
                  battery_level: { type: 'number', nullable: true },
                  is_mock_gps: { type: 'boolean', default: false },
                  recorded_at: { type: 'string' },
                },
              },
            },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;
      const userId = request.user!.id;
      const points = Array.isArray(body.breadcrumbs) ? body.breadcrumbs : [body];

      if (points.length === 0) {
        return reply.status(400).send({ success: false, error: 'No breadcrumb points provided' });
      }

      let insertedCount = 0;
      for (const pt of points) {
        await query(
          `INSERT INTO location_breadcrumbs (
             user_id, attendance_id, latitude, longitude,
             accuracy_meters, speed, bearing, battery_level, is_mock_gps, recorded_at
           ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
          [
            userId,
            body.attendance_id || pt.attendance_id || null,
            pt.latitude,
            pt.longitude,
            pt.accuracy_meters || 5.0,
            pt.speed ?? null,
            pt.bearing ?? null,
            pt.battery_level ?? null,
            pt.is_mock_gps || false,
            pt.recorded_at || new Date().toISOString(),
          ]
        );
        insertedCount++;
      }

      return reply.status(201).send({
        success: true,
        message: `Successfully ingested ${insertedCount} GPS breadcrumb points`,
      });
    }
  );

  // GET /api/v1/breadcrumbs/user/:userId/route
  fastify.get(
    '/user/:userId/route',
    {
      schema: {
        description: 'Get route playback GeoJSON LineString for map rendering on a given date',
        tags: ['Location Breadcrumbs & GPS'],
        params: {
          type: 'object',
          required: ['userId'],
          properties: { userId: { type: 'string' } },
        },
        querystring: {
          type: 'object',
          properties: {
            date: { type: 'string', description: 'YYYY-MM-DD' },
          },
        },
      },
    },
    async (request, reply) => {
      const { userId } = request.params as { userId: string };
      const { date = new Date().toISOString().slice(0, 10) } = request.query as any;

      const pointsRes = await query(
        `SELECT longitude, latitude, speed, battery_level, recorded_at
         FROM location_breadcrumbs
         WHERE user_id = $1 AND recorded_at::date = $2::date
         ORDER BY recorded_at ASC`,
        [userId, date]
      );

      const coordinates = pointsRes.rows.map((r) => [parseFloat(r.longitude), parseFloat(r.latitude)]);

      const geoJson = {
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates,
        },
        properties: {
          userId,
          date,
          total_points: pointsRes.rows.length,
          startTime: pointsRes.rows[0]?.recorded_at || null,
          endTime: pointsRes.rows[pointsRes.rows.length - 1]?.recorded_at || null,
        },
      };

      return reply.send({
        success: true,
        data: geoJson,
      });
    }
  );
};
