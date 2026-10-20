-- ============================================================================
-- ENOL SALESFORZE - ENTERPRISE FIELD FORCE AUTOMATION & TERRITORY MANAGEMENT
-- Migration 002: PostGIS Triggers, Hierarchy Materialized Paths & Auto-Calculations
-- Target: PostgreSQL 16 + PostGIS
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. POSTGIS SPATIAL GIST INDEXES
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_districts_polygon_geom 
    ON pakistan_districts USING GIST (polygon_geom);

CREATE INDEX IF NOT EXISTS idx_clients_location_gist 
    ON clients USING GIST (location);

CREATE INDEX IF NOT EXISTS idx_breadcrumbs_geom_gist 
    ON location_breadcrumbs USING GIST (geom);

-- ----------------------------------------------------------------------------
-- 2. AUTO-UPDATING `updated_at` TRIGGER FUNCTION & TRIGGERS
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables with updated_at column
DO $$
DECLARE
    tbl text;
BEGIN
    FOR tbl IN 
        SELECT table_name 
        FROM information_schema.columns 
        WHERE column_name = 'updated_at' 
          AND table_schema = 'public'
    LOOP
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_set_updated_at_%1$I ON %1$I;
            CREATE TRIGGER trg_set_updated_at_%1$I
            BEFORE UPDATE ON %1$I
            FOR EACH ROW
            EXECUTE FUNCTION trigger_set_updated_at();
        ', tbl);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ----------------------------------------------------------------------------
-- 3. 5-TIER ORGANIZATIONAL HIERARCHY MATERIALIZED PATH TRIGGER
-- Computes and maintains `hierarchy_path` (e.g. '/<director_id>/<zsm_id>/<asm_id>/<rep_id>/')
-- Automatically cascades path changes to all subordinate employees down the tree!
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trigger_sync_user_hierarchy_path()
RETURNS TRIGGER AS $$
DECLARE
    parent_path VARCHAR(500);
    old_path VARCHAR(500);
    new_path VARCHAR(500);
