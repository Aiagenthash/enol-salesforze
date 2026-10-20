import Fastify, { FastifyInstance } from 'fastify';
import cors from '@fastify/cors';
import fastifyJwt from '@fastify/jwt';
import swagger from '@fastify/swagger';
import swaggerUi from '@fastify/swagger-ui';
import { env } from './config/env';

// Import domain route plugins
import { authRoutes } from './routes/auth.routes';
import { userRoutes } from './routes/users.routes';
import { territoryRoutes } from './routes/territories.routes';
import { clientRoutes } from './routes/clients.routes';
import { mechanicRoutes } from './routes/mechanics.routes';
import { productRoutes } from './routes/products.routes';
import { orderRoutes } from './routes/orders.routes';
import { attendanceRoutes } from './routes/attendance.routes';
import { visitRoutes } from './routes/visits.routes';
import { breadcrumbRoutes } from './routes/breadcrumbs.routes';
import { tadaRoutes } from './routes/tada.routes';
import { kpiRoutes } from './routes/kpis.routes';
import { competitionRoutes } from './routes/competitions.routes';
import { syncRoutes } from './routes/sync.routes';
import { exportRoutes } from './routes/exports.routes';
import { webhookRoutes } from './routes/webhooks.routes';

export function buildApp(): FastifyInstance {
  const app = Fastify({
    logger: env.NODE_ENV === 'development' ? {
      transport: {
        target: 'pino-pretty',
        options: {
          translateTime: 'HH:MM:ss Z',
          ignore: 'pid,hostname',
        },
      },
    } : true,
    disableRequestLogging: env.NODE_ENV === 'test',
  });

  // Enable CORS
  app.register(cors, {
    origin: true,
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  });

  // Enable JWT
  app.register(fastifyJwt, {
    secret: env.JWT_SECRET,
  });

  // OpenAPI 3.1 & Swagger Specification
  app.register(swagger, {
    openapi: {
      openapi: '3.1.0',
      info: {
        title: 'ENOL SALESFORZE - Enterprise Field Force Automation & Territory Management API',
        description: `
### Overview
Backend REST API & Webhook Gateway for Automotive Lubricant Field Force Automation (FFA) in Pakistan.
Features:
- **5-Tier Organizational Hierarchy**: Director -> ZSM -> ASM -> Sales Rep -> Admin with materialized path queries
- **Territory & Route Beat Management**: PostGIS multi-polygon district boundaries & daily rep beat schedules
- **Retail & Workshop CRM**: Geocoded shop locations, credit limit hold enforcement & customer statement ledgers
- **Mechanic Influencer Loyalty Network**: QR code scans & digital wallet payouts (JazzCash, Easypaisa)
- **Lubricant SKU Catalog**: Carton units, pack sizes in liters, trade prices & automated commercial promotion schemes
- **Order Booking & Idempotency**: Offline order booking with idempotency keys & automatic credit balance verification
- **Shift & Attendance Tracking**: Shift punch-in/out with selfie verification & odometer image telemetry
- **Field Visit Audit**: Real-time geofence distance calculation, mock GPS detection, and cash recovery slips
- **GPS Breadcrumb Tracking**: Real-time location stream & route playback LineString generation
- **TADA Claim Fraud Audit**: Automated odometer delta vs GPS distance comparison with variance % fraud flagging
- **Sales Gamification & KPIs**: Real-time leaderboard rankings, target scorecards, and incentive races
- **Offline Delta Sync**: High-efficiency delta synchronization for field mobile apps
- **Bulk Data Exports**: High-throughput streaming CSV and JSON exports for enterprise data lakes
- **Outbound Webhook Dispatcher**: Asynchronous event dispatch with HMAC-SHA256 signatures & auto-retry backoff
        `,
        version: '1.0.0',
        contact: {
          name: 'ENOL Salesforze Architecture Team',
          email: 'architecture@enollubricants.com',
        },
      },
      servers: [
        {
          url: 'http://localhost:4000',
          description: 'Local Development Server',
        },
        {
          url: 'https://api.salesforze.enollubricants.com',
          description: 'Production Enterprise Cluster',
        },
      ],
      components: {
        securitySchemes: {
          BearerAuth: {
            type: 'http',
            scheme: 'bearer',
            bearerFormat: 'JWT',
            description: 'Enter your JWT Bearer token obtained from POST /api/v1/auth/login',
          },
          ApiKeyAuth: {
            type: 'apiKey',
            name: 'X-API-Key',
            in: 'header',
            description: 'API Key for backend integrations, external OMS/ERP connectors, or automated pipelines',
          },
        },
      },
      tags: [
        { name: 'Authentication', description: 'User login, token verification, and API key management' },
        { name: 'Users & Hierarchy', description: '5-Tier organizational structure and employee materialized paths' },
        { name: 'Territories & Beats', description: 'Pakistan provincial districts and daily field sales beat routes' },
        { name: 'Clients & Ledger', description: 'Retailers, workshops, geocoded locations, and statement of accounts' },
        { name: 'Mechanics & Loyalty', description: 'Mechanic loyalty network, QR code scans, and mobile wallet rewards' },
        { name: 'Product Catalog', description: 'Automotive lubricant SKUs, carton packaging, and trade schemes' },
        { name: 'Orders & Line Items', description: 'Idempotent order placement, item line calculations, and credit hold' },
        { name: 'Daily Attendance & Shifts', description: 'Shift start/end with photo selfies and odometer readings' },
        { name: 'Field Shop Visits', description: 'Client visits, geofence distance verification, and payment recoveries' },
        { name: 'Location Breadcrumbs & GPS', description: 'GPS location tracking stream and route playback LineString' },
        { name: 'TADA Claims & Allowances', description: 'Travel allowance audit comparing odometer delta vs GPS distance' },
        { name: 'Targets & KPIs', description: 'Sales volume/revenue targets and employee scorecard performance' },
        { name: 'Sales Competitions & Gamification', description: 'Volume races, incentive rewards, and real-time leaderboards' },
        { name: 'Offline Delta Sync', description: 'Differential synchronization for field mobile devices' },
        { name: 'Bulk Data Exports', description: 'High-volume streaming CSV and JSON exports' },
        { name: 'Outbound Webhooks Gateway', description: 'Event subscription management and HMAC-SHA256 signature dispatch' },
      ],
    },
  });

  // Mount Swagger UI at /api/v1/docs
  app.register(swaggerUi, {
    routePrefix: '/api/v1/docs',
    uiConfig: {
      docExpansion: 'list',
      deepLinking: true,
      displayRequestDuration: true,
    },
    staticCSP: true,
    transformStaticCSP: (header) => header,
  });

  // Health check routes
  app.get('/health', async () => ({ status: 'UP', service: 'enol-salesforze-api', timestamp: new Date().toISOString() }));
  app.get('/api/v1/health', async () => ({ status: 'UP', service: 'enol-salesforze-api', timestamp: new Date().toISOString() }));

  // Register all 12 domains + Core Services under /api/v1
  app.register(authRoutes, { prefix: '/api/v1/auth' });
  app.register(userRoutes, { prefix: '/api/v1/users' });
  app.register(territoryRoutes, { prefix: '/api/v1/territories' });
  app.register(clientRoutes, { prefix: '/api/v1/clients' });
  app.register(mechanicRoutes, { prefix: '/api/v1/mechanics' });
  app.register(productRoutes, { prefix: '/api/v1/products' });
  app.register(orderRoutes, { prefix: '/api/v1/orders' });
  app.register(attendanceRoutes, { prefix: '/api/v1/attendance' });
  app.register(visitRoutes, { prefix: '/api/v1/visits' });
  app.register(breadcrumbRoutes, { prefix: '/api/v1/breadcrumbs' });
  app.register(tadaRoutes, { prefix: '/api/v1/tada-claims' });
  app.register(kpiRoutes, { prefix: '/api/v1/kpis' });
  app.register(competitionRoutes, { prefix: '/api/v1/competitions' });
  app.register(syncRoutes, { prefix: '/api/v1/sync' });
  app.register(exportRoutes, { prefix: '/api/v1/exports' });
  app.register(webhookRoutes, { prefix: '/api/v1/webhooks' });

  // Global Error Handler
  app.setErrorHandler((error, request, reply) => {
    app.log.error(error);
    const statusCode = error.statusCode || 500;
    reply.status(statusCode).send({
      success: false,
      error: error.message || 'Internal Server Error',
      statusCode,
    });
  });

  return app;
}
