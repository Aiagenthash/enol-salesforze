-- ============================================================================
-- ENOL SALESFORZE - ENTERPRISE FIELD FORCE AUTOMATION & TERRITORY MANAGEMENT
-- Migration 001: Initial Core Schema & Tables
-- Target: PostgreSQL 16 + PostGIS
-- ============================================================================

-- Ensure required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- ----------------------------------------------------------------------------
-- 1. USERS (5-Tier Organizational Hierarchy: DIRECTOR -> ZSM -> ASM -> SALES_REP -> ADMIN)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('DIRECTOR', 'ZSM', 'ASM', 'SALES_REP', 'ADMIN')),
    manager_id UUID REFERENCES users(id) ON DELETE SET NULL,
    hierarchy_path VARCHAR(500) NOT NULL DEFAULT '/',
    bound_device_id VARCHAR(100),
    vehicle_type VARCHAR(50) NOT NULL DEFAULT 'MOTORCYCLE' CHECK (vehicle_type IN ('MOTORCYCLE', 'CAR', 'VAN', 'OTHER')),
    vehicle_number VARCHAR(50),
    fuel_rate_per_km NUMERIC(10, 2) NOT NULL DEFAULT 7.50,
    daily_allowance NUMERIC(10, 2) NOT NULL DEFAULT 500.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_manager_id ON users (manager_id);
CREATE INDEX IF NOT EXISTS idx_users_hierarchy_path ON users (hierarchy_path);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- ----------------------------------------------------------------------------
-- 2. PAKISTAN DISTRICTS (Territory Boundaries)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pakistan_districts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    province VARCHAR(50) NOT NULL CHECK (province IN ('Punjab', 'Sindh', 'Khyber Pakhtunkhwa', 'Balochistan', 'Islamabad Capital Territory', 'Gilgit-Baltistan', 'Azad Jammu & Kashmir')),
    polygon_geom GEOMETRY(MultiPolygon, 4326),
    polygon_geojson JSONB,
    assigned_zsm_id UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_asm_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_districts_province ON pakistan_districts (province);
CREATE INDEX IF NOT EXISTS idx_districts_assigned_asm ON pakistan_districts (assigned_asm_id);
CREATE INDEX IF NOT EXISTS idx_districts_assigned_zsm ON pakistan_districts (assigned_zsm_id);