BEGIN
    -- Prevent self-reference
    IF NEW.manager_id IS NOT NULL AND NEW.manager_id = NEW.id THEN
        RAISE EXCEPTION 'A user cannot be their own manager (User ID: %)', NEW.id;
    END IF;

    -- Determine new hierarchy path
    IF NEW.manager_id IS NULL THEN
        NEW.hierarchy_path = '/' || NEW.id::text || '/';
    ELSE
        SELECT hierarchy_path INTO parent_path 
        FROM users 
        WHERE id = NEW.manager_id;

        IF parent_path IS NULL THEN
            parent_path = '/' || NEW.manager_id::text || '/';
        END IF;

        -- Prevent circular reference
        IF parent_path LIKE ('%/' || NEW.id::text || '/%') THEN
            RAISE EXCEPTION 'Circular hierarchy detected: User % cannot report to subordinate %', NEW.id, NEW.manager_id;
        END IF;

        NEW.hierarchy_path = parent_path || NEW.id::text || '/';
    END IF;

    -- If this is an UPDATE and the path changed, update all descendants recursively
    IF TG_OP = 'UPDATE' AND (OLD.hierarchy_path IS DISTINCT FROM NEW.hierarchy_path) THEN
        old_path = OLD.hierarchy_path;
        new_path = NEW.hierarchy_path;

        -- Defer recursive update after this row finishes
        EXECUTE format('
            UPDATE users 
            SET hierarchy_path = %L || substring(hierarchy_path from length(%L) + 1)
            WHERE hierarchy_path LIKE %L AND id <> %L;
        ', new_path, old_path, old_path || '%', NEW.id);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_user_hierarchy_path ON users;
CREATE TRIGGER trg_user_hierarchy_path
BEFORE INSERT OR UPDATE OF manager_id ON users
FOR EACH ROW
EXECUTE FUNCTION trigger_sync_user_hierarchy_path();

-- ----------------------------------------------------------------------------
-- 4. POSTGIS GEOMETRY SYNCHRONIZATION TRIGGERS
-- Synchronizes Point geometry on Clients and Location Breadcrumbs
-- ----------------------------------------------------------------------------

-- Clients Geometry Trigger
CREATE OR REPLACE FUNCTION trigger_sync_client_geometry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_client_geometry ON clients;
CREATE TRIGGER trg_client_geometry
BEFORE INSERT OR UPDATE OF latitude, longitude ON clients
FOR EACH ROW
EXECUTE FUNCTION trigger_sync_client_geometry();

-- Location Breadcrumbs Geometry Trigger
CREATE OR REPLACE FUNCTION trigger_sync_breadcrumb_geometry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.geom = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_breadcrumb_geometry ON location_breadcrumbs;
CREATE TRIGGER trg_breadcrumb_geometry
BEFORE INSERT OR UPDATE OF latitude, longitude ON location_breadcrumbs
FOR EACH ROW
EXECUTE FUNCTION trigger_sync_breadcrumb_geometry();

-- Districts GeoJSON to Geometry Trigger
CREATE OR REPLACE FUNCTION trigger_sync_district_geometry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.polygon_geojson IS NOT NULL AND (NEW.polygon_geom IS NULL OR TG_OP = 'UPDATE') THEN
        BEGIN
            NEW.polygon_geom = ST_SetSRID(ST_Multi(ST_GeomFromGeoJSON(NEW.polygon_geojson::text)), 4326);
        EXCEPTION WHEN OTHERS THEN
            -- Fallback or ignore invalid geojson in trigger, keep existing geom
            NULL;
        END;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_district_geometry ON pakistan_districts;
CREATE TRIGGER trg_district_geometry
BEFORE INSERT OR UPDATE OF polygon_geojson ON pakistan_districts
FOR EACH ROW
EXECUTE FUNCTION trigger_sync_district_geometry();

-- ----------------------------------------------------------------------------
-- 5. ORDER & ORDER ITEMS CALCULATIONS & CREDIT HOLD LOGIC
-- Automatically aggregates total cartons, liters, gross amount, schemes, net amount
-- and evaluates credit limit vs outstanding balance
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trigger_recalculate_order_totals()
RETURNS TRIGGER AS $$
DECLARE
    target_order_id UUID;
    v_total_cartons NUMERIC(8, 2);
    v_total_liters NUMERIC(10, 2);
    v_gross_amount NUMERIC(12, 2);
    v_discount_amount NUMERIC(12, 2);
    v_net_amount NUMERIC(12, 2);
    v_client_id UUID;
    v_credit_limit NUMERIC(12, 2);
    v_outstanding_balance NUMERIC(12, 2);
    v_credit_hold BOOLEAN;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_order_id = OLD.order_id;
    ELSE
        target_order_id = NEW.order_id;
    END IF;

    -- Aggregate line items
    SELECT 
        COALESCE(SUM(cartons), 0),
        COALESCE(SUM(total_liters), 0),
        COALESCE(SUM(line_total), 0),
        COALESCE(SUM(scheme_discount), 0),
        COALESCE(SUM(net_line_total), 0)
    INTO 
        v_total_cartons,
        v_total_liters,
        v_gross_amount,
        v_discount_amount,
        v_net_amount
    FROM order_items
    WHERE order_id = target_order_id;

    -- Fetch client credit settings
    SELECT o.client_id, c.credit_limit, c.outstanding_balance, c.credit_hold
    INTO v_client_id, v_credit_limit, v_outstanding_balance, v_credit_hold
    FROM orders o
    JOIN clients c ON c.id = o.client_id
    WHERE o.id = target_order_id;

    -- If client is manually on credit hold or order puts them over credit limit
    IF v_credit_hold OR ((v_outstanding_balance + v_net_amount) > v_credit_limit) THEN
        UPDATE orders
        SET total_cartons = v_total_cartons,
            total_liters = v_total_liters,
            gross_amount = v_gross_amount,
            discount_amount = v_discount_amount,
            net_amount = v_net_amount,
            credit_hold_flag = TRUE
        WHERE id = target_order_id;
    ELSE
        UPDATE orders
        SET total_cartons = v_total_cartons,
            total_liters = v_total_liters,
            gross_amount = v_gross_amount,
            discount_amount = v_discount_amount,
            net_amount = v_net_amount,
            credit_hold_flag = FALSE
        WHERE id = target_order_id;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_order_items_recalc ON order_items;
CREATE TRIGGER trg_order_items_recalc
AFTER INSERT OR UPDATE OR DELETE ON order_items
FOR EACH ROW
EXECUTE FUNCTION trigger_recalculate_order_totals();

-- ----------------------------------------------------------------------------
-- 6. TADA CLAIMS AUTO-CALCULATOR & VARIANCE AUDIT TRIGGER
-- Computes odometer delta, compares with GPS distance, computes variance %,
-- sets flag if variance > 15% (Anti-Fraud / Variance Audit)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION trigger_calculate_tada_claim()
RETURNS TRIGGER AS $$
DECLARE
    v_user_fuel_rate NUMERIC(10, 2);
    v_user_daily_allowance NUMERIC(10, 2);
BEGIN
    -- Odometer delta
    NEW.odometer_delta_km = GREATEST(0.00, NEW.end_odometer - NEW.start_odometer);

    -- Fetch user rate if not provided or 0
    IF NEW.fuel_rate_per_km IS NULL OR NEW.fuel_rate_per_km = 0 THEN
        SELECT fuel_rate_per_km, daily_allowance 
        INTO v_user_fuel_rate, v_user_daily_allowance
        FROM users 
        WHERE id = NEW.user_id;

        NEW.fuel_rate_per_km = COALESCE(v_user_fuel_rate, 7.50);
        NEW.daily_allowance_amount = COALESCE(v_user_daily_allowance, 500.00);
    END IF;

    -- Calculate variance percentage against GPS distance
    IF NEW.gps_distance_km > 0 THEN
        NEW.variance_percentage = ROUND(
            ((NEW.odometer_delta_km - NEW.gps_distance_km) / NEW.gps_distance_km) * 100.0,
            2
        );
    ELSE
        NEW.variance_percentage = 0.00;
    END IF;

    -- Calculate financial amounts
    NEW.fuel_claim_amount = ROUND(NEW.odometer_delta_km * NEW.fuel_rate_per_km, 2);
    NEW.total_claim_amount = ROUND(NEW.fuel_claim_amount + NEW.daily_allowance_amount + COALESCE(NEW.toll_parking_amount, 0.00), 2);

    -- Automatic fraud/variance flag if variance > 15%
    IF NEW.variance_percentage > 15.00 AND NEW.status = 'SUBMITTED' THEN
        NEW.status = 'VERIFIED_FLAGGED';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_tada_claim_calc ON tada_claims;
CREATE TRIGGER trg_tada_claim_calc
BEFORE INSERT OR UPDATE OF start_odometer, end_odometer, gps_distance_km, fuel_rate_per_km, toll_parking_amount ON tada_claims
FOR EACH ROW
EXECUTE FUNCTION trigger_calculate_tada_claim();
