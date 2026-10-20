import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';

export const attendanceRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/attendance
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated daily attendance records for field force',
        tags: ['Daily Attendance & Shifts'],
        querystring: {
          type: 'object',
          properties: {
            user_id: { type: 'string' },
            date: { type: 'string' },
            status: { type: 'string', enum: ['ACTIVE', 'COMPLETED', 'AUTO_CLOSED', 'REJECTED'] },
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
        whereConditions.push(`a.user_id = $${pIdx++}`);
        values.push(queryParams.user_id);
      }
      if (queryParams.date) {
        whereConditions.push(`a.date = $${pIdx++}`);
        values.push(queryParams.date);
      }
      if (queryParams.status) {
        whereConditions.push(`a.status = $${pIdx++}`);
        values.push(queryParams.status);
      }

      // Reps only see their own attendance
      if (request.user!.role === 'SALES_REP') {
        whereConditions.push(`a.user_id = $${pIdx++}`);
        values.push(request.user!.id);
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM daily_attendance a WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT a.id, a.user_id, u.full_name as user_name, u.role as user_role,
                a.date, a.shift_start, a.shift_end,
                a.start_latitude, a.start_longitude, a.start_selfie_url,
                a.start_odometer_reading, a.start_odometer_photo_url,
                a.end_latitude, a.end_longitude, a.end_selfie_url,
                a.end_odometer_reading, a.end_odometer_photo_url,
                a.total_hours, a.status, a.notes, a.created_at, a.updated_at
         FROM daily_attendance a
         JOIN users u ON u.id = a.user_id
         WHERE ${whereClause}
         ORDER BY a.date DESC, a.shift_start DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/attendance/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get single attendance shift record by ID',
        tags: ['Daily Attendance & Shifts'],
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
        `SELECT a.*, u.full_name as user_name, u.role as user_role
         FROM daily_attendance a
         JOIN users u ON u.id = a.user_id
         WHERE a.id = $1`,
        [id]
      );

      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Attendance record not found' });
      }

      return reply.send({ success: true, data: result.rows[0] });
    }
  );

  // POST /api/v1/attendance/start-shift
  fastify.post(
    '/start-shift',
    {
      schema: {
        description: 'Punch-in and start field sales shift with photo selfie, odometer reading & photo',
        tags: ['Daily Attendance & Shifts'],
        body: {
          type: 'object',
          required: [
            'start_latitude',
            'start_longitude',
            'start_selfie_url',
            'start_odometer_reading',
            'start_odometer_photo_url',
          ],
          properties: {
            start_latitude: { type: 'number' },
            start_longitude: { type: 'number' },
            start_selfie_url: { type: 'string', format: 'uri' },
            start_odometer_reading: { type: 'number', minimum: 0 },
            start_odometer_photo_url: { type: 'string', format: 'uri' },
            notes: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;
      const userId = request.user!.id;
      const today = new Date().toISOString().slice(0, 10);

      // Check if user already has attendance today
      const existing = await query(
        `SELECT id, status FROM daily_attendance WHERE user_id = $1 AND date = $2`,
        [userId, today]
      );

      if (existing.rows.length > 0) {
        return reply.status(409).send({
          success: false,
          error: 'Shift already started for today',
          existing_attendance: existing.rows[0],
        });
      }

      const result = await query(
        `INSERT INTO daily_attendance (
           user_id, date, shift_start, start_latitude, start_longitude,
           start_selfie_url, start_odometer_reading, start_odometer_photo_url,
           status, notes
         ) VALUES ($1, $2, CURRENT_TIMESTAMP, $3, $4, $5, $6, $7, 'ACTIVE', $8)
         RETURNING *`,
        [
          userId,
          today,
          body.start_latitude,
          body.start_longitude,
          body.start_selfie_url,
          body.start_odometer_reading,
          body.start_odometer_photo_url,
          body.notes || null,
        ]
      );

      return reply.status(201).send({
        success: true,
        message: 'Shift started successfully',
        data: result.rows[0],
      });
    }
  );

  // POST /api/v1/attendance/:id/end-shift
  fastify.post(
    '/:id/end-shift',
    {
      schema: {
        description: 'Punch-out and complete daily shift with closing selfie, odometer reading, and hours calculation',
        tags: ['Daily Attendance & Shifts'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          required: [
            'end_latitude',
            'end_longitude',
            'end_selfie_url',
            'end_odometer_reading',
            'end_odometer_photo_url',
          ],
          properties: {
            end_latitude: { type: 'number' },
            end_longitude: { type: 'number' },
            end_selfie_url: { type: 'string', format: 'uri' },
            end_odometer_reading: { type: 'number', minimum: 0 },
            end_odometer_photo_url: { type: 'string', format: 'uri' },
            notes: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const body = request.body as any;

      const attRes = await query(`SELECT * FROM daily_attendance WHERE id = $1`, [id]);
      if (attRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Attendance record not found' });
      }

      const att = attRes.rows[0];
      if (att.status === 'COMPLETED') {
        return reply.status(400).send({ success: false, error: 'Shift is already completed' });
      }

      const startTime = new Date(att.shift_start).getTime();
      const endTime = Date.now();
      const totalHours = Math.round(((endTime - startTime) / (1000 * 60 * 60)) * 100) / 100;

      const result = await query(
        `UPDATE daily_attendance
         SET shift_end = CURRENT_TIMESTAMP,
             end_latitude = $1,
             end_longitude = $2,
             end_selfie_url = $3,
             end_odometer_reading = $4,
             end_odometer_photo_url = $5,
             total_hours = $6,
             status = 'COMPLETED',
             notes = COALESCE($7, notes)
         WHERE id = $8
         RETURNING *`,
        [
          body.end_latitude,
          body.end_longitude,
          body.end_selfie_url,
          body.end_odometer_reading,
          body.end_odometer_photo_url,
          totalHours,
          body.notes || null,
          id,
        ]
      );

      return reply.send({
        success: true,
        message: 'Shift completed successfully',
        data: result.rows[0],
      });
    }
  );
};