-- ----------------------------------------------------------------------------
-- 3. TERRITORY BEATS (Route / Day Plan for Field Sales Reps)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS territory_beats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    district_id UUID NOT NULL REFERENCES pakistan_districts(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    day_of_week VARCHAR(20) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    assigned_rep_id UUID REFERENCES users(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_beats_district ON territory_beats (district_id);
CREATE INDEX IF NOT EXISTS idx_beats_rep ON territory_beats (assigned_rep_id);
CREATE INDEX IF NOT EXISTS idx_beats_day ON territory_beats (day_of_week);

-- ----------------------------------------------------------------------------
-- 4. CLIENTS (Retailers, Distributors, Workshops, Spare Parts Shops)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    beat_id UUID REFERENCES territory_beats(id) ON DELETE SET NULL,
    district_id UUID NOT NULL REFERENCES pakistan_districts(id) ON DELETE RESTRICT,
    business_name VARCHAR(200) NOT NULL,
    owner_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    cnic VARCHAR(30),
    address TEXT NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    location GEOMETRY(Point, 4326),
    credit_limit NUMERIC(12, 2) NOT NULL DEFAULT 100000.00,
    outstanding_balance NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    credit_hold BOOLEAN NOT NULL DEFAULT FALSE,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED')),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clients_district ON clients (district_id);
CREATE INDEX IF NOT EXISTS idx_clients_beat ON clients (beat_id);
CREATE INDEX IF NOT EXISTS idx_clients_credit_hold ON clients (credit_hold);
CREATE INDEX IF NOT EXISTS idx_clients_approval_status ON clients (approval_status);

-- ----------------------------------------------------------------------------
-- 5. CLIENT LEDGER ENTRIES
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS client_ledger_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN ('INVOICE', 'PAYMENT', 'CREDIT_NOTE', 'DEBIT_NOTE', 'ADJUSTMENT')),
    amount NUMERIC(12, 2) NOT NULL,
    balance_after NUMERIC(12, 2) NOT NULL,
    reference_type VARCHAR(50) NOT NULL DEFAULT 'ORDER' CHECK (reference_type IN ('ORDER', 'RECOVERY_SLIP', 'BANK_TRANSFER', 'CHEQUE', 'MANUAL')),
    reference_id VARCHAR(100),
    description TEXT,
    recorded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ledger_client ON client_ledger_entries (client_id);
CREATE INDEX IF NOT EXISTS idx_ledger_created_at ON client_ledger_entries (created_at);

-- ----------------------------------------------------------------------------
-- 6. MECHANICS (Influencer Loyalty Network)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS mechanics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID REFERENCES clients(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(50) NOT NULL UNIQUE,
    cnic VARCHAR(30),
    specialty VARCHAR(50) NOT NULL DEFAULT '4-STROKE_MOTORCYCLE' CHECK (specialty IN ('4-STROKE_MOTORCYCLE', 'HEAVY_DIESEL', 'PASSENGER_CAR', 'TRACTOR_AGRI', 'GENERAL_AUTO')),
    wallet_provider VARCHAR(50) NOT NULL DEFAULT 'JAZZCASH' CHECK (wallet_provider IN ('EASYPAISA', 'JAZZCASH', 'NAYAPAY', 'SADAPAY', 'BANK_ACCOUNT')),
    wallet_account_number VARCHAR(50) NOT NULL,
    qr_code_id VARCHAR(100) NOT NULL UNIQUE,
    points_balance INTEGER NOT NULL DEFAULT 0,
    total_scans INTEGER NOT NULL DEFAULT 0,
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    registered_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mechanics_phone ON mechanics (phone);
CREATE INDEX IF NOT EXISTS idx_mechanics_qr_code ON mechanics (qr_code_id);
CREATE INDEX IF NOT EXISTS idx_mechanics_client_id ON mechanics (client_id);

-- ----------------------------------------------------------------------------
-- 7. PRODUCTS (Automotive Lubricant Catalog)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL CHECK (category IN ('PASSENGER_CAR_MOTOR_OIL', 'HEAVY_DUTY_DIESEL', 'MOTORCYCLE_4T', 'GEAR_OIL', 'TRANSMISSION_FLUID', 'COOLANT', 'GREASE', 'SPECIALTY')),
    viscosity_grade VARCHAR(50) NOT NULL,
    pack_size_liters NUMERIC(6, 2) NOT NULL,
    units_per_carton INTEGER NOT NULL DEFAULT 1,
    trade_price NUMERIC(10, 2) NOT NULL,
    retail_price NUMERIC(10, 2) NOT NULL,
    active_schemes JSONB NOT NULL DEFAULT '[]'::jsonb,
    stock_quantity INTEGER NOT NULL DEFAULT 1000,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_sku ON products (sku);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);
CREATE INDEX IF NOT EXISTS idx_products_viscosity ON products (viscosity_grade);

-- ----------------------------------------------------------------------------
-- 8. ORDERS & 9. ORDER ITEMS
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number VARCHAR(50) NOT NULL UNIQUE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    sales_rep_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    beat_id UUID REFERENCES territory_beats(id) ON DELETE SET NULL,
    total_cartons NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    total_liters NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    net_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    payment_terms VARCHAR(50) NOT NULL DEFAULT 'CREDIT_15_DAYS' CHECK (payment_terms IN ('CASH_ON_DELIVERY', 'CREDIT_7_DAYS', 'CREDIT_15_DAYS', 'CREDIT_30_DAYS', 'ADVANCE')),
    credit_hold_flag BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'DISPATCHED', 'DELIVERED', 'CANCELLED', 'REJECTED_CREDIT_HOLD')),
    external_oms_reference VARCHAR(100),
    idempotency_key VARCHAR(100) UNIQUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_client ON orders (client_id);
