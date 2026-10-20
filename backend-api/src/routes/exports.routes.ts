import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { ExportService } from '../services/export.service';

export const exportRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/exports/:entity
  fastify.get(
    '/:entity',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM'])],
      schema: {
        description: 'Bulk data export endpoint generating streaming CSV or JSON for data warehouse and enterprise BI integration',
        tags: ['Bulk Data Exports'],
        security: [{ BearerAuth: [] }, { ApiKeyAuth: [] }],
        params: {
          type: 'object',
          required: ['entity'],
          properties: {
            entity: {
              type: 'string',
              enum: [
                'users',
                'clients',
                'districts',
                'beats',
                'products',
                'orders',
                'visits',
                'attendance',
                'breadcrumbs',
                'tada-claims',
                'mechanics',
                'kpis',
                'ledger',
              ],
            },
          },
        },
        querystring: {
          type: 'object',
          properties: {
            format: { type: 'string', enum: ['json', 'csv'], default: 'json' },
            from: { type: 'string', description: 'Start date / timestamp filter' },
            to: { type: 'string', description: 'End date / timestamp filter' },
            limit: { type: 'integer', default: 5000, maximum: 10000 },
          },
        },
      },
    },
    async (request, reply) => {
      const { entity } = request.params as { entity: string };
      const { format = 'json', from, to, limit } = request.query as any;

      try {
        const { contentType, data, filename } = await ExportService.exportEntity(entity, format as any, {
          from,
          to,
          limit,
        });

        reply
          .header('Content-Type', contentType)
          .header('Content-Disposition', `attachment; filename="${filename}"`)
          .send(data);
      } catch (err: any) {
        return reply.status(400).send({
          success: false,
          error: err.message,
        });
      }
    }
  );
};
