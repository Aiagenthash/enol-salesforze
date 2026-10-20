import { FastifyRequest, FastifyReply } from 'fastify';
import { UserRole, AuthenticatedUser } from '../types';

export function requireRoles(allowedRoles: UserRole[]) {
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    if (!request.user) {
      reply.status(401).send({ success: false, error: 'Authentication required' });
      return;
    }

    if (!allowedRoles.includes(request.user.role)) {
      reply.status(403).send({
        success: false,
        error: `Forbidden: Role '${request.user.role}' is not authorized to access this resource. Required roles: ${allowedRoles.join(', ')}`,
      });
      return;
    }
  };
}

export function requireScopes(requiredScopes: string[]) {
  return async (request: FastifyRequest, reply: FastifyReply): Promise<void> => {
    if (!request.user) {
      reply.status(401).send({ success: false, error: 'Authentication required' });
      return;
    }

    // JWT users default to full access based on role; API keys enforce explicit scopes
    if (request.user.authMethod === 'api_key') {
      const userScopes = request.user.scopes || [];
      const hasAllScopes = requiredScopes.every((s) => userScopes.includes(s) || userScopes.includes('admin'));
      if (!hasAllScopes) {
        reply.status(403).send({
          success: false,
          error: `Forbidden: API key lacks required scope(s): ${requiredScopes.join(', ')}`,
        });
        return;
      }
    }
  };
}

/**
 * Builds SQL filter clause for 5-tier organizational hierarchy.
 * - ADMIN and DIRECTOR can see all records.
 * - ZSM, ASM, and SALES_REP can only see records belonging to themselves or their subordinates
 *   down their materialized path (hierarchy_path LIKE user.hierarchy_path || '%').
 */
export function getHierarchySqlFilter(
  user: AuthenticatedUser,
  userPathColumn: string,
  paramIndexStart: number
): { clause: string; params: any[] } {
  if (user.role === 'ADMIN' || user.role === 'DIRECTOR') {
    return { clause: '1=1', params: [] };
  }

  // Matches the user themselves or any subordinate whose hierarchy_path starts with the manager's path
  return {
    clause: `${userPathColumn} LIKE $${paramIndexStart}`,
    params: [`${user.hierarchy_path}%`],
  };
}
