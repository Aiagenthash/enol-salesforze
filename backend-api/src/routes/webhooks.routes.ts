import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import crypto from 'crypto';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';
import { WebhookDispatcher } from '../services/webhook-dispatcher.service';

export const webhookRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // --------------------------------------------------------------------------
  // SUBSCRIPTIONS
  // --------------------------------------------------------------------------

  // GET /api/v1/webhooks/subscriptions
  fastify.get(
    '/subscriptions',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Get list of registered outbound webhook subscriptions',
        tags: ['Outbound Webhooks Gateway'],
      },
    },
    async (request, reply) => {
      const result = await query(
        `SELECT id, name, target_url, events, is_active, headers, created_at, updated_at
         FROM webhook_subscriptions
         ORDER BY created_at DESC`
      );

      return reply.send({
        success: true,
        data: result.rows,
      });
    }
  );

  // POST /api/v1/webhooks/subscriptions
  fastify.post(
    '/subscriptions',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Register a new outbound webhook subscriber endpoint with HMAC-SHA256 secret',
        tags: ['Outbound Webhooks Gateway'],
        body: {
          type: 'object',
          required: ['name', 'target_url', 'events'],
          properties: {
            name: { type: 'string' },
            target_url: { type: 'string', format: 'uri' },
            secret: { type: 'string', minLength: 16 },
            events: {
              type: 'array',
              minItems: 1,
              items: {
                type: 'string',
                enum: [
                  'order.created',
                  'order.approved',
                  'order.delivered',
                  'visit.completed',
                  'client.created',
                  'tada.submitted',
                ],
              },
            },
            headers: { type: 'object' },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;
      const secret = body.secret || `whsec_${crypto.randomBytes(24).toString('hex')}`;

      const result = await query(
        `INSERT INTO webhook_subscriptions (
           name, target_url, secret, events, headers
         ) VALUES ($1, $2, $3, $4, $5)
         RETURNING id, name, target_url, secret, events, is_active, headers, created_at`,
        [body.name, body.target_url, secret, body.events, JSON.stringify(body.headers || {})]
      );

      return reply.status(201).send({
        success: true,
        message: 'Webhook subscription created successfully. Use the signing secret to verify signatures.',
        data: result.rows[0],
      });
    }
  );

  // PUT /api/v1/webhooks/subscriptions/:id
  fastify.put(
    '/subscriptions/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Update webhook subscription URL, events, secret, or active state',
        tags: ['Outbound Webhooks Gateway'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            target_url: { type: 'string', format: 'uri' },
            secret: { type: 'string' },
            events: { type: 'array', items: { type: 'string' } },
            is_active: { type: 'boolean' },
            headers: { type: 'object' },
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
          values.push(key === 'headers' ? JSON.stringify(val) : val);
        }
      }

      if (fields.length === 0) {
        return reply.status(400).send({ success: false, error: 'No fields provided for update' });
      }

      values.push(id);
      const sql = `UPDATE webhook_subscriptions SET ${fields.join(', ')} WHERE id = $${pIdx} RETURNING id, name, target_url, events, is_active, headers, updated_at`;

      const result = await query(sql, values);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Webhook subscription not found' });
      }

      return reply.send({ success: true, message: 'Webhook subscription updated', data: result.rows[0] });
    }
  );

  // DELETE /api/v1/webhooks/subscriptions/:id
  fastify.delete(
    '/subscriptions/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Delete webhook subscription',
        tags: ['Outbound Webhooks Gateway'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const result = await query(`DELETE FROM webhook_subscriptions WHERE id = $1 RETURNING id`, [id]);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Webhook subscription not found' });
      }
      return reply.send({ success: true, message: 'Webhook subscription deleted successfully' });
    }
  );

  // --------------------------------------------------------------------------
  // DELIVERY AUDIT LOGS
  // --------------------------------------------------------------------------

  // GET /api/v1/webhooks/logs
  fastify.get(
    '/logs',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Get paginated audit logs of webhook delivery attempts, HTTP responses, and error traces',
        tags: ['Outbound Webhooks Gateway'],
        querystring: {
          type: 'object',
          properties: {
            subscription_id: { type: 'string' },
            event_name: { type: 'string' },
            failed_only: { type: 'boolean' },
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

      if (queryParams.subscription_id) {
        whereConditions.push(`l.subscription_id = $${pIdx++}`);
        values.push(queryParams.subscription_id);
      }
      if (queryParams.event_name) {
        whereConditions.push(`l.event_name = $${pIdx++}`);
        values.push(queryParams.event_name);
      }
      if (queryParams.failed_only === 'true' || queryParams.failed_only === true) {
        whereConditions.push(`(l.response_status IS NULL OR l.response_status >= 400)`);
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM webhook_delivery_logs l WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT l.id, l.subscription_id, s.name as subscription_name, s.target_url,
                l.event_name, l.payload, l.response_status, l.response_body,
                l.attempt_number, l.error_message, l.delivered_at, l.created_at
         FROM webhook_delivery_logs l
         JOIN webhook_subscriptions s ON s.id = l.subscription_id
         WHERE ${whereClause}
         ORDER BY l.created_at DESC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // POST /api/v1/webhooks/test-dispatch
  fastify.post(
    '/test-dispatch',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Trigger a simulated webhook event for external developers to verify HMAC-SHA256 signature verification',
        tags: ['Outbound Webhooks Gateway'],
        body: {
          type: 'object',
          required: ['event_name'],
          properties: {
            event_name: {
              type: 'string',
              enum: [
                'order.created',
                'order.approved',
                'order.delivered',
                'visit.completed',
                'client.created',
                'tada.submitted',
              ],
            },
            sample_payload: { type: 'object' },
          },
        },
      },
    },
    async (request, reply) => {
      const { event_name, sample_payload } = request.body as any;

      const payload = sample_payload || {
        event: event_name,
        test_mode: true,
        dispatched_at: new Date().toISOString(),
        triggered_by: request.user!.email,
        sample_data: {
          reference_id: `TEST-${crypto.randomBytes(4).toString('hex').toUpperCase()}`,
          message: 'Simulation of outbound event from ENOL Salesforze Webhook Gateway',
        },
      };

      await WebhookDispatcher.dispatch(event_name, payload);

      return reply.send({
        success: true,
        message: `Dispatched test event '${event_name}' to all matching active subscriptions`,
        payload,
      });
    }
  );
};
