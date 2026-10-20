import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import bcrypt from 'bcryptjs';
import crypto from 'crypto';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { env } from '../config/env';

export const authRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  // POST /api/v1/auth/login
  fastify.post(
    '/login',
    {
      schema: {
        description: 'Authenticate user with email and password, returning JWT Bearer token and user profile',
        tags: ['Authentication'],
        body: {
          type: 'object',
          required: ['email', 'password'],
          properties: {
            email: { type: 'string', format: 'email' },
            password: { type: 'string' },
          },
        },
        response: {
          200: {
            type: 'object',
            properties: {
              success: { type: 'boolean' },
              token: { type: 'string' },
              user: {
                type: 'object',
                properties: {
                  id: { type: 'string' },
                  email: { type: 'string' },
                  full_name: { type: 'string' },
                  phone: { type: 'string' },
                  role: { type: 'string' },
                  manager_id: { type: 'string', nullable: true },
                  hierarchy_path: { type: 'string' },
                  vehicle_type: { type: 'string' },
                  vehicle_number: { type: 'string', nullable: true },
                  fuel_rate_per_km: { type: 'number' },
                  daily_allowance: { type: 'number' },
                },
              },
            },
          },
          401: {
            type: 'object',
            properties: {
              success: { type: 'boolean' },
              error: { type: 'string' },
            },
          },
        },
      },
    },
    async (request, reply) => {
      const { email, password } = request.body as { email: string; password: string };

      const result = await query(
        `SELECT id, email, password_hash, full_name, phone, role, manager_id, hierarchy_path,
                vehicle_type, vehicle_number, fuel_rate_per_km, daily_allowance, is_active
         FROM users 
         WHERE LOWER(email) = LOWER($1)`,
        [email]
      );

      if (result.rows.length === 0) {
        return reply.status(401).send({ success: false, error: 'Invalid email or password' });
      }

      const user = result.rows[0];
      if (!user.is_active) {
        return reply.status(401).send({ success: false, error: 'User account is deactivated' });
      }

      const isMatch = await bcrypt.compare(password, user.password_hash);
      if (!isMatch) {
        return reply.status(401).send({ success: false, error: 'Invalid email or password' });
      }

      // Generate JWT
      const token = fastify.jwt.sign(
        {
          id: user.id,
          email: user.email,
          role: user.role,
        },
        { expiresIn: env.JWT_EXPIRES_IN }
      );

      delete user.password_hash;
      delete user.is_active;

      return reply.send({
        success: true,
        token,
        user,
      });
    }
  );

  // GET /api/v1/auth/me
  fastify.get(
    '/me',
    {
      preHandler: [authenticate],
      schema: {
        description: 'Get profile details of currently authenticated user',
        tags: ['Authentication'],
        security: [{ BearerAuth: [] }, { ApiKeyAuth: [] }],
      },
    },
    async (request, reply) => {
      const user = request.user!;
      const result = await query(
        `SELECT u.id, u.email, u.full_name, u.phone, u.role, u.manager_id, u.hierarchy_path,
                u.bound_device_id, u.vehicle_type, u.vehicle_number, u.fuel_rate_per_km,
                u.daily_allowance, u.created_at, u.updated_at,
                m.full_name as manager_name, m.role as manager_role
         FROM users u
         LEFT JOIN users m ON m.id = u.manager_id
         WHERE u.id = $1`,
        [user.id]
      );

      return reply.send({
        success: true,
        user: result.rows[0],
        authMethod: user.authMethod,
      });
    }
  );

  // POST /api/v1/auth/api-keys
  fastify.post(
    '/api-keys',
    {
      preHandler: [authenticate],
      schema: {
        description: 'Generate a new API key for external OMS/ERP or backend integration',
        tags: ['Authentication'],
        security: [{ BearerAuth: [] }],
        body: {
          type: 'object',
          required: ['key_name'],
          properties: {
            key_name: { type: 'string' },
            scopes: { type: 'array', items: { type: 'string' } },
            expires_in_days: { type: 'integer', minimum: 1 },
          },
        },
      },
    },
    async (request, reply) => {
      const { key_name, scopes = ['read', 'write'], expires_in_days } = request.body as any;
      const rawSecret = `enol_live_${crypto.randomBytes(24).toString('hex')}`;
      const keyPrefix = rawSecret.slice(0, 14);
      const keyHash = crypto.createHash('sha256').update(rawSecret).digest('hex');

      let expiresAt: string | null = null;
      if (expires_in_days) {
        const d = new Date();
        d.setDate(d.getDate() + expires_in_days);
        expiresAt = d.toISOString();
      }

      const result = await query(
        `INSERT INTO api_keys (user_id, key_name, key_hash, key_prefix, scopes, expires_at)
         VALUES ($1, $2, $3, $4, $5, $6)
         RETURNING id, key_name, key_prefix, scopes, expires_at, created_at`,
        [request.user!.id, key_name, keyHash, keyPrefix, scopes, expiresAt]
      );

      return reply.status(201).send({
        success: true,
        message: 'API Key generated successfully. Save this secret securely as it cannot be shown again.',
        apiKey: rawSecret,
        keyDetails: result.rows[0],
      });
    }
  );

  // GET /api/v1/auth/api-keys
  fastify.get(
    '/api-keys',
    {
      preHandler: [authenticate],
      schema: {
        description: 'List API keys owned by currently authenticated user',
        tags: ['Authentication'],
        security: [{ BearerAuth: [] }],
      },
    },
    async (request, reply) => {
      const result = await query(
        `SELECT id, key_name, key_prefix, scopes, is_active, expires_at, created_at, updated_at
         FROM api_keys
         WHERE user_id = $1
         ORDER BY created_at DESC`,
        [request.user!.id]
      );

      return reply.send({
        success: true,
        data: result.rows,
      });
    }
  );
};
