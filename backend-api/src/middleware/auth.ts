import { FastifyRequest, FastifyReply } from 'fastify';
import crypto from 'crypto';
import { query } from '../config/database';
import { AuthenticatedUser } from '../types';

declare module '@fastify/jwt' {
  interface FastifyJWT {
    payload: { id: string; email: string; role: string };
    user: AuthenticatedUser;
  }
}

export async function authenticate(request: FastifyRequest, reply: FastifyReply): Promise<void> {
  const apiKeyHeader = request.headers['x-api-key'] as string | undefined;
  const authHeader = request.headers.authorization;

  // 1. Try API Key Authentication
  if (apiKeyHeader) {
    const keyHash = crypto.createHash('sha256').update(apiKeyHeader.trim()).digest('hex');
    try {
      const result = await query(
        `SELECT k.id as key_id, k.scopes, u.id, u.email, u.role, u.hierarchy_path, u.manager_id, u.full_name
         FROM api_keys k
         JOIN users u ON u.id = k.user_id
         WHERE k.key_hash = $1 
           AND k.is_active = TRUE 
           AND (k.expires_at IS NULL OR k.expires_at > CURRENT_TIMESTAMP)
           AND u.is_active = TRUE`,
        [keyHash]
      );

      if (result.rows.length > 0) {
        const row = result.rows[0];
        request.user = {
          id: row.id,
          email: row.email,
          role: row.role,
          hierarchy_path: row.hierarchy_path,
          manager_id: row.manager_id,
          full_name: row.full_name,
          authMethod: 'api_key',
          scopes: row.scopes,
        };
        return;
      } else {
        reply.status(401).send({
          success: false,
          error: 'Invalid or expired API Key',
        });
        return;
      }
    } catch (dbErr: any) {
      reply.status(401).send({
        success: false,
        error: 'Invalid or expired API Key',
        details: dbErr.message,
      });
      return;
    }
  }

  // 2. Try JWT Bearer Authentication
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.substring(7).trim();
    try {
      const decoded = await request.jwtVerify<{
        id: string;
        email: string;
        role: string;
      }>({ onlyCookie: false });

      const result = await query(
        `SELECT id, email, role, hierarchy_path, manager_id, full_name, is_active
         FROM users 
         WHERE id = $1 AND is_active = TRUE`,
        [decoded.id]
      );

      if (result.rows.length === 0) {
        reply.status(401).send({
          success: false,
          error: 'User account is deactivated or not found',
        });
        return;
      }

      const row = result.rows[0];
      request.user = {
        id: row.id,
        email: row.email,
        role: row.role,
        hierarchy_path: row.hierarchy_path,
        manager_id: row.manager_id,
        full_name: row.full_name,
        authMethod: 'jwt',
      };
      return;
    } catch (err: any) {
      reply.status(401).send({
        success: false,
        error: 'Invalid or expired JWT token',
        details: err.message,
      });
      return;
    }
  }

  // No authentication credentials provided
  reply.status(401).send({
    success: false,
    error: 'Authentication required. Provide Bearer token or X-API-Key header.',
  });
}

export async function optionalAuth(request: FastifyRequest, reply: FastifyReply): Promise<void> {
  const apiKeyHeader = request.headers['x-api-key'] as string | undefined;
  const authHeader = request.headers.authorization;

  if (apiKeyHeader || (authHeader && authHeader.startsWith('Bearer '))) {
    try {
      await authenticate(request, reply);
    } catch {
      // Ignore in optional mode
    }
  }
}
