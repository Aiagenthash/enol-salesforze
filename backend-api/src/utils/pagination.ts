import { FastifyRequest } from 'fastify';
import { PaginationParams, PaginatedResponse } from '../types';
import { env } from '../config/env';

export function parsePagination(request: FastifyRequest): Required<PaginationParams> {
  const query = request.query as any;
  const page = Math.max(1, parseInt(query.page || '1', 10));
  const rawLimit = parseInt(query.limit || String(env.DEFAULT_PAGE_SIZE), 10);
  const limit = Math.min(env.MAX_PAGE_SIZE, Math.max(1, rawLimit));
  const sort = (query.sort as string) || 'created_at';
  const order = (query.order as string)?.toLowerCase() === 'asc' ? 'asc' : 'desc';
  const search = ((query.search as string) || '').trim();

  return { page, limit, sort, order, search };
}

export function formatPaginatedResponse<T>(
  data: T[],
  total: number,
  page: number,
  limit: number
): PaginatedResponse<T> {
  const totalPages = Math.ceil(total / limit) || 1;
  return {
    success: true,
    data,
    pagination: {
      total,
      page,
      limit,
      totalPages,
      hasNextPage: page < totalPages,
      hasPrevPage: page > 1,
    },
  };
}
