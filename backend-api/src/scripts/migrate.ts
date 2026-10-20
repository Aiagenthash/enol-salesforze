import fs from 'fs';
import path from 'path';
import { pool } from '../config/database';

async function runMigrations() {
  console.log('--- ENOL SALESFORZE: Running Database Migrations ---');
  const migrationsDir = path.resolve(__dirname, '../../../db/migrations');

  const migrationFiles = [
    '001_initial_schema.sql',
    '002_postgis_and_triggers.sql',
  ];

  const client = await pool.connect();
  try {
    for (const file of migrationFiles) {
      const filePath = path.join(migrationsDir, file);
      if (!fs.existsSync(filePath)) {
        throw new Error(`Migration file not found: ${filePath}`);
      }

      console.log(`Executing migration: ${file}...`);
      const sql = fs.readFileSync(filePath, 'utf-8');
      await client.query(sql);
      console.log(`[SUCCESS] Migration ${file} applied successfully.`);
    }

    console.log('--- All database migrations executed successfully ---');
  } catch (error) {
    console.error('Migration failed:', error);
    process.exit(1);
  } finally {
    client.release();
    await pool.end();
  }
}

if (require.main === module) {
  runMigrations();
}
