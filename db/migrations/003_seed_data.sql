-- ============================================================================
-- ENOL SALESFORZE - ENTERPRISE FIELD FORCE AUTOMATION & TERRITORY MANAGEMENT
-- Migration 003: Comprehensive Seed Data for Automotive Lubricant Organization
-- Target: PostgreSQL 16 + PostGIS
-- ============================================================================

-- Fixed UUIDs for predictable hierarchy linking
-- Director:    11111111-1111-1111-1111-111111111111
-- ZSM:         22222222-2222-2222-2222-222222222222
-- ASM:         33333333-3333-3333-3333-333333333333
-- Sales Rep:   44444444-4444-4444-4444-444444444444
-- Admin:       55555555-5555-5555-5555-555555555555

-- Default password hash for all seed users is 'Password123!' (bcrypt $2a$10$srwrBv1tWvmyV02BCC0ywuOKzuAduiPkdShyUwAfq6Hcc1ib9c4fq)
-- ----------------------------------------------------------------------------
-- 1. SEED 5-TIER ORGANIZATIONAL USERS
-- ----------------------------------------------------------------------------
INSERT INTO users (
    id, email, password_hash, full_name, phone, role, manager_id,
    bound_device_id, vehicle_type, vehicle_number, fuel_rate_per_km, daily_allowance, is_active
) VALUES 
-- Tier 1: Director
(
    '11111111-1111-1111-1111-111111111111',
    'director@enollubricants.com',
    '$2a$10$srwrBv1tWvmyV02BCC0ywuOKzuAduiPkdShyUwAfq6Hcc1ib9c4fq',
    'Tariq Mehmood',
    '+923001000001',
    'DIRECTOR',
    NULL,
    'DEV-DIR-01',
    'CAR',
    'ICT-AB-101',
    24.00,
    2500.00,
    TRUE
),
-- Tier 2: Zonal Sales Manager (Central Punjab Zone)
(
    '22222222-2222-2222-2222-222222222222',
    'zsm.punjab@enollubricants.com',
    '$2a$10$srwrBv1tWvmyV02BCC0ywuOKzuAduiPkdShyUwAfq6Hcc1ib9c4fq',
    'Imran Khan Niazi',
    '+923001000002',
    'ZSM',
    '11111111-1111-1111-1111-111111111111',
    'DEV-ZSM-01',
    'CAR',
    'LEA-2022-786',
    22.00,
    1800.00,
    TRUE
),
-- Tier 3: Area Sales Manager (Lahore District)
(
    '33333333-3333-3333-3333-333333333333',
    'asm.lahore@enollubricants.com',
    '$2a$10$srwrBv1tWvmyV02BCC0ywuOKzuAduiPkdShyUwAfq6Hcc1ib9c4fq',
    'Kamran Akram',
    '+923001000003',
    'ASM',
    '22222222-2222-2222-2222-222222222222',
    'DEV-ASM-01',
    'CAR',
    'LEB-2024-432',
    20.00,
    1200.00,
    TRUE
),
-- Tier 4: Field Sales Rep / Order Booker (Lahore Commercial Auto Markets)
(
    '44444444-4444-4444-4444-444444444444',
    'rep.hamza@enollubricants.com',
    '$2a$10$srwrBv1tWvmyV02BCC0ywuOKzuAduiPkdShyUwAfq6Hcc1ib9c4fq',
    'Hamza Farooq',
    '+923001000004',
    'SALES_REP',
    '33333333-3333-3333-3333-333333333333',
    'DEV-REP-HAMZA-99',
    'MOTORCYCLE',
    'LEK-19-9021',
    8.50,
    600.00,
    TRUE
),
-- Tier 5: System Administrator
(
    '55555555-5555-5555-5555-555555555555',
    'admin@enollubricants.com',
    '$2a$10$srwrBv1tWvmyV02BCC0ywuOKzuAduiPkdShyUwAfq6Hcc1ib9c4fq',
    'System Administrator',
    '+923001000005',
    'ADMIN',
    NULL,
    'DEV-ADMIN-01',
    'CAR',
    'ISB-2026-001',
    22.00,
    2000.00,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. SEED PAKISTAN DISTRICTS
-- ----------------------------------------------------------------------------
INSERT INTO pakistan_districts (
    id, name, code, province, polygon_geojson, assigned_zsm_id, assigned_asm_id, is_active
) VALUES
(
    'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Lahore',
    'PK-LHR',
    'Punjab',
    '{"type": "MultiPolygon", "coordinates": [[[[74.25, 31.45], [74.45, 31.45], [74.45, 31.65], [74.25, 31.65], [74.25, 31.45]]]]}',
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    TRUE
),
(
    'a2222222-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Faisalabad',
    'PK-FSD',
    'Punjab',
    '{"type": "MultiPolygon", "coordinates": [[[[73.00, 31.35], [73.20, 31.35], [73.20, 31.55], [73.00, 31.55], [73.00, 31.35]]]]}',
    '22222222-2222-2222-2222-222222222222',
    NULL,
    TRUE
),
(
    'a3333333-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Rawalpindi',
    'PK-RWP',
    'Punjab',
    '{"type": "MultiPolygon", "coordinates": [[[[73.00, 33.55], [73.15, 33.55], [73.15, 33.70], [73.00, 33.70], [73.00, 33.55]]]]}',
    NULL,
    NULL,
    TRUE
),
(
    'a4444444-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Karachi South',
    'PK-KHI-S',
    'Sindh',
    '{"type": "MultiPolygon", "coordinates": [[[[66.98, 24.80], [67.08, 24.80], [67.08, 24.88], [66.98, 24.88], [66.98, 24.80]]]]}',
    NULL,
    NULL,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. SEED TERRITORY BEATS
-- ----------------------------------------------------------------------------
INSERT INTO territory_beats (
    id, district_id, name, code, day_of_week, assigned_rep_id, is_active
) VALUES
(
    'b1111111-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Badami Bagh Auto Market Beat',
    'BEAT-LHR-001',
    'MONDAY',
    '44444444-4444-4444-4444-444444444444',
    TRUE
),
(
    'b2222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Montgomery Road Commercial Beat',
    'BEAT-LHR-002',
    'TUESDAY',
    '44444444-4444-4444-4444-444444444444',
    TRUE
),
(
    'b3333333-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Ferozepur Road Workshop Beat',
    'BEAT-LHR-003',
    'WEDNESDAY',
    '44444444-4444-4444-4444-444444444444',
    TRUE
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. SEED CLIENTS
-- ----------------------------------------------------------------------------
INSERT INTO clients (
    id, beat_id, district_id, business_name, owner_name, phone, cnic,
    address, latitude, longitude, credit_limit, outstanding_balance, credit_hold, approval_status, created_by
) VALUES
(
    'c1111111-cccc-cccc-cccc-cccccccccccc',
    'b1111111-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Bilal Autos & Oil Depot',
    'Muhammad Bilal',
    '+923214567890',
    '35201-1234567-1',
    'Shop 14, Main Auto Market, Badami Bagh, Lahore',
    31.5925000,
    74.3186000,
    350000.00,
    85000.00,
    FALSE,
    'APPROVED',
    '44444444-4444-4444-4444-444444444444'
),
(
    'c2222222-cccc-cccc-cccc-cccccccccccc',
    'b2222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Al-Rehman Spare Parts & Lubricants',
    'Haji Abdul Rehman',
    '+923334567891',
    '35201-7654321-3',
    'Plaza 8, Montgomery Road, Lahore',
    31.5583000,
    74.3262000,
    500000.00,
    490000.00,
    FALSE,
    'APPROVED',
    '44444444-4444-4444-4444-444444444444'
),
(
    'c3333333-cccc-cccc-cccc-cccccccccccc',
    'b3333333-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'a1111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Shaheen 3S Lubricant Service Station',
    'Malik Rashid',
    '+923454567892',
    '35202-9988776-5',
    'Plot 45, Ferozepur Road, Lahore',
    31.4812000,
    74.3415000,
    200000.00,
    15000.00,
    FALSE,
    'APPROVED',
    '44444444-4444-4444-4444-444444444444'
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. SEED CLIENT LEDGER ENTRIES
-- ----------------------------------------------------------------------------
INSERT INTO client_ledger_entries (
    client_id, entry_type, amount, balance_after, reference_type, reference_id, description, recorded_by
) VALUES
(
    'c1111111-cccc-cccc-cccc-cccccccccccc',
    'INVOICE',
    125000.00,
    125000.00,
    'ORDER',
    'ORD-20261010-0089',
    'Initial inventory stocking order',
    '44444444-4444-4444-4444-444444444444'
),
(
    'c1111111-cccc-cccc-cccc-cccccccccccc',
    'PAYMENT',
    40000.00,
    85000.00,
    'RECOVERY_SLIP',
    'REC-20261018-0012',
    'Cash recovery by Sales Rep Hamza',
    '44444444-4444-4444-4444-444444444444'
);

-- ----------------------------------------------------------------------------
-- 6. SEED MECHANICS (Influencer Loyalty Network)
-- ----------------------------------------------------------------------------
INSERT INTO mechanics (
    id, client_id, name, phone, cnic, specialty, wallet_provider,
    wallet_account_number, qr_code_id, points_balance, total_scans, is_verified, registered_by
) VALUES
(
    '66666666-6666-6666-6666-666666666661',
    'c1111111-cccc-cccc-cccc-cccccccccccc',
    'Ustad Asghar Ali',
    '+923005544331',
    '35201-4455667-9',
    '4-STROKE_MOTORCYCLE',
    'JAZZCASH',
    '03005544331',
    'QR-MECH-LHR-001',
    450,
    45,
    TRUE,
    '44444444-4444-4444-4444-444444444444'
),
(
    '66666666-6666-6666-6666-666666666662',
    'c2222222-cccc-cccc-cccc-cccccccccccc',
    'Master Bashir Diesel Works',
    '+923129988772',
    '35201-1122334-5',
    'HEAVY_DIESEL',
    'EASYPAISA',
    '03129988772',
    'QR-MECH-LHR-002',
    1280,
    85,
    TRUE,
    '44444444-4444-4444-4444-444444444444'
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 7. SEED PRODUCT CATALOG (Automotive Lubricants & Fluids)
-- ----------------------------------------------------------------------------
INSERT INTO products (
    id, sku, name, description, category, viscosity_grade,
    pack_size_liters, units_per_carton, trade_price, retail_price,
    active_schemes, stock_quantity, is_active
) VALUES
(
    '77777777-7777-7777-7777-777777777771',
    'ENOL-SYN-5W30-4L',
    'ENOL Apex Fully Synthetic 5W-30 (4L)',
    'Premium synthetic engine oil with advanced additive chemistry for gasoline and modern hybrid engines.',
    'PASSENGER_CAR_MOTOR_OIL',
    '5W-30',
    4.00,
    4,
    14500.00,
    17200.00,
    '[{"type": "BUY_X_GET_Y", "buy_cartons": 5, "free_cartons": 0.25, "description": "Buy 5 Cartons get 1 Free 4L can"}, {"type": "DISCOUNT_PERCENT", "min_cartons": 10, "discount_pct": 5.0}]'::jsonb,
    1500,
    TRUE
),
(
    '77777777-7777-7777-7777-777777777772',
    'ENOL-PCMO-20W50-4L',
    'ENOL Super Force 20W-50 API SL/CF (4L)',
    'High-viscosity multi-grade engine oil designed for high ambient temperatures and heavy-duty urban stop-and-go driving.',
    'PASSENGER_CAR_MOTOR_OIL',
    '20W-50',
    4.00,
    4,
    7200.00,
    8800.00,
    '[{"type": "DISCOUNT_PERCENT", "min_cartons": 10, "discount_pct": 4.0}]'::jsonb,
    3200,
    TRUE
),
(
    '77777777-7777-7777-7777-777777777773',
    'ENOL-4T-20W50-0.7L',
    'ENOL Rider 4T 20W-50 JASO MA2 (0.7L)',
    'Dedicated 4-stroke motorcycle engine oil providing wet clutch protection and smooth gear shift performance.',
    'MOTORCYCLE_4T',
    '20W-50',
    0.70,
    24,
    11520.00,
    13920.00,
    '[{"type": "CASH_REBATE", "per_carton_pkr": 300.00}]'::jsonb,
    4500,
    TRUE
),
(
    '77777777-7777-7777-7777-777777777774',
    'ENOL-HDO-15W40-10L',
    'ENOL Titan Diesel Fleet CI-4 (10L Bucket)',
    'Heavy-duty diesel engine oil for turbocharged commercial trucks, tractors, and generator applications.',
    'HEAVY_DUTY_DIESEL',
    '15W-40',
    10.00,
    1,
    12800.00,
    14900.00,
    '[]'::jsonb,
    800,
    TRUE
),
(
    '77777777-7777-7777-7777-777777777775',
    'ENOL-GEAR-85W140-4L',
    'ENOL TransGear EP 85W-140 GL-5 (4L)',
    'Extreme pressure hypoid gear and differential oil for heavy load automotive transmissions.',
    'GEAR_OIL',
    '85W-140',
    4.00,
    4,
    8100.00,
    9600.00,
    '[]'::jsonb,
    1100,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 8. SEED ORDERS & 9. ORDER ITEMS
-- ----------------------------------------------------------------------------
INSERT INTO orders (
    id, order_number, client_id, sales_rep_id, beat_id,
    total_cartons, total_liters, gross_amount, discount_amount, net_amount,
    payment_terms, credit_hold_flag, status, external_oms_reference, idempotency_key, notes
) VALUES
(
    '88888888-8888-8888-8888-888888888881',
    'ORD-20261020-0001',
    'c1111111-cccc-cccc-cccc-cccccccccccc',
    '44444444-4444-4444-4444-444444444444',
    'b1111111-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    4.00,
    57.60,
    40240.00,
    1200.00,
    39040.00,
    'CREDIT_15_DAYS',
    FALSE,
    'APPROVED',
    'OMS-SAP-98124',
    'IDEMP-CLIENT1-20261020-01',
    'Routine weekly restock order for Badami Bagh'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (
    id, order_id, product_id, cartons, units, pack_size_liters,
    total_liters, unit_price, line_total, scheme_discount, net_line_total
) VALUES
(
    '88888888-8888-8888-8888-8888888888a1',
    '88888888-8888-8888-8888-888888888881',
    '77777777-7777-7777-7777-777777777771',
    2.00,
    8,
    4.00,
    32.00,
    3625.00,
    29000.00,
    600.00,
    28400.00
),
(
    '88888888-8888-8888-8888-8888888888a2',
    '88888888-8888-8888-8888-888888888881',
    '77777777-7777-7777-7777-777777777773',
    2.00,
    48,
    0.70,
    25.60,
    562.00,
    11240.00,
    600.00,
    10640.00
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 10. SEED DAILY ATTENDANCE
-- ----------------------------------------------------------------------------
INSERT INTO daily_attendance (
    id, user_id, date, shift_start, shift_end,
    start_latitude, start_longitude, start_selfie_url, start_odometer_reading, start_odometer_photo_url,
    end_latitude, end_longitude, end_selfie_url, end_odometer_reading, end_odometer_photo_url,
    total_hours, status, notes
) VALUES
(
    'd1111111-dddd-dddd-dddd-dddddddddddd',
    '44444444-4444-4444-4444-444444444444',
    '2026-10-20',
    '2026-10-20 09:05:00+05',
    '2026-10-20 18:15:00+05',
    31.5497000,
    74.3436000,
    'https://storage.enollubricants.com/selfies/rep-hamza-20261020-start.jpg',
    24510.50,
    'https://storage.enollubricants.com/odometer/rep-hamza-20261020-start.jpg',
    31.5498000,
    74.3435000,
    'https://storage.enollubricants.com/selfies/rep-hamza-20261020-end.jpg',
    24558.00,
    'https://storage.enollubricants.com/odometer/rep-hamza-20261020-end.jpg',
    9.17,
    'COMPLETED',
    'Beat completed with 8 retail shop visits and 1 order placed'
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 11. SEED VISITS
-- ----------------------------------------------------------------------------
INSERT INTO visits (
    id, attendance_id, client_id, sales_rep_id, scheduled_at,
    check_in_time, check_out_time, check_in_latitude, check_in_longitude,
    check_out_latitude, check_out_longitude, geofence_distance_meters, is_mock_gps,
    outcome, notes, recovery_slip_number, recovery_amount, order_id
) VALUES
(
    '99999999-9999-9999-9999-999999999991',
    'd1111111-dddd-dddd-dddd-dddddddddddd',
    'c1111111-cccc-cccc-cccc-cccccccccccc',
    '44444444-4444-4444-4444-444444444444',
    '2026-10-20 10:00:00+05',
    '2026-10-20 10:14:22+05',
    '2026-10-20 10:48:15+05',
    31.5925100,
    74.3186200,
    31.5924900,
    74.3185900,
    18.50,
    FALSE,
    'ORDER_PLACED',
    'Client ordered 4 cartons. Promoted Apex 5W30 scheme.',
    NULL,
    0.00,
    '88888888-8888-8888-8888-888888888881'
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 12. SEED LOCATION BREADCRUMBS
-- ----------------------------------------------------------------------------
INSERT INTO location_breadcrumbs (
    user_id, attendance_id, latitude, longitude, accuracy_meters, speed, bearing, battery_level, is_mock_gps, recorded_at
) VALUES
(
    '44444444-4444-4444-4444-444444444444',
    'd1111111-dddd-dddd-dddd-dddddddddddd',
    31.5497000,
    74.3436000,
    4.50,
    0.0,
    0.0,
    98.0,
    FALSE,
    '2026-10-20 09:05:00+05'
),
(
    '44444444-4444-4444-4444-444444444444',
    'd1111111-dddd-dddd-dddd-dddddddddddd',
    31.5710000,
    74.3310000,
    5.00,
    8.5,
    340.0,
    95.0,
    FALSE,
    '2026-10-20 09:35:00+05'
),
(
    '44444444-4444-4444-4444-444444444444',
    'd1111111-dddd-dddd-dddd-dddddddddddd',
    31.5925100,
    74.3186200,
    3.80,
    0.0,
    0.0,
    91.0,
    FALSE,
    '2026-10-20 10:14:22+05'
);

-- ----------------------------------------------------------------------------
-- 13. SEED TADA CLAIMS
-- ----------------------------------------------------------------------------
INSERT INTO tada_claims (
    id, attendance_id, user_id, claim_date,
    start_odometer, end_odometer, odometer_delta_km, gps_distance_km,
    variance_percentage, fuel_rate_per_km, fuel_claim_amount, daily_allowance_amount,
    toll_parking_amount, total_claim_amount, status
) VALUES
(
    'baaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
    'd1111111-dddd-dddd-dddd-dddddddddddd',
    '44444444-4444-4444-4444-444444444444',
    '2026-10-20',
    24510.50,
    24558.00,
    47.50,
    44.80,
    6.03,
    8.50,
    403.75,
    600.00,
    150.00,
    1153.75,
    'SUBMITTED'
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 14. SEED TARGETS & KPIS
-- ----------------------------------------------------------------------------
INSERT INTO targets_kpis (
    id, user_id, period_type, period_start, period_end,
    target_volume_liters, achieved_volume_liters,
    target_revenue, achieved_revenue,
    target_visits, achieved_visits,
    target_new_clients, achieved_new_clients, calculated_kpi_score
) VALUES
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
    '44444444-4444-4444-4444-444444444444',
    'MONTHLY',
    '2026-10-01',
    '2026-10-31',
    3500.00,
    2480.00,
    3800000.00,
    2750000.00,
    220,
    165,
    12,
    9,
    74.25
)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 15. SEED COMPETITIONS & 16. COMPETITION SCORES
-- ----------------------------------------------------------------------------
INSERT INTO competitions (
    id, title, description, start_date, end_date, target_metric, reward_details, is_active
) VALUES
(
    'caaaaaaa-cccc-cccc-cccc-ccccccccccc1',
    'Q4 2026 Lubricant Champion Trophy',
    'National volume race across all territories in Pakistan. Highest liters booked wins cash prize and luxury tour.',
    '2026-10-01',
    '2026-12-31',
    'VOLUME_LITERS',
    '{"first_prize": "PKR 250,000 + Gold Medal", "second_prize": "PKR 150,000", "third_prize": "PKR 75,000"}'::jsonb,
    TRUE
),
(
    'caaaaaaa-cccc-cccc-cccc-ccccccccccc2',
    'October Synthetic Rush',
    'Highest percentage share of Apex Synthetic 5W-30 sales in total volume.',
    '2026-10-01',
    '2026-10-31',
    'SYNTHETIC_RATIO',
    '{"winner_prize": "55-inch 4K Smart TV", "min_qualifying_cartons": 50}'::jsonb,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO competition_scores (
    competition_id, user_id, current_metric_value, current_rank, points
) VALUES
(
    'caaaaaaa-cccc-cccc-cccc-ccccccccccc1',
    '44444444-4444-4444-4444-444444444444',
    2480.00,
    3,
    2480
)
ON CONFLICT (competition_id, user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 17. SEED API KEYS
-- ----------------------------------------------------------------------------
-- API Key Plaintext: 'enol_live_9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d'
-- SHA-256 Hash: '3a7b526fc85ab6a6dfa25efabda1ee981e42777fbbe9b08f51152a65d568393e'
INSERT INTO api_keys (
    user_id, key_name, key_hash, key_prefix, scopes, is_active
) VALUES
(
    '55555555-5555-5555-5555-555555555555',
    'Enterprise ERP Connector Key',
    '3a7b526fc85ab6a6dfa25efabda1ee981e42777fbbe9b08f51152a65d568393e',
    'enol_live_9a8b',
    ARRAY['read', 'write', 'admin', 'sync', 'webhooks'],
    TRUE
);

-- ----------------------------------------------------------------------------
-- 18. SEED WEBHOOK SUBSCRIPTION
-- ----------------------------------------------------------------------------
INSERT INTO webhook_subscriptions (
    id, name, target_url, secret, events, is_active, headers
) VALUES
(
    'daaaaaaa-dddd-dddd-dddd-ddddddddddd1',
    'Central SAP ERP Webhook Gateway',
    'https://erp-gateway.enollubricants.com/api/v1/integrations/enol-salesforze',
    'whsec_7f9a2b4c8d1e3f5a0b2c4d6e8f0a1b3c5d7e9f1a3b5c7d9e',
    ARRAY['order.created', 'order.approved', 'visit.completed', 'tada.submitted'],
    TRUE,
    '{"X-Company-Code": "ENOL-PK", "X-Integration-Version": "1.0"}'::jsonb
)
ON CONFLICT (id) DO NOTHING;
