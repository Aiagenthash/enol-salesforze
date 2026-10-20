import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import bcrypt from 'bcryptjs';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';

export const userRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // GET /api/v1/users
  fastify.get(
    '/',
    {
      schema: {
        description: 'Get paginated list of users with role, manager, and search filtering',
        tags: ['Users & Hierarchy'],
        security: [{ BearerAuth: [] }, { ApiKeyAuth: [] }],
        querystring: {
          type: 'object',
          properties: {
            page: { type: 'integer', default: 1 },
            limit: { type: 'integer', default: 25 },
            role: { type: 'string', enum: ['DIRECTOR', 'ZSM', 'ASM', 'SALES_REP', 'ADMIN'] },
            manager_id: { type: 'string' },
            search: { type: 'string' },
            is_active: { type: 'boolean' },
          },
        },
      },
    },
    async (request, reply) => {
      const { page, limit, sort, order, search } = parsePagination(request);
      const queryParams = request.query as any;

      let whereConditions: string[] = ['1=1'];
      let values: any[] = [];
      let pIdx = 1;

      if (queryParams.role) {
        whereConditions.push(`u.role = $${pIdx++}`);
        values.push(queryParams.role);
      }
      if (queryParams.manager_id) {
        whereConditions.push(`u.manager_id = $${pIdx++}`);
        values.push(queryParams.manager_id);
      }
      if (queryParams.is_active !== undefined) {
        whereConditions.push(`u.is_active = $${pIdx++}`);
        values.push(queryParams.is_active === 'true' || queryParams.is_active === true);
      }
      if (search) {
        whereConditions.push(`(u.full_name ILIKE $${pIdx} OR u.email ILIKE $${pIdx} OR u.phone ILIKE $${pIdx})`);
        values.push(`%${search}%`);
        pIdx++;
      }

      // Hierarchy constraint: SALES_REP and ASM only view users under their tree or self
      if (request.user!.role === 'SALES_REP' || request.user!.role === 'ASM' || request.user!.role === 'ZSM') {
        whereConditions.push(`u.hierarchy_path LIKE $${pIdx++}`);
        values.push(`${request.user!.hierarchy_path}%`);
      }

      const whereClause = whereConditions.join(' AND ');

      // Count total
      const countRes = await query(
        `SELECT COUNT(*) FROM users u WHERE ${whereClause}`,
        values
      );
      const total = parseInt(countRes.rows[0].count, 10);

      // Data query
      const offset = (page - 1) * limit;
      const validSortCols: Record<string, string> = {
        created_at: 'u.created_at',
        full_name: 'u.full_name',
        role: 'u.role',
        email: 'u.email',
      };
      const sortCol = validSortCols[sort] || 'u.created_at';

      const dataRes = await query(
        `SELECT u.id, u.email, u.full_name, u.phone, u.role, u.manager_id, u.hierarchy_path,
                u.bound_device_id, u.vehicle_type, u.vehicle_number, u.fuel_rate_per_km,
                u.daily_allowance, u.is_active, u.created_at, u.updated_at,
                m.full_name as manager_name
         FROM users u
         LEFT JOIN users m ON m.id = u.manager_id
         WHERE ${whereClause}
         ORDER BY ${sortCol} ${order}
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/users/:id
  fastify.get(
    '/:id',
    {
      schema: {
        description: 'Get user details by ID including manager info and direct subordinate count',
        tags: ['Users & Hierarchy'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const userRes = await query(
        `SELECT u.id, u.email, u.full_name, u.phone, u.role, u.manager_id, u.hierarchy_path,
                u.bound_device_id, u.vehicle_type, u.vehicle_number, u.fuel_rate_per_km,
                u.daily_allowance, u.is_active, u.created_at, u.updated_at,
                m.full_name as manager_name, m.role as manager_role,
                (SELECT COUNT(*) FROM users s WHERE s.hierarchy_path LIKE u.hierarchy_path || '%' AND s.id <> u.id) as total_subordinates
         FROM users u
         LEFT JOIN users m ON m.id = u.manager_id
         WHERE u.id = $1`,
        [id]
      );

      if (userRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'User not found' });
      }

      return reply.send({ success: true, data: userRes.rows[0] });
    }
  );

  // GET /api/v1/users/:id/subordinates
  fastify.get(
    '/:id/subordinates',
    {
      schema: {
        description: 'Get tree of all hierarchical subordinates down the materialized path for given user',
        tags: ['Users & Hierarchy'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const leaderRes = await query(`SELECT hierarchy_path FROM users WHERE id = $1`, [id]);
      if (leaderRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'User not found' });
      }

      const path = leaderRes.rows[0].hierarchy_path;
      const subordinatesRes = await query(
        `SELECT id, email, full_name, phone, role, manager_id, hierarchy_path, vehicle_type, vehicle_number, is_active
         FROM users
         WHERE hierarchy_path LIKE $1 AND id <> $2
         ORDER BY hierarchy_path ASC`,
        [`${path}%`, id]
      );

      return reply.send({
        success: true,
        count: subordinatesRes.rows.length,
        subordinates: subordinatesRes.rows,
      });
    }
  );

  // POST /api/v1/users
  fastify.post(
    '/',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM'])],
      schema: {
        description: 'Create a new user in the 5-tier sales force organization',
        tags: ['Users & Hierarchy'],
        body: {
          type: 'object',
          required: ['email', 'password', 'full_name', 'phone', 'role'],
          properties: {
            email: { type: 'string', format: 'email' },
            password: { type: 'string', minLength: 6 },
            full_name: { type: 'string' },
            phone: { type: 'string' },
            role: { type: 'string', enum: ['DIRECTOR', 'ZSM', 'ASM', 'SALES_REP', 'ADMIN'] },
            manager_id: { type: 'string', nullable: true },
            bound_device_id: { type: 'string', nullable: true },
            vehicle_type: { type: 'string', enum: ['MOTORCYCLE', 'CAR', 'VAN', 'OTHER'] },
            vehicle_number: { type: 'string', nullable: true },
            fuel_rate_per_km: { type: 'number' },
            daily_allowance: { type: 'number' },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;

      // Hash password
      const salt = await bcrypt.genSalt(10);
      const passwordHash = await bcrypt.hash(body.password, salt);

      try {
        const result = await query(
          `INSERT INTO users (
             email, password_hash, full_name, phone, role, manager_id,
             bound_device_id, vehicle_type, vehicle_number, fuel_rate_per_km, daily_allowance
           ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
           RETURNING id, email, full_name, phone, role, manager_id, hierarchy_path,
                     vehicle_type, vehicle_number, fuel_rate_per_km, daily_allowance, is_active, created_at`,
          [
            body.email,
            passwordHash,
            body.full_name,
            body.phone,
            body.role,
            body.manager_id || null,
            body.bound_device_id || null,
            body.vehicle_type || 'MOTORCYCLE',
            body.vehicle_number || null,
            body.fuel_rate_per_km || 7.50,
            body.daily_allowance || 500.00,
          ]
        );

        return reply.status(201).send({
          success: true,
          message: 'User created successfully',
          data: result.rows[0],
        });
      } catch (err: any) {
        if (err.code === '23505') {
          return reply.status(409).send({ success: false, error: 'User with this email or phone already exists' });
        }
        throw err;
      }
    }
  );

  // PUT /api/v1/users/:id
  fastify.put(
    '/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM', 'ASM'])],
      schema: {
        description: 'Update user profile, vehicle rates, or manager assignment',
        tags: ['Users & Hierarchy'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          properties: {
            full_name: { type: 'string' },
            phone: { type: 'string' },
            manager_id: { type: 'string', nullable: true },
            bound_device_id: { type: 'string', nullable: true },
            vehicle_type: { type: 'string', enum: ['MOTORCYCLE', 'CAR', 'VAN', 'OTHER'] },
            vehicle_number: { type: 'string', nullable: true },
            fuel_rate_per_km: { type: 'number' },
            daily_allowance: { type: 'number' },
            is_active: { type: 'boolean' },
          },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const body = request.body as any;

      const fields: string[] = [];
      const values: any[] = [];
      let pIdx = 1;

      for (const [key, val] of Object.entries(body)) {
        if (val !== undefined && key !== 'id' && key !== 'password') {
          fields.push(`${key} = $${pIdx++}`);
          values.push(val);
        }
      }

      if (fields.length === 0) {
        return reply.status(400).send({ success: false, error: 'No fields provided for update' });
      }

      values.push(id);
      const sql = `UPDATE users SET ${fields.join(', ')} WHERE id = $${pIdx} RETURNING id, email, full_name, role, manager_id, hierarchy_path, vehicle_type, vehicle_number, fuel_rate_per_km, daily_allowance, is_active, updated_at`;

      const result = await query(sql, values);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'User not found' });
      }

      return reply.send({ success: true, message: 'User updated successfully', data: result.rows[0] });
    }
  );

  // DELETE /api/v1/users/:id
  fastify.delete(
    '/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Deactivate user account (soft delete)',
        tags: ['Users & Hierarchy'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };
      const result = await query(
        `UPDATE users SET is_active = FALSE WHERE id = $1 RETURNING id, email, is_active`,
        [id]
      );
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'User not found' });
      }
      return reply.send({ success: true, message: 'User deactivated successfully' });
    }
  );
};
