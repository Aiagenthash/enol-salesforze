import { query } from '../config/database';
import { AuthenticatedUser, DeltaSyncResponse } from '../types';

export class SyncService {
  public static async getDelta(since: string | null, user: AuthenticatedUser): Promise<DeltaSyncResponse> {
    const syncTimestamp = new Date().toISOString();
    const hasSince = !!since && !isNaN(Date.parse(since));

    // Base query filter
    const timeFilter = hasSince ? `AND updated_at > $1` : '';
    const params = hasSince ? [since] : [];

    // 1. Users
    let usersQuery = `SELECT id, email, full_name, phone, role, manager_id, hierarchy_path, vehicle_type, vehicle_number, fuel_rate_per_km, daily_allowance, is_active, created_at, updated_at FROM users WHERE is_active = TRUE ${timeFilter}`;
    const usersRes = await query(usersQuery, params);

    // 2. Districts
    let districtsQuery = `SELECT id, name, code, province, polygon_geojson, assigned_zsm_id, assigned_asm_id, is_active, created_at, updated_at FROM pakistan_districts WHERE is_active = TRUE ${timeFilter}`;
    const districtsRes = await query(districtsQuery, params);

    // 3. Beats
    let beatsQuery = `SELECT id, district_id, name, code, day_of_week, assigned_rep_id, is_active, created_at, updated_at FROM territory_beats WHERE is_active = TRUE ${timeFilter}`;
    const beatsRes = await query(beatsQuery, params);

    // 4. Clients
    let clientsQuery = `SELECT id, beat_id, district_id, business_name, owner_name, phone, cnic, address, latitude, longitude, credit_limit, outstanding_balance, credit_hold, approval_status, created_by, created_at, updated_at FROM clients WHERE 1=1 ${timeFilter}`;
    const clientsRes = await query(clientsQuery, params);

    // 5. Products
    let productsQuery = `SELECT id, sku, name, description, category, viscosity_grade, pack_size_liters, units_per_carton, trade_price, retail_price, active_schemes, stock_quantity, is_active, created_at, updated_at FROM products WHERE is_active = TRUE ${timeFilter}`;
    const productsRes = await query(productsQuery, params);

    // 6. Orders
    let ordersQuery = `SELECT id, order_number, client_id, sales_rep_id, beat_id, total_cartons, total_liters, gross_amount, discount_amount, net_amount, payment_terms, credit_hold_flag, status, external_oms_reference, idempotency_key, notes, created_at, updated_at FROM orders WHERE 1=1 ${timeFilter}`;
    const ordersRes = await query(ordersQuery, params);

    // Fetch line items for any updated orders
    const ordersWithItems = await Promise.all(
      ordersRes.rows.map(async (order) => {
        const itemsRes = await query(
          `SELECT id, order_id, product_id, cartons, units, pack_size_liters, total_liters, unit_price, line_total, scheme_discount, net_line_total, created_at, updated_at
           FROM order_items WHERE order_id = $1`,
          [order.id]
        );
        return {
          ...order,
          items: itemsRes.rows,
        };
      })
    );

    // 7. Mechanics
    let mechanicsQuery = `SELECT id, client_id, name, phone, cnic, specialty, wallet_provider, wallet_account_number, qr_code_id, points_balance, total_scans, is_verified, registered_by, created_at, updated_at FROM mechanics WHERE 1=1 ${timeFilter}`;
    const mechanicsRes = await query(mechanicsQuery, params);

    // 8. KPIs
    let kpisQuery = `SELECT id, user_id, period_type, period_start, period_end, target_volume_liters, achieved_volume_liters, target_revenue, achieved_revenue, target_visits, achieved_visits, target_new_clients, achieved_new_clients, calculated_kpi_score, created_at, updated_at FROM targets_kpis WHERE 1=1 ${timeFilter}`;
    const kpisRes = await query(kpisQuery, params);

    // 9. Competitions
    let competitionsQuery = `SELECT id, title, description, start_date, end_date, target_metric, reward_details, is_active, created_at, updated_at FROM competitions WHERE is_active = TRUE ${timeFilter}`;
    const competitionsRes = await query(competitionsQuery, params);

    return {
      sync_timestamp: syncTimestamp,
      since: since || null,
      counts: {
        users: usersRes.rowCount || 0,
        districts: districtsRes.rowCount || 0,
        beats: beatsRes.rowCount || 0,
        clients: clientsRes.rowCount || 0,
        products: productsRes.rowCount || 0,
        orders: ordersRes.rowCount || 0,
        mechanics: mechanicsRes.rowCount || 0,
        kpis: kpisRes.rowCount || 0,
        competitions: competitionsRes.rowCount || 0,
      },
      delta: {
        users: usersRes.rows,
        districts: districtsRes.rows,
        beats: beatsRes.rows,
        clients: clientsRes.rows,
        products: productsRes.rows,
        orders: ordersWithItems,
        mechanics: mechanicsRes.rows,
        kpis: kpisRes.rows,
        competitions: competitionsRes.rows,
      },
    };
  }
}
