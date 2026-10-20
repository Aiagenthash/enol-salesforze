import fs from 'fs';
import path from 'path';
import { pool } from '../config/database';

async function runSeed() {
  console.log('--- ENOL SALESFORZE: Seeding Initial Master Data ---');
  const seedFile = path.resolve(__dirname, '../../../db/migrations/003_seed_data.sql');

  if (!fs.existsSync(seedFile)) {
    console.error(`Seed file not found: ${seedFile}`);
    process.exit(1);
  }

  const client = await pool.connect();
  try {
    console.log('Executing 003_seed_data.sql...');
    const sql = fs.readFileSync(seedFile, 'utf-8');
    await client.query(sql);
    console.log('[SUCCESS] Seed data loaded successfully.');
  } catch (error) {
    console.error('Seeding failed:', error);
    process.exit(1);
  } finally {
    client.release();
    await pool.end();
  }
}

if (require.main === module) {
  runSeed();
}
