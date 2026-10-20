import { FastifyInstance, FastifyPluginAsync } from 'fastify';
import { query } from '../config/database';
import { authenticate } from '../middleware/auth';
import { requireRoles } from '../middleware/rbac';
import { parsePagination, formatPaginatedResponse } from '../utils/pagination';

export const territoryRoutes: FastifyPluginAsync = async (fastify: FastifyInstance) => {
  fastify.addHook('preHandler', authenticate);

  // --------------------------------------------------------------------------
  // DISTRICTS
  // --------------------------------------------------------------------------

  // GET /api/v1/territories/districts
  fastify.get(
    '/districts',
    {
      schema: {
        description: 'Get list of Pakistan territory districts with assigned ZSM/ASM and counts',
        tags: ['Territories & Beats'],
        querystring: {
          type: 'object',
          properties: {
            province: { type: 'string' },
            assigned_zsm_id: { type: 'string' },
            assigned_asm_id: { type: 'string' },
            search: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const { page, limit, search } = parsePagination(request);
      const queryParams = request.query as any;

      let whereConditions = ['d.is_active = TRUE'];
      let values: any[] = [];
      let pIdx = 1;

      if (queryParams.province) {
        whereConditions.push(`d.province = $${pIdx++}`);
        values.push(queryParams.province);
      }
      if (queryParams.assigned_zsm_id) {
        whereConditions.push(`d.assigned_zsm_id = $${pIdx++}`);
        values.push(queryParams.assigned_zsm_id);
      }
      if (queryParams.assigned_asm_id) {
        whereConditions.push(`d.assigned_asm_id = $${pIdx++}`);
        values.push(queryParams.assigned_asm_id);
      }
      if (search) {
        whereConditions.push(`(d.name ILIKE $${pIdx} OR d.code ILIKE $${pIdx})`);
        values.push(`%${search}%`);
        pIdx++;
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM pakistan_districts d WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT d.id, d.name, d.code, d.province, d.polygon_geojson,
                d.assigned_zsm_id, z.full_name as zsm_name,
                d.assigned_asm_id, a.full_name as asm_name,
                d.is_active, d.created_at, d.updated_at,
                (SELECT COUNT(*) FROM territory_beats b WHERE b.district_id = d.id AND b.is_active = TRUE) as total_beats,
                (SELECT COUNT(*) FROM clients c WHERE c.district_id = d.id) as total_clients
         FROM pakistan_districts d
         LEFT JOIN users z ON z.id = d.assigned_zsm_id
         LEFT JOIN users a ON a.id = d.assigned_asm_id
         WHERE ${whereClause}
         ORDER BY d.name ASC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/territories/districts/:id
  fastify.get(
    '/districts/:id',
    {
      schema: {
        description: 'Get territory district detail with polygon boundaries and active beats',
        tags: ['Territories & Beats'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const distRes = await query(
        `SELECT d.id, d.name, d.code, d.province, d.polygon_geojson,
                d.assigned_zsm_id, z.full_name as zsm_name,
                d.assigned_asm_id, a.full_name as asm_name,
                d.is_active, d.created_at, d.updated_at
         FROM pakistan_districts d
         LEFT JOIN users z ON z.id = d.assigned_zsm_id
         LEFT JOIN users a ON a.id = d.assigned_asm_id
         WHERE d.id = $1`,
        [id]
      );

      if (distRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'District not found' });
      }

      const beatsRes = await query(
        `SELECT b.id, b.name, b.code, b.day_of_week, b.assigned_rep_id, u.full_name as rep_name
         FROM territory_beats b
         LEFT JOIN users u ON u.id = b.assigned_rep_id
         WHERE b.district_id = $1 AND b.is_active = TRUE
         ORDER BY b.name ASC`,
        [id]
      );

      return reply.send({
        success: true,
        data: {
          ...distRes.rows[0],
          beats: beatsRes.rows,
        },
      });
    }
  );

  // POST /api/v1/territories/districts
  fastify.post(
    '/districts',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR'])],
      schema: {
        description: 'Create a new territory district with boundary polygon GeoJSON',
        tags: ['Territories & Beats'],
        body: {
          type: 'object',
          required: ['name', 'code', 'province'],
          properties: {
            name: { type: 'string' },
            code: { type: 'string' },
            province: {
              type: 'string',
              enum: ['Punjab', 'Sindh', 'Khyber Pakhtunkhwa', 'Balochistan', 'Islamabad Capital Territory', 'Gilgit-Baltistan', 'Azad Jammu & Kashmir'],
            },
            polygon_geojson: { type: 'object' },
            assigned_zsm_id: { type: 'string', nullable: true },
            assigned_asm_id: { type: 'string', nullable: true },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;

      try {
        const result = await query(
          `INSERT INTO pakistan_districts (
             name, code, province, polygon_geojson, assigned_zsm_id, assigned_asm_id
           ) VALUES ($1, $2, $3, $4, $5, $6)
           RETURNING id, name, code, province, polygon_geojson, assigned_zsm_id, assigned_asm_id, created_at`,
          [
            body.name,
            body.code,
            body.province,
            body.polygon_geojson || null,
            body.assigned_zsm_id || null,
            body.assigned_asm_id || null,
          ]
        );

        return reply.status(201).send({
          success: true,
          message: 'District created successfully',
          data: result.rows[0],
        });
      } catch (err: any) {
        if (err.code === '23505') {
          return reply.status(409).send({ success: false, error: 'District code already exists' });
        }
        throw err;
      }
    }
  );

  // PUT /api/v1/territories/districts/:id
  fastify.put(
    '/districts/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM'])],
      schema: {
        description: 'Update district boundaries, name, or assigned managers',
        tags: ['Territories & Beats'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            polygon_geojson: { type: 'object' },
            assigned_zsm_id: { type: 'string', nullable: true },
            assigned_asm_id: { type: 'string', nullable: true },
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
        if (val !== undefined && key !== 'id') {
          fields.push(`${key} = $${pIdx++}`);
          values.push(val);
        }
      }

      if (fields.length === 0) {
        return reply.status(400).send({ success: false, error: 'No fields provided for update' });
      }

      values.push(id);
      const sql = `UPDATE pakistan_districts SET ${fields.join(', ')} WHERE id = $${pIdx} RETURNING id, name, code, province, assigned_zsm_id, assigned_asm_id, is_active, updated_at`;

      const result = await query(sql, values);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'District not found' });
      }

      return reply.send({ success: true, message: 'District updated successfully', data: result.rows[0] });
    }
  );

  // --------------------------------------------------------------------------
  // BEATS (Field Sales Rep Daily Routes)
  // --------------------------------------------------------------------------

  // GET /api/v1/territories/beats
  fastify.get(
    '/beats',
    {
      schema: {
        description: 'Get list of territory beats filtered by district, day of week, or assigned rep',
        tags: ['Territories & Beats'],
        querystring: {
          type: 'object',
          properties: {
            district_id: { type: 'string' },
            day_of_week: { type: 'string', enum: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] },
            assigned_rep_id: { type: 'string' },
            search: { type: 'string' },
          },
        },
      },
    },
    async (request, reply) => {
      const { page, limit, search } = parsePagination(request);
      const queryParams = request.query as any;

      let whereConditions = ['b.is_active = TRUE'];
      let values: any[] = [];
      let pIdx = 1;

      if (queryParams.district_id) {
        whereConditions.push(`b.district_id = $${pIdx++}`);
        values.push(queryParams.district_id);
      }
      if (queryParams.day_of_week) {
        whereConditions.push(`b.day_of_week = $${pIdx++}`);
        values.push(queryParams.day_of_week);
      }
      if (queryParams.assigned_rep_id) {
        whereConditions.push(`b.assigned_rep_id = $${pIdx++}`);
        values.push(queryParams.assigned_rep_id);
      }
      if (search) {
        whereConditions.push(`(b.name ILIKE $${pIdx} OR b.code ILIKE $${pIdx})`);
        values.push(`%${search}%`);
        pIdx++;
      }

      const whereClause = whereConditions.join(' AND ');

      const countRes = await query(`SELECT COUNT(*) FROM territory_beats b WHERE ${whereClause}`, values);
      const total = parseInt(countRes.rows[0].count, 10);

      const offset = (page - 1) * limit;
      const dataRes = await query(
        `SELECT b.id, b.district_id, d.name as district_name,
                b.name, b.code, b.day_of_week, b.assigned_rep_id,
                u.full_name as assigned_rep_name, b.is_active, b.created_at, b.updated_at,
                (SELECT COUNT(*) FROM clients c WHERE c.beat_id = b.id) as total_clients
         FROM territory_beats b
         JOIN pakistan_districts d ON d.id = b.district_id
         LEFT JOIN users u ON u.id = b.assigned_rep_id
         WHERE ${whereClause}
         ORDER BY b.name ASC
         LIMIT $${pIdx++} OFFSET $${pIdx++}`,
        [...values, limit, offset]
      );

      return reply.send(formatPaginatedResponse(dataRes.rows, total, page, limit));
    }
  );

  // GET /api/v1/territories/beats/:id
  fastify.get(
    '/beats/:id',
    {
      schema: {
        description: 'Get beat details and list of all clients on this beat route',
        tags: ['Territories & Beats'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
      },
    },
    async (request, reply) => {
      const { id } = request.params as { id: string };

      const beatRes = await query(
        `SELECT b.id, b.district_id, d.name as district_name,
                b.name, b.code, b.day_of_week, b.assigned_rep_id,
                u.full_name as assigned_rep_name, b.is_active, b.created_at, b.updated_at
         FROM territory_beats b
         JOIN pakistan_districts d ON d.id = b.district_id
         LEFT JOIN users u ON u.id = b.assigned_rep_id
         WHERE b.id = $1`,
        [id]
      );

      if (beatRes.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Beat not found' });
      }

      const clientsRes = await query(
        `SELECT c.id, c.business_name, c.owner_name, c.phone, c.address,
                c.latitude, c.longitude, c.credit_limit, c.outstanding_balance,
                c.credit_hold, c.approval_status
         FROM clients c
         WHERE c.beat_id = $1
         ORDER BY c.business_name ASC`,
        [id]
      );

      return reply.send({
        success: true,
        data: {
          ...beatRes.rows[0],
          clients: clientsRes.rows,
        },
      });
    }
  );

  // POST /api/v1/territories/beats
  fastify.post(
    '/beats',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM', 'ASM'])],
      schema: {
        description: 'Create a new beat route for field sales force',
        tags: ['Territories & Beats'],
        body: {
          type: 'object',
          required: ['district_id', 'name', 'code', 'day_of_week'],
          properties: {
            district_id: { type: 'string' },
            name: { type: 'string' },
            code: { type: 'string' },
            day_of_week: {
              type: 'string',
              enum: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
            },
            assigned_rep_id: { type: 'string', nullable: true },
          },
        },
      },
    },
    async (request, reply) => {
      const body = request.body as any;

      try {
        const result = await query(
          `INSERT INTO territory_beats (
             district_id, name, code, day_of_week, assigned_rep_id
           ) VALUES ($1, $2, $3, $4, $5)
           RETURNING id, district_id, name, code, day_of_week, assigned_rep_id, is_active, created_at`,
          [
            body.district_id,
            body.name,
            body.code,
            body.day_of_week,
            body.assigned_rep_id || null,
          ]
        );

        return reply.status(201).send({
          success: true,
          message: 'Beat created successfully',
          data: result.rows[0],
        });
      } catch (err: any) {
        if (err.code === '23505') {
          return reply.status(409).send({ success: false, error: 'Beat code already exists' });
        }
        throw err;
      }
    }
  );

  // PUT /api/v1/territories/beats/:id
  fastify.put(
    '/beats/:id',
    {
      preHandler: [requireRoles(['ADMIN', 'DIRECTOR', 'ZSM', 'ASM'])],
      schema: {
        description: 'Update beat details or rep assignment',
        tags: ['Territories & Beats'],
        params: {
          type: 'object',
          required: ['id'],
          properties: { id: { type: 'string' } },
        },
        body: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            day_of_week: {
              type: 'string',
              enum: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
            },
            assigned_rep_id: { type: 'string', nullable: true },
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
        if (val !== undefined && key !== 'id') {
          fields.push(`${key} = $${pIdx++}`);
          values.push(val);
        }
      }

      if (fields.length === 0) {
        return reply.status(400).send({ success: false, error: 'No fields provided for update' });
      }

      values.push(id);
      const sql = `UPDATE territory_beats SET ${fields.join(', ')} WHERE id = $${pIdx} RETURNING id, name, code, day_of_week, assigned_rep_id, is_active, updated_at`;

      const result = await query(sql, values);
      if (result.rows.length === 0) {
        return reply.status(404).send({ success: false, error: 'Beat not found' });
      }

      return reply.send({ success: true, message: 'Beat updated successfully', data: result.rows[0] });
    }
  );
};
