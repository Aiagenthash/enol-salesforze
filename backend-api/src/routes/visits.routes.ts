import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query, withTransaction } from '../config/database';
import { authenticate } from '../middleware/auth';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';
import { WebhookDispatcher } from '../services/webhook-dispatcher.service';

/**
 * Calculates Great-Circle distance in meters between two lat/lng coordinates (Haversine formula).
 */
function calculateDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000; // Earth radius in meters
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 100) / 100;
}

export const visitRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/visits
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated list of field client visits with geofence distance & outcomes',
        tags: ['Field Shop Visits'],
        querystring: {
          type: 'object',
          properties: {
            sales_rep_id: { type: 'string' },
            client_id: { type: 'string' },
            outcome: {
              type: 'string',
              enum: ['ORDER_PLACED', 'PAYMENT_COLLECTED', 'STOCK_AUDIT_ONLY', 'CLIENT_CLOSED', 'NO_ORDER', 'MERCHANDISING_ONLY'],
            },
            is_mock_gps: { type: 'boolean' },
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

      if (queryParams.sales_rep_id) {
        whereConditions.push(`v.sales_rep_id = $${pIdx++}`);
        values.push(queryParams.sales_rep_id);
      }
      if (queryParams.client_id) {
        whereConditions.push(`v.client_id = $${pIdx++}`);
        values.push(queryParams.client_id);
      }
      if (queryParams.outcome) {
        whereConditions.push(`v.outcome = $${pIdx++}`);
        values.push(queryParams.outcome);
      }
      if (queryParams.is_mock_gps !== undefined) {
        whereConditions.push(`v.is_mock_gps = $${pIdx++}`);
        values.push(queryParams.is_mock_gps === 'true' || queryParams.is_mock_gps === true);
      }
      if (queryParams.from_date) {
        whereConditions.push(`v.check_in_time >= $${pIdx++}`);
        values.push(queryParams.from_date);
      }
      if (queryParams.to_date) {
        whereConditions.push(`v.check_in_time <= $${pIdx++}`);
        values.push(queryParams.to_date);
      }

      // Reps only see their visits
      if (request.user!.role === 'SALES_REP') {
        whereConditions.push(`v.sales_rep_id = $${pIdx++}`);
        values.push(request.user!.id);
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM visits v WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT v.id, v.attendance_id, v.client_id, c.business_name as client_name,
                v.sales_rep_id, u.full_name as sales_rep_name,
                v.scheduled_at, v.check_in_time, v.check_out_time,
                v.check_in_latitude, v.check_in_longitude,
                v.check_out_latitude, v.check_out_longitude,
                v.geofence_distance_meters, v.is_mock_gps,
                v.outcome, v.notes, v.recovery_slip_number, v.recovery_amount,
                v.order_id, o.order_number, v.created_at
         FROM visits v
         JOIN clients c ON c.id = v.client_id
         JOIN users u ON u.id = v.sales_rep_id
         LEFT JOIN orders o ON o.id = v.order_id
         WHERE ${whereClause}
         ORDER BY v.check_in_time DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/visits/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get single visit details including geofence distance and recovery slip',
        tags: ['Field Shop Visits'],
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
        `SELECT v.*, c.business_name as client_name, c.address as client_address,
                u.full_name as sales_rep_name, o.order_number
         FROM visits v
         JOIN clients c ON c.id = v.client_id
         JOIN users u ON u.id = v.sales_rep_id
         LEFT JOIN orders o ON o.id = v.order_id
         WHERE v.id = $1`,
        [id]
      );

      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Visit not found' });
      }

      return reply.send({ success: true, data: result.rows[0] });
    }
  );

  // POST /api/v1/visits/check-in
  fastify.post(
    '/check-in',
    {
      schema: {
        description: 'Check-in at client location with geofence distance calculation and mock GPS detection',
        tags: ['Field Shop Visits'],
        body: {
          type: 'object',
          required: ['client_id', 'check_in_latitude', 'check_in_longitude'],
          properties: {
            client_id: { type: 'string' },
            attendance_id: { type: 'string', nullable: true },
            scheduled_at: { type: 'string', nullable: true },
            check_in_latitude: { type: 'number' },
            check_in_longitude: { type: 'number' },
            is_mock_gps: { type: 'boolean', default: false },
            outcome: {
              type: 'string',
              enum: ['ORDER_PLACED', 'PAYMENT_COLLECTED', 'STOCK_AUDIT_ONLY', 'CLIENT_CLOSED', 'NO_ORDER', 'MERCHANDISING_ONLY'],
              default: 'NO_ORDER',
            },
            notes: { type: 'string', nullable: true },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;
      const salesRepId = request.user!.id;

      // Fetch client location to compute geofence distance
      const clientRes = await query(`SELECT latitude, longitude FROM clients WHERE id = $1`, [body.client_id]);
      if (clientRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Client not found' });
      }

      const clientLat = parseFloat(clientRes.rows[0].latitude);
      const clientLng = parseFloat(clientRes.rows[0].longitude);

      const geofenceDistanceMeters = calculateDistanceMeters(
        body.check_in_latitude,
        body.check_in_longitude,
        clientLat,
        clientLng
      );

      const result = await query(
        `INSERT INTO visits (
           client_id, sales_rep_id, attendance_id, scheduled_at,
           check_in_time, check_in_latitude, check_in_longitude,
           geofence_distance_meters, is_mock_gps, outcome, notes
         ) VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP, $5, $6, $7, $8, $9, $10)
         RETURNING *`,
        [
          body.client_id,
          salesRepId,
          body.attendance_id || null,
          body.scheduled_at || null,
          body.check_in_latitude,
          body.check_in_longitude,
          geofenceDistanceMeters,
          body.is_mock_gps || false,
          body.outcome || 'NO_ORDER',
          body.notes || null,
        ]
      );

      return reply.status(201).send({
        success: true,
        message: `Visit check-in recorded. Geofence distance: ${geofenceDistanceMeters}m`,
        data: result.rows[0],
      });
    }
  );

  // PUT /api/v1/visits/:id/check-out
  fastify.put(
    '/:id/check-out',
    {
      schema: {
        description: 'Complete visit check-out, record outcome, recovery cash collection & update ledger',
        tags: ['Field Shop Visits'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          required: ['check_out_latitude', 'check_out_longitude', 'outcome'],
          properties: {
            check_out_latitude: { type: 'number' },
            check_out_longitude: { type: 'number' },
            outcome: {
              type: 'string',
              enum: ['ORDER_PLACED', 'PAYMENT_COLLECTED', 'STOCK_AUDIT_ONLY', 'CLIENT_CLOSED', 'NO_ORDER', 'MERCHANDISING_ONLY'],
            },
            notes: { type: 'string', nullable: true },
            recovery_slip_number: { type: 'string', nullable: true },
            recovery_amount: { type: 'number', minimum: 0, default: 0 },
            order_id: { type: 'string', nullable: true },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const body = request.body as any;

      const visit = await withTransaction(async (client) => {
        const vRes = await client.query(`SELECT * FROM visits WHERE id = $1 FOR UPDATE`, [id]);
        if (vRes.rows.length === 0) {
          throw new Error('Visit not found');
        }

        const currentVisit = vRes.rows[0];
        const recoveryAmount = parseFloat(body.recovery_amount || 0);

        // Update visit record
        const updateRes = await client.query(
          `UPDATE visits
           SET check_out_time = CURRENT_TIMESTAMP,
               check_out_latitude = $1,
               check_out_longitude = $2,
               outcome = $3,
               notes = COALESCE($4, notes),
               recovery_slip_number = $5,
               recovery_amount = $6,
               order_id = $7
           WHERE id = $8
           RETURNING *`,
          [
            body.check_out_latitude,
            body.check_out_longitude,
            body.outcome,
            body.notes || null,
            body.recovery_slip_number || null,
            recoveryAmount,
            body.order_id || null,
            id,
          ]
        );

        const updatedVisit = updateRes.rows[0];

        // If recovery amount collected > 0, decrease client outstanding balance & add ledger entry
        if (recoveryAmount > 0) {
          const cRes = await client.query(
            `SELECT outstanding_balance FROM clients WHERE id = $1 FOR UPDATE`,
            [currentVisit.client_id]
          );
          const currentBal = parseFloat(cRes.rows[0].outstanding_balance);
          const newBal = Math.max(0, currentBal - recoveryAmount);

          await client.query(`UPDATE clients SET outstanding_balance = $1 WHERE id = $2`, [
            newBal,
            currentVisit.client_id,
          ]);

          await client.query(
            `INSERT INTO client_ledger_entries (
               client_id, entry_type, amount, balance_after,
               reference_type, reference_id, description, recorded_by
             ) VALUES ($1, 'PAYMENT', $2, $3, 'RECOVERY_SLIP', $4, $5, $6)`,
            [
              currentVisit.client_id,
              recoveryAmount,
              newBal,
              body.recovery_slip_number || `REC-${id.slice(0, 8)}`,
              `Payment recovery collected during field visit`,
              request.user!.id,
            ]
          );
        }

        return updatedVisit;
      });

      // Dispatch Webhook event `visit.completed`
      WebhookDispatcher.dispatch('visit.completed', visit);

      return reply.send({
        success: true,
        message: 'Visit completed successfully',
        data: visit,
      });
    }
  );
};
