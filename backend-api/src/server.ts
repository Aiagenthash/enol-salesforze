import { buildApp } from './app';
import { env } from './config/env';
import { pool } from './config/database';

export async function startServer() {
  const app = buildApp();

  // Test database connection on startup
  try {
    const dbTest = await pool.query('SELECT NOW() as current_time');
    app.log.info(`[Database] PostgreSQL connected successfully at ${dbTest.rows[0].current_time}`);
  } catch (err: any) {
    app.log.error(`[Database] Failed connecting to PostgreSQL (${env.DATABASE_URL}): ${err.message}`);
    console.warn('Note: API is operating. Ensure PostgreSQL with PostGIS is running and migrations are applied.');
  }

  // Graceful shutdown handling
  const signals: NodeJS.Signals[] = ['SIGINT', 'SIGTERM'];
  for (const sig of signals) {
    process.on(sig, async () => {
      app.log.info(`Received ${sig}. Shutting down gracefully...`);
      await app.close();
      await pool.end();
      process.exit(0);
    });
  }

  try {
    await app.listen({ port: env.PORT, host: env.HOST });
    console.log(`
=============================================================================
  ENOL SALESFORZE - FIELD FORCE AUTOMATION & TERRITORY MANAGEMENT API
=============================================================================
  REST API Server:       http://${env.HOST === '0.0.0.0' ? 'localhost' : env.HOST}:${env.PORT}
  OpenAPI 3.1 Swagger:   http://${env.HOST === '0.0.0.0' ? 'localhost' : env.HOST}:${env.PORT}/api/v1/docs
  Delta-Sync Endpoint:   http://${env.HOST === '0.0.0.0' ? 'localhost' : env.HOST}:${env.PORT}/api/v1/sync/delta
  Health Check:          http://${env.HOST === '0.0.0.0' ? 'localhost' : env.HOST}:${env.PORT}/health
=============================================================================
    `);
  } catch (err) {
    app.log.error(err);
    process.exit(1);
  }
}
