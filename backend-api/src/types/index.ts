// ============================================================================
// ENOL SALESFORZE - DOMAIN TYPES & INTERFACES
// ============================================================================

export type UserRole = 'DIRECTOR' | 'ZSM' | 'ASM' | 'SALES_REP' | 'ADMIN';

export type VehicleType = 'MOTORCYCLE' | 'CAR' | 'VAN' | 'OTHER';

export interface User {
  id: string;
  email: string;
  password_hash?: string;
  full_name: string;
  phone: string;
  role: UserRole;
  manager_id: string | null;
  hierarchy_path: string;
  bound_device_id: string | null;
  vehicle_type: VehicleType;
  vehicle_number: string | null;
  fuel_rate_per_km: number;
  daily_allowance: number;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface PakistanDistrict {
  id: string;
  name: string;
  code: string;
  province: string;
  polygon_geom?: any;
  polygon_geojson?: any;
  assigned_zsm_id: string | null;
  assigned_asm_id: string | null;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface TerritoryBeat {
  id: string;
  district_id: string;
  name: string;
  code: string;
  day_of_week: DayOfWeek;
  assigned_rep_id: string | null;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export type ClientApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED';

export interface Client {
  id: string;
  beat_id: string | null;
  district_id: string;
  business_name: string;
  owner_name: string;
  phone: string;
  cnic: string | null;
  address: string;
  latitude: number;
  longitude: number;
  credit_limit: number;
  outstanding_balance: number;
  credit_hold: boolean;
  approval_status: ClientApprovalStatus;
  created_by: string | null;
  created_at: string;
  updated_at: string;
}

export type LedgerEntryType = 'INVOICE' | 'PAYMENT' | 'CREDIT_NOTE' | 'DEBIT_NOTE' | 'ADJUSTMENT';
export type LedgerReferenceType = 'ORDER' | 'RECOVERY_SLIP' | 'BANK_TRANSFER' | 'CHEQUE' | 'MANUAL';

export interface ClientLedgerEntry {
  id: string;
  client_id: string;
  entry_type: LedgerEntryType;
  amount: number;
  balance_after: number;
  reference_type: LedgerReferenceType;
  reference_id: string | null;
  description: string | null;
  recorded_by: string | null;
  created_at: string;
}

export type MechanicSpecialty = '4-STROKE_MOTORCYCLE' | 'HEAVY_DIESEL' | 'PASSENGER_CAR' | 'TRACTOR_AGRI' | 'GENERAL_AUTO';
export type WalletProvider = 'EASYPAISA' | 'JAZZCASH' | 'NAYAPAY' | 'SADAPAY' | 'BANK_ACCOUNT';

export interface Mechanic {
  id: string;
  client_id: string | null;
  name: string;
  phone: string;
  cnic: string | null;
  specialty: MechanicSpecialty;
  wallet_provider: WalletProvider;
  wallet_account_number: string;
  qr_code_id: string;
  points_balance: number;
  total_scans: number;
  is_verified: boolean;
  registered_by: string | null;
  created_at: string;
  updated_at: string;
}

export type ProductCategory =
  | 'PASSENGER_CAR_MOTOR_OIL'
  | 'HEAVY_DUTY_DIESEL'
  | 'MOTORCYCLE_4T'
  | 'GEAR_OIL'
  | 'TRANSMISSION_FLUID'
  | 'COOLANT'
  | 'GREASE'
  | 'SPECIALTY';

export interface ProductScheme {
  type: string;
  buy_cartons?: number;
  free_cartons?: number;
  free_units?: number;
  min_cartons?: number;
  discount_pct?: number;
  per_carton_pkr?: number;
  description?: string;
}

export interface Product {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  category: ProductCategory;
  viscosity_grade: string;
  pack_size_liters: number;
  units_per_carton: number;
  trade_price: number;
  retail_price: number;
  active_schemes: ProductScheme[];
  stock_quantity: number;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export type PaymentTerms = 'CASH_ON_DELIVERY' | 'CREDIT_7_DAYS' | 'CREDIT_15_DAYS' | 'CREDIT_30_DAYS' | 'ADVANCE';
export type OrderStatus = 'PENDING' | 'APPROVED' | 'DISPATCHED' | 'DELIVERED' | 'CANCELLED' | 'REJECTED_CREDIT_HOLD';

export interface Order {
  id: string;
  order_number: string;
  client_id: string;
  sales_rep_id: string;
  beat_id: string | null;
  total_cartons: number;
  total_liters: number;
  gross_amount: number;
  discount_amount: number;
  net_amount: number;
  payment_terms: PaymentTerms;
  credit_hold_flag: boolean;
  status: OrderStatus;
  external_oms_reference: string | null;
  idempotency_key: string | null;
  notes: string | null;
  created_at: string;
  updated_at: string;
  items?: OrderItem[];
}

export interface OrderItem {
  id: string;
  order_id: string;
  product_id: string;
  cartons: number;
  units: number;
  pack_size_liters: number;
  total_liters: number;
  unit_price: number;
  line_total: number;
  scheme_discount: number;
  net_line_total: number;
  created_at: string;
  updated_at: string;
  product_name?: string;
  product_sku?: string;
}

export type AttendanceStatus = 'ACTIVE' | 'COMPLETED' | 'AUTO_CLOSED' | 'REJECTED';

export interface DailyAttendance {
  id: string;
  user_id: string;
  date: string;
  shift_start: string;
  shift_end: string | null;
  start_latitude: number;
  start_longitude: number;
  start_selfie_url: string;
  start_odometer_reading: number;
  start_odometer_photo_url: string;
  end_latitude: number | null;
  end_longitude: number | null;
  end_selfie_url: string | null;
  end_odometer_reading: number | null;
  end_odometer_photo_url: string | null;
  total_hours: number | null;
  status: AttendanceStatus;
  notes: string | null;
  created_at: string;
  updated_at: string;
}

export type VisitOutcome =
  | 'ORDER_PLACED'
  | 'PAYMENT_COLLECTED'
  | 'STOCK_AUDIT_ONLY'
  | 'CLIENT_CLOSED'
  | 'NO_ORDER'
  | 'MERCHANDISING_ONLY';

export interface Visit {
  id: string;
  attendance_id: string | null;
  client_id: string;
  sales_rep_id: string;
  scheduled_at: string | null;
  check_in_time: string;
  check_out_time: string | null;
  check_in_latitude: number;
  check_in_longitude: number;
  check_out_latitude: number | null;
  check_out_longitude: number | null;
  geofence_distance_meters: number | null;
  is_mock_gps: boolean;
  outcome: VisitOutcome;
  notes: string | null;
  recovery_slip_number: string | null;
  recovery_amount: number;
  order_id: string | null;
  created_at: string;
  updated_at: string;
}

export interface LocationBreadcrumb {
  id: string;
  user_id: string;
  attendance_id: string | null;
  latitude: number;
  longitude: number;
  accuracy_meters: number;
  speed: number | null;
  bearing: number | null;
  battery_level: number | null;
  is_mock_gps: boolean;
  recorded_at: string;
  created_at: string;
}

export type TadaStatus = 'SUBMITTED' | 'VERIFIED_FLAGGED' | 'APPROVED' | 'REJECTED' | 'DISBURSED';

export interface TadaClaim {
  id: string;
  attendance_id: string;
  user_id: string;
  claim_date: string;
  start_odometer: number;
  end_odometer: number;
  odometer_delta_km: number;
  gps_distance_km: number;
  variance_percentage: number;
  fuel_rate_per_km: number;
  fuel_claim_amount: number;
  daily_allowance_amount: number;
  toll_parking_amount: number;
  total_claim_amount: number;
  status: TadaStatus;
  approved_by: string | null;
  approved_at: string | null;
  rejection_reason: string | null;
  created_at: string;
  updated_at: string;
}

export type PeriodType = 'MONTHLY' | 'QUARTERLY' | 'ANNUAL';

export interface TargetKpi {
  id: string;
  user_id: string;
  period_type: PeriodType;
  period_start: string;
  period_end: string;
  target_volume_liters: number;
  achieved_volume_liters: number;
  target_revenue: number;
  achieved_revenue: number;
  target_visits: number;
  achieved_visits: number;
  target_new_clients: number;
  achieved_new_clients: number;
  calculated_kpi_score: number;
  created_at: string;
  updated_at: string;
}

export type CompetitionMetric = 'VOLUME_LITERS' | 'REVENUE_PKR' | 'NEW_CLIENTS' | 'SYNTHETIC_RATIO' | 'COLLECTION_RATE';

export interface Competition {
  id: string;
  title: string;
  description: string | null;
  start_date: string;
  end_date: string;
  target_metric: CompetitionMetric;
  reward_details: any;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface CompetitionScore {
  id: string;
  competition_id: string;
  user_id: string;
  current_metric_value: number;
  current_rank: number;
  points: number;
  last_calculated_at: string;
  created_at: string;
  updated_at: string;
  user_name?: string;
  user_role?: string;
}

export interface ApiKey {
  id: string;
  user_id: string;
  key_name: string;
  key_hash: string;
  key_prefix: string;
  scopes: string[];
  is_active: boolean;
  expires_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface WebhookSubscription {
  id: string;
  name: string;
  target_url: string;
  secret: string;
  events: string[];
  is_active: boolean;
  headers: Record<string, string>;
  created_at: string;
  updated_at: string;
}

export interface WebhookDeliveryLog {
  id: string;
  subscription_id: string;
  event_name: string;
  payload: any;
  response_status: number | null;
  response_body: string | null;
  attempt_number: number;
  error_message: string | null;
  delivered_at: string | null;
  created_at: string;
}

// ----------------------------------------------------------------------------
// REQUEST & RESPONSE INTERFACES
// ----------------------------------------------------------------------------

export interface PaginationParams {
  page?: number;
  limit?: number;
  sort?: string;
  order?: 'asc' | 'desc';
  search?: string;
}

export interface PaginatedResponse<T> {
  success: boolean;
  data: T[];
  pagination: {
    total: number;
    page: number;
    limit: number;
    totalPages: number;
    hasNextPage: boolean;
    hasPrevPage: boolean;
  };
}

export interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
  error?: string;
}

export interface AuthenticatedUser {
  id: string;
  email: string;
  role: UserRole;
  hierarchy_path: string;
  manager_id: string | null;
  full_name: string;
  authMethod: 'jwt' | 'api_key';
  scopes?: string[];
}

export interface DeltaSyncResponse {
  sync_timestamp: string;
  since: string | null;
  counts: Record<string, number>;
  delta: {
    users: User[];
    districts: PakistanDistrict[];
    beats: TerritoryBeat[];
    clients: Client[];
    products: Product[];
    orders: Order[];
    mechanics: Mechanic[];
    kpis: TargetKpi[];
    competitions: Competition[];
  };
}