CREATE INDEX IF NOT EXISTS idx_orders_rep ON orders (sales_rep_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_idempotency ON orders (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_orders_external_oms ON orders (external_oms_reference);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    cartons NUMERIC(8, 2) NOT NULL DEFAULT 0.00,
    units INTEGER NOT NULL DEFAULT 0,
    pack_size_liters NUMERIC(6, 2) NOT NULL,
    total_liters NUMERIC(10, 2) NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    scheme_discount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    net_line_total NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items (product_id);

-- ----------------------------------------------------------------------------
-- 10. DAILY ATTENDANCE (Shift Start/End, Selfie, Odometer)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS daily_attendance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    shift_start TIMESTAMPTZ NOT NULL,
    shift_end TIMESTAMPTZ,
    start_latitude NUMERIC(10, 7) NOT NULL,
    start_longitude NUMERIC(10, 7) NOT NULL,
    start_selfie_url TEXT NOT NULL,
    start_odometer_reading NUMERIC(10, 2) NOT NULL,
    start_odometer_photo_url TEXT NOT NULL,
    end_latitude NUMERIC(10, 7),
    end_longitude NUMERIC(10, 7),
    end_selfie_url TEXT,
    end_odometer_reading NUMERIC(10, 2),
    end_odometer_photo_url TEXT,
    total_hours NUMERIC(4, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'AUTO_CLOSED', 'REJECTED')),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_attendance_date UNIQUE(user_id, date)
);

CREATE INDEX IF NOT EXISTS idx_attendance_user ON daily_attendance (user_id);
CREATE INDEX IF NOT EXISTS idx_attendance_date ON daily_attendance (date);

-- ----------------------------------------------------------------------------
-- 11. VISITS (Field Shop Visits, Geofencing, Recovery)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS visits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    attendance_id UUID REFERENCES daily_attendance(id) ON DELETE SET NULL,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    sales_rep_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    scheduled_at TIMESTAMPTZ,
    check_in_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    check_out_time TIMESTAMPTZ,
    check_in_latitude NUMERIC(10, 7) NOT NULL,
    check_in_longitude NUMERIC(10, 7) NOT NULL,
    check_out_latitude NUMERIC(10, 7),
    check_out_longitude NUMERIC(10, 7),
    geofence_distance_meters NUMERIC(10, 2),
    is_mock_gps BOOLEAN NOT NULL DEFAULT FALSE,
    outcome VARCHAR(30) NOT NULL CHECK (outcome IN ('ORDER_PLACED', 'PAYMENT_COLLECTED', 'STOCK_AUDIT_ONLY', 'CLIENT_CLOSED', 'NO_ORDER', 'MERCHANDISING_ONLY')),
    notes TEXT,
    recovery_slip_number VARCHAR(100),
    recovery_amount NUMERIC(12, 2) DEFAULT 0.00,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visits_rep ON visits (sales_rep_id);
CREATE INDEX IF NOT EXISTS idx_visits_client ON visits (client_id);
CREATE INDEX IF NOT EXISTS idx_visits_attendance ON visits (attendance_id);
CREATE INDEX IF NOT EXISTS idx_visits_check_in ON visits (check_in_time);

-- ----------------------------------------------------------------------------
-- 12. LOCATION BREADCRUMBS (GPS Realtime Tracking)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS location_breadcrumbs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attendance_id UUID REFERENCES daily_attendance(id) ON DELETE SET NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    geom GEOMETRY(Point, 4326),
    accuracy_meters NUMERIC(6, 2) NOT NULL DEFAULT 5.0,
    speed NUMERIC(6, 2),
    bearing NUMERIC(6, 2),
    battery_level NUMERIC(4, 1),
    is_mock_gps BOOLEAN NOT NULL DEFAULT FALSE,
    recorded_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_breadcrumbs_user ON location_breadcrumbs (user_id);
CREATE INDEX IF NOT EXISTS idx_breadcrumbs_attendance ON location_breadcrumbs (attendance_id);
CREATE INDEX IF NOT EXISTS idx_breadcrumbs_recorded_at ON location_breadcrumbs (recorded_at);

