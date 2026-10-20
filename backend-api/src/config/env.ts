import dotenv from 'dotenv';
import path from 'path';

// Load .env from backend-api directory or parent
dotenv.config({ path: path.resolve(__dirname, '../../.env') });

export const env = {
  NODE_ENV: process.env.NODE_ENV || 'development',
  PORT: parseInt(process.env.PORT || '4000', 10),
  HOST: process.env.HOST || '0.0.0.0',

  // Database settings
  DATABASE_URL: process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/enol_salesforze',
  DB_HOST: process.env.DB_HOST || 'localhost',
  DB_PORT: parseInt(process.env.DB_PORT || '5432', 10),
  DB_USER: process.env.DB_USER || 'postgres',
  DB_PASSWORD: process.env.DB_PASSWORD || 'postgres',
  DB_NAME: process.env.DB_NAME || 'enol_salesforze',
  DB_SSL: process.env.DB_SSL === 'true',
  DB_MAX_CONNECTIONS: parseInt(process.env.DB_MAX_CONNECTIONS || '20', 10),

  // Auth & Security
  JWT_SECRET: process.env.JWT_SECRET || 'super_secret_jwt_key_enol_salesforze_2026_enterprise',
  JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || '7d',

  // Webhooks
  WEBHOOK_MAX_RETRIES: parseInt(process.env.WEBHOOK_MAX_RETRIES || '3', 10),
  WEBHOOK_TIMEOUT_MS: parseInt(process.env.WEBHOOK_TIMEOUT_MS || '5000', 10),
  WEBHOOK_DEFAULT_SECRET: process.env.WEBHOOK_DEFAULT_SECRET || 'whsec_default_fallback_secret_enol_salesforze',

  // Defaults
  DEFAULT_PAGE_SIZE: parseInt(process.env.DEFAULT_PAGE_SIZE || '25', 10),
  MAX_PAGE_SIZE: parseInt(process.env.MAX_PAGE_SIZE || '100', 10),
};
