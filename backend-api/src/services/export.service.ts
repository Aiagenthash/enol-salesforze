import { query } from '../config/database';

export class ExportService {
  public static async exportEntity(
    entity: string,
    format: 'json' | 'csv' = 'json',
    filters: { from?: string; to?: string; limit?: number } = {}
  ): Promise<{ contentType: string; data: string; filename: string }> {
    const tableMap: Record<string, { table: string; dateCol: string; cols: string }> = {
      users: {
        table: 'users',
        dateCol: 'created_at',
        cols: 'id, email, full_name, phone, role, hierarchy_path, vehicle_type, vehicle_number, fuel_rate_per_km, daily_allowance, is_active, created_at',
      },
      clients: {
        table: 'clients',
        dateCol: 'created_at',
        cols: 'id, beat_id, district_id, business_name, owner_name, phone, cnic, address, latitude, longitude, credit_limit, outstanding_balance, credit_hold, approval_status, created_at',
      },
      districts: {
        table: 'pakistan_districts',
        dateCol: 'created_at',
        cols: 'id, name, code, province, assigned_zsm_id, assigned_asm_id, is_active, created_at',
      },
      beats: {
        table: 'territory_beats',
        dateCol: 'created_at',
        cols: 'id, district_id, name, code, day_of_week, assigned_rep_id, is_active, created_at',
      },
      products: {
        table: 'products',
        dateCol: 'created_at',
        cols: 'id, sku, name, category, viscosity_grade, pack_size_liters, units_per_carton, trade_price, retail_price, stock_quantity, is_active, created_at',
      },
      orders: {
        table: 'orders',
        dateCol: 'created_at',
        cols: 'id, order_number, client_id, sales_rep_id, beat_id, total_cartons, total_liters, gross_amount, discount_amount, net_amount, payment_terms, credit_hold_flag, status, external_oms_reference, created_at',
      },
      visits: {
        table: 'visits',
        dateCol: 'check_in_time',
        cols: 'id, attendance_id, client_id, sales_rep_id, check_in_time, check_out_time, check_in_latitude, check_in_longitude, geofence_distance_meters, is_mock_gps, outcome, recovery_slip_number, recovery_amount, order_id, created_at',
      },
      attendance: {
        table: 'daily_attendance',
        dateCol: 'date',
        cols: 'id, user_id, date, shift_start, shift_end, start_latitude, start_longitude, start_odometer_reading, end_odometer_reading, total_hours, status, created_at',
      },
      breadcrumbs: {
        table: 'location_breadcrumbs',
        dateCol: 'recorded_at',
        cols: 'id, user_id, attendance_id, latitude, longitude, accuracy_meters, speed, bearing, battery_level, is_mock_gps, recorded_at',
      },
      'tada-claims': {
        table: 'tada_claims',
        dateCol: 'claim_date',
        cols: 'id, attendance_id, user_id, claim_date, start_odometer, end_odometer, odometer_delta_km, gps_distance_km, variance_percentage, fuel_rate_per_km, fuel_claim_amount, daily_allowance_amount, toll_parking_amount, total_claim_amount, status, created_at',
      },
      mechanics: {
        table: 'mechanics',
        dateCol: 'created_at',
        cols: 'id, client_id, name, phone, cnic, specialty, wallet_provider, wallet_account_number, qr_code_id, points_balance, total_scans, is_verified, created_at',
      },
      kpis: {
        table: 'targets_kpis',
        dateCol: 'period_start',
        cols: 'id, user_id, period_type, period_start, period_end, target_volume_liters, achieved_volume_liters, target_revenue, achieved_revenue, target_visits, achieved_visits, calculated_kpi_score',
      },
      ledger: {
        table: 'client_ledger_entries',
        dateCol: 'created_at',
        cols: 'id, client_id, entry_type, amount, balance_after, reference_type, reference_id, description, recorded_by, created_at',
      },
    };

    const config = tableMap[entity.toLowerCase()];
    if (!config) {
      throw new Error(`Unsupported export entity: ${entity}. Supported: ${Object.keys(tableMap).join(', ')}`);
    }

    let sql = `SELECT ${config.cols} FROM ${config.table} WHERE 1=1`;
    const params: any[] = [];
    let pIdx = 1;

    if (filters.from) {
      sql += ` AND ${config.dateCol} >= $${pIdx++}`;
      params.push(filters.from);
    }
    if (filters.to) {
      sql += ` AND ${config.dateCol} <= $${pIdx++}`;
      params.push(filters.to);
    }

    sql += ` ORDER BY ${config.dateCol} DESC`;

    const limit = Math.min(filters.limit || 5000, 10000);
    sql += ` LIMIT ${limit}`;

    const result = await query(sql, params);
    const rows = result.rows;

    const dateStamp = new Date().toISOString().slice(0, 10);
    const filename = `export_${entity}_${dateStamp}.${format}`;

    if (format === 'csv') {
      const csv = this.convertToCsv(rows);
      return {
        contentType: 'text/csv',
        data: csv,
        filename,
      };
    } else {
      return {
        contentType: 'application/json',
        data: JSON.stringify(rows, null, 2),
        filename,
      };
    }
  }

  private static convertToCsv(rows: any[]): string {
    if (rows.length === 0) return '';
    const headers = Object.keys(rows[0]);
    const escape = (val: any) => {
      if (val === null || val === undefined) return '';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    };

    const headerLine = headers.map(escape).join(',');
    const dataLines = rows.map((row) => headers.map((h) => escape(row[h])).join(','));

    return [headerLine, ...dataLines].join('\r\n');
  }
}