-- ----------------------------------------------------------------------------
-- 13. TADA CLAIMS (Travel & Daily Allowance)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tada_claims (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    attendance_id UUID NOT NULL REFERENCES daily_attendance(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    claim_date DATE NOT NULL,
    start_odometer NUMERIC(10, 2) NOT NULL,
    end_odometer NUMERIC(10, 2) NOT NULL,
    odometer_delta_km NUMERIC(10, 2) NOT NULL,
    gps_distance_km NUMERIC(10, 2) NOT NULL,
    variance_percentage NUMERIC(6, 2) NOT NULL,
    fuel_rate_per_km NUMERIC(10, 2) NOT NULL,
    fuel_claim_amount NUMERIC(10, 2) NOT NULL,
    daily_allowance_amount NUMERIC(10, 2) NOT NULL,
    toll_parking_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    total_claim_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'VERIFIED_FLAGGED', 'APPROVED', 'REJECTED', 'DISBURSED')),
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tada_user ON tada_claims (user_id);
CREATE INDEX IF NOT EXISTS idx_tada_attendance ON tada_claims (attendance_id);
CREATE INDEX IF NOT EXISTS idx_tada_claim_date ON tada_claims (claim_date);
CREATE INDEX IF NOT EXISTS idx_tada_status ON tada_claims (status);

-- ----------------------------------------------------------------------------
-- 14. TARGETS & KPIS
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS targets_kpis (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_type VARCHAR(20) NOT NULL CHECK (period_type IN ('MONTHLY', 'QUARTERLY', 'ANNUAL')),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    target_volume_liters NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    achieved_volume_liters NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    target_revenue NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    achieved_revenue NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    target_visits INTEGER NOT NULL DEFAULT 0,
    achieved_visits INTEGER NOT NULL DEFAULT 0,
    target_new_clients INTEGER NOT NULL DEFAULT 0,
    achieved_new_clients INTEGER NOT NULL DEFAULT 0,
    calculated_kpi_score NUMERIC(5, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_target_period UNIQUE (user_id, period_type, period_start)
);

CREATE INDEX IF NOT EXISTS idx_targets_user ON targets_kpis (user_id);
CREATE INDEX IF NOT EXISTS idx_targets_period ON targets_kpis (period_start, period_end);

-- ----------------------------------------------------------------------------
-- 15. COMPETITIONS & 16. COMPETITION SCORES
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS competitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    target_metric VARCHAR(50) NOT NULL CHECK (target_metric IN ('VOLUME_LITERS', 'REVENUE_PKR', 'NEW_CLIENTS', 'SYNTHETIC_RATIO', 'COLLECTION_RATE')),
    reward_details JSONB NOT NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_competitions_active ON competitions (is_active);

CREATE TABLE IF NOT EXISTS competition_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    competition_id UUID NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_metric_value NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    current_rank INTEGER NOT NULL DEFAULT 1,
    points INTEGER NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_comp_user UNIQUE(competition_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_comp_scores_competition ON competition_scores (competition_id);
CREATE INDEX IF NOT EXISTS idx_comp_scores_user ON competition_scores (user_id);
CREATE INDEX IF NOT EXISTS idx_comp_scores_rank ON competition_scores (current_rank);

-- ----------------------------------------------------------------------------
-- 17. API KEYS
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key_name VARCHAR(100) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    scopes TEXT[] NOT NULL DEFAULT ARRAY['read', 'write'],
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON api_keys (key_hash);
CREATE INDEX IF NOT EXISTS idx_api_keys_user ON api_keys (user_id);

-- ----------------------------------------------------------------------------
-- 18. WEBHOOK SUBSCRIPTIONS & 19. WEBHOOK DELIVERY LOGS
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS webhook_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(150) NOT NULL,
    target_url TEXT NOT NULL,
    secret VARCHAR(255) NOT NULL,
    events TEXT[] NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webhook_subs_active ON webhook_subscriptions (is_active);

CREATE TABLE IF NOT EXISTS webhook_delivery_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id UUID NOT NULL REFERENCES webhook_subscriptions(id) ON DELETE CASCADE,
    event_name VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    error_message TEXT,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webhook_logs_sub ON webhook_delivery_logs (subscription_id);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_event ON webhook_delivery_logs (event_name);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_created_at ON webhook_delivery_logs (created_at);
