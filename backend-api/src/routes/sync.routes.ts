import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { authenticate } from '../middleware/auth';
import { SyncService } from '../services/sync.service';

export const syncRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/sync/delta
  fastify.get(
    '/delta',
    {
      schema: {
        description: 'Delta-Sync endpoint for offline-first mobile apps. Returns all records created or modified since specified ISO8601 timestamp.',
        tags: ['Offline Delta Sync'],
        security: [{ BearerAuth: [] }, { ApiKeyAuth: [] }],
        querystring: {
          type: 'object',
          properties: {
            since: {
              type: 'string',
              description: 'ISO 8601 timestamp of last successful sync (e.g. 2026-10-20T10:00:00Z)',
            },
          },
        },
        response: {
          200: {
            type: 'object',
            properties: {
              success: { type: 'boolean' },
              sync_timestamp: { type: 'string' },
              since: { type: 'string', nullable: true },
              counts: { type: 'object', additionalProperties: { type: 'integer' } },
              delta: {
                type: 'object',
                properties: {
                  users: { type: 'array', items: { type: 'object' } },
                  districts: { type: 'array', items: { type: 'object' } },
                  beats: { type: 'array', items: { type: 'object' } },
                  clients: { type: 'array', items: { type: 'object' } },
                  products: { type: 'array', items: { type: 'object' } },
                  orders: { type: 'array', items: { type: 'object' } },
                  mechanics: { type: 'array', items: { type: 'object' } },
                  kpis: { type: 'array', items: { type: 'object' } },
                  competitions: { type: 'array', items: { type: 'object' } },
                },
              },
            },
          },
        },
      },
    },
    async (request, reply) => {
      const { since } = request.query as { since?: string };

      if (since && isNaN(Date.parse(since))) {
        return reply.status(400).send({
          success: false,
          error: "Invalid 'since' timestamp format. Provide a valid ISO 8601 string (e.g. 2026-10-20T12:00:00Z)",
        });
      }

      const result = await SyncService.getDelta(since || null, request.user!);

      return reply.send({
        success: true,
        ...result,
      });
    }
  );
};
