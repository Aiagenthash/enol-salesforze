package com.enol.salesforze.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ============================================================================
// GENERIC API & PAGINATION WRAPPERS
// ============================================================================

@Serializable
data class ApiResponse<T>(
    @SerialName("success") val success: Boolean = true,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: T? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("statusCode") val statusCode: Int? = null
)

@Serializable
data class PaginatedResponse<T>(
    @SerialName("success") val success: Boolean = true,
    @SerialName("data") val data: List<T> = emptyList(),
    @SerialName("pagination") val pagination: PaginationMetadata = PaginationMetadata()
)

@Serializable
data class PaginationMetadata(
    @SerialName("total") val total: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("limit") val limit: Int = 20,
    @SerialName("totalPages") val totalPages: Int = 1,
    @SerialName("hasNextPage") val hasNextPage: Boolean = false,
    @SerialName("hasPrevPage") val hasPrevPage: Boolean = false
)

// ============================================================================
// AUTH & USERS
// ============================================================================

@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String
)

@Serializable
data class LoginResponse(
    @SerialName("success") val success: Boolean = true,
    @SerialName("token") val token: String,
    @SerialName("user") val user: UserDto
)

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("phone") val phone: String,
    @SerialName("role") val role: String,
    @SerialName("manager_id") val managerId: String? = null,
    @SerialName("hierarchy_path") val hierarchyPath: String = "",
    @SerialName("bound_device_id") val boundDeviceId: String? = null,
    @SerialName("vehicle_type") val vehicleType: String = "MOTORCYCLE",
    @SerialName("vehicle_number") val vehicleNumber: String? = null,
    @SerialName("fuel_rate_per_km") val fuelRatePerKm: Double = 0.0,
    @SerialName("daily_allowance") val dailyAllowance: Double = 0.0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("manager_name") val managerName: String? = null,
    @SerialName("manager_role") val managerRole: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class UserProfileResponse(
    @SerialName("success") val success: Boolean = true,
    @SerialName("user") val user: UserDto,
    @SerialName("authMethod") val authMethod: String? = null
)

@Serializable
data class SubordinatesResponse(
    @SerialName("success") val success: Boolean = true,
    @SerialName("count") val count: Int = 0,
    @SerialName("subordinates") val subordinates: List<UserDto> = emptyList()
)

@Serializable
data class CreateUserRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("phone") val phone: String,
    @SerialName("role") val role: String,
    @SerialName("manager_id") val managerId: String? = null,
    @SerialName("vehicle_type") val vehicleType: String = "MOTORCYCLE",
    @SerialName("vehicle_number") val vehicleNumber: String? = null,
    @SerialName("fuel_rate_per_km") val fuelRatePerKm: Double = 7.50,
    @SerialName("daily_allowance") val dailyAllowance: Double = 500.00
)

@Serializable
data class UpdateUserRequest(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("manager_id") val managerId: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    @SerialName("vehicle_number") val vehicleNumber: String? = null,
    @SerialName("fuel_rate_per_km") val fuelRatePerKm: Double? = null,
    @SerialName("daily_allowance") val dailyAllowance: Double? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class SimpleActionResponse(
    @SerialName("success") val success: Boolean = true,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null
)

// ============================================================================
// TERRITORIES: DISTRICTS & BEATS
// ============================================================================

@Serializable
data class DistrictDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
    @SerialName("province") val province: String,
    @SerialName("polygon_geojson") val polygonGeoJson: JsonElement? = null,
    @SerialName("assigned_zsm_id") val assignedZsmId: String? = null,
    @SerialName("assigned_asm_id") val assignedAsmId: String? = null,
    @SerialName("zsm_name") val zsmName: String? = null,
    @SerialName("asm_name") val asmName: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("total_beats") val totalBeats: Int = 0,
    @SerialName("total_clients") val totalClients: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class BeatDto(
    @SerialName("id") val id: String,
    @SerialName("district_id") val districtId: String,
    @SerialName("district_name") val districtName: String? = null,
    @SerialName("name") val name: String,
    @SerialName("code") val code: String,
    @SerialName("day_of_week") val dayOfWeek: String,
    @SerialName("assigned_rep_id") val assignedRepId: String? = null,
    @SerialName("assigned_rep_name") val assignedRepName: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("total_clients") val totalClients: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ============================================================================
// CLIENTS & LEDGER
// ============================================================================

@Serializable
data class ClientDto(
    @SerialName("id") val id: String,
    @SerialName("beat_id") val beatId: String? = null,
    @SerialName("beat_name") val beatName: String? = null,
    @SerialName("district_id") val districtId: String,
    @SerialName("district_name") val districtName: String? = null,
    @SerialName("business_name") val businessName: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("phone") val phone: String,
    @SerialName("cnic") val cnic: String? = null,
    @SerialName("address") val address: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("credit_limit") val creditLimit: Double = 100000.0,
    @SerialName("outstanding_balance") val outstandingBalance: Double = 0.0,
    @SerialName("available_credit") val availableCredit: Double? = null,
    @SerialName("credit_hold") val creditHold: Boolean = false,
    @SerialName("approval_status") val approvalStatus: String = "PENDING",
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_by_name") val createdByName: String? = null,
    @SerialName("recent_ledger") val recentLedger: List<LedgerEntryDto>? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreateClientRequest(
    @SerialName("district_id") val districtId: String,
    @SerialName("beat_id") val beatId: String? = null,
    @SerialName("business_name") val businessName: String,
    @SerialName("owner_name") val ownerName: String,
    @SerialName("phone") val phone: String,
    @SerialName("cnic") val cnic: String? = null,
    @SerialName("address") val address: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("credit_limit") val creditLimit: Double = 100000.0
)

@Serializable
data class LedgerEntryDto(
    @SerialName("id") val id: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("entry_type") val entryType: String,
    @SerialName("amount") val amount: Double,
    @SerialName("balance_after") val balanceAfter: Double,
    @SerialName("reference_type") val referenceType: String = "MANUAL",
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("recorded_by") val recordedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateLedgerEntryRequest(
    @SerialName("entry_type") val entryType: String,
    @SerialName("amount") val amount: Double,
    @SerialName("reference_type") val referenceType: String = "MANUAL",
    @SerialName("reference_id") val referenceId: String? = null,
    @SerialName("description") val description: String? = null
)

@Serializable
data class ClientLedgerResponse(
    @SerialName("success") val success: Boolean = true,
    @SerialName("client") val client: ClientDto? = null,
    @SerialName("entries") val entries: List<LedgerEntryDto> = emptyList()
)

// ============================================================================
// MECHANICS & LOYALTY
// ============================================================================

@Serializable
data class MechanicDto(
    @SerialName("id") val id: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("linked_workshop_name") val linkedWorkshopName: String? = null,
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String,
    @SerialName("cnic") val cnic: String? = null,
    @SerialName("specialty") val specialty: String,
    @SerialName("wallet_provider") val walletProvider: String,
    @SerialName("wallet_account_number") val walletAccountNumber: String,
    @SerialName("qr_code_id") val qrCodeId: String,
    @SerialName("points_balance") val pointsBalance: Int = 0,
    @SerialName("total_scans") val totalScans: Int = 0,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("registered_by") val registeredBy: String? = null,
    @SerialName("registered_by_name") val registeredByName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RegisterMechanicRequest(
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("phone") val phone: String,
    @SerialName("cnic") val cnic: String? = null,
    @SerialName("specialty") val specialty: String,
    @SerialName("wallet_provider") val walletProvider: String,
    @SerialName("wallet_account_number") val walletAccountNumber: String,
    @SerialName("qr_code_id") val qrCodeId: String? = null
)

@Serializable
data class AwardPointsRequest(
    @SerialName("points") val points: Int,
    @SerialName("reason") val reason: String? = null
)

// ============================================================================
// PRODUCTS & SCHEMES
// ============================================================================

@Serializable
data class ProductSchemeDto(
    @SerialName("type") val type: String,
    @SerialName("buy_cartons") val buyCartons: Double? = null,
    @SerialName("free_cartons") val freeCartons: Double? = null,
    @SerialName("free_units") val freeUnits: Int? = null,
    @SerialName("min_cartons") val minCartons: Double? = null,
    @SerialName("discount_pct") val discountPct: Double? = null,
    @SerialName("per_carton_pkr") val perCartonPkr: Double? = null,
    @SerialName("description") val description: String? = null
)

@Serializable
data class ProductDto(
    @SerialName("id") val id: String,
    @SerialName("sku") val sku: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String,
    @SerialName("viscosity_grade") val viscosityGrade: String,
    @SerialName("pack_size_liters") val packSizeLiters: Double,
    @SerialName("units_per_carton") val unitsPerCarton: Int,
    @SerialName("trade_price") val tradePrice: Double,
    @SerialName("retail_price") val retailPrice: Double,
    @SerialName("active_schemes") val activeSchemes: List<ProductSchemeDto> = emptyList(),
    @SerialName("stock_quantity") val stockQuantity: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreateProductRequest(
    @SerialName("sku") val sku: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String,
    @SerialName("viscosity_grade") val viscosityGrade: String,
    @SerialName("pack_size_liters") val packSizeLiters: Double,
    @SerialName("units_per_carton") val unitsPerCarton: Int = 1,
    @SerialName("trade_price") val tradePrice: Double,
    @SerialName("retail_price") val retailPrice: Double,
    @SerialName("stock_quantity") val stockQuantity: Int = 1000
)

@Serializable
data class UpdateProductRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("trade_price") val tradePrice: Double? = null,
    @SerialName("retail_price") val retailPrice: Double? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

// ============================================================================
// ORDERS & LINE ITEMS
// ============================================================================

@Serializable
data class OrderItemDto(
    @SerialName("id") val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("cartons") val cartons: Double = 0.0,
    @SerialName("units") val units: Int = 0,
    @SerialName("pack_size_liters") val packSizeLiters: Double = 0.0,
    @SerialName("total_liters") val totalLiters: Double = 0.0,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("line_total") val lineTotal: Double = 0.0,
    @SerialName("scheme_discount") val schemeDiscount: Double = 0.0,
    @SerialName("net_line_total") val netLineTotal: Double = 0.0,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_sku") val productSku: String? = null,
    @SerialName("viscosity_grade") val viscosityGrade: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class OrderDto(
    @SerialName("id") val id: String,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("client_phone") val clientPhone: String? = null,
    @SerialName("credit_limit") val creditLimit: Double? = null,
    @SerialName("outstanding_balance") val outstandingBalance: Double? = null,
    @SerialName("sales_rep_id") val salesRepId: String,
    @SerialName("sales_rep_name") val salesRepName: String? = null,
    @SerialName("beat_id") val beatId: String? = null,
    @SerialName("beat_name") val beatName: String? = null,
    @SerialName("total_cartons") val totalCartons: Double = 0.0,
    @SerialName("total_liters") val totalLiters: Double = 0.0,
    @SerialName("gross_amount") val grossAmount: Double = 0.0,
    @SerialName("discount_amount") val discountAmount: Double = 0.0,
    @SerialName("net_amount") val netAmount: Double = 0.0,
    @SerialName("payment_terms") val paymentTerms: String = "CREDIT_15_DAYS",
    @SerialName("credit_hold_flag") val creditHoldFlag: Boolean = false,
    @SerialName("status") val status: String = "PENDING",
    @SerialName("external_oms_reference") val externalOmsReference: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("items") val items: List<OrderItemDto>? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreateOrderItemRequest(
    @SerialName("product_id") val productId: String,
    @SerialName("cartons") val cartons: Double = 0.0,
    @SerialName("units") val units: Int = 0,
    @SerialName("scheme_discount") val schemeDiscount: Double = 0.0
)

@Serializable
data class CreateOrderRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("beat_id") val beatId: String? = null,
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
    @SerialName("payment_terms") val paymentTerms: String = "CREDIT_15_DAYS",
    @SerialName("notes") val notes: String? = null,
    @SerialName("external_oms_reference") val externalOmsReference: String? = null,
    @SerialName("items") val items: List<CreateOrderItemRequest>
)

@Serializable
data class UpdateOrderStatusRequest(
    @SerialName("status") val status: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("external_oms_reference") val externalOmsReference: String? = null
)

// ============================================================================
// ATTENDANCE & SHIFTS
// ============================================================================

@Serializable
data class AttendanceDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_role") val userRole: String? = null,
    @SerialName("date") val date: String,
    @SerialName("shift_start") val shiftStart: String,
    @SerialName("shift_end") val shiftEnd: String? = null,
    @SerialName("start_latitude") val startLatitude: Double,
    @SerialName("start_longitude") val startLongitude: Double,
    @SerialName("start_selfie_url") val startSelfieUrl: String,
    @SerialName("start_odometer_reading") val startOdometerReading: Double,
    @SerialName("start_odometer_photo_url") val startOdometerPhotoUrl: String,
    @SerialName("end_latitude") val endLatitude: Double? = null,
    @SerialName("end_longitude") val endLongitude: Double? = null,
    @SerialName("end_selfie_url") val endSelfieUrl: String? = null,
    @SerialName("end_odometer_reading") val endOdometerReading: Double? = null,
    @SerialName("end_odometer_photo_url") val endOdometerPhotoUrl: String? = null,
    @SerialName("total_hours") val totalHours: Double? = null,
    @SerialName("status") val status: String = "ACTIVE",
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class StartShiftRequest(
    @SerialName("start_latitude") val startLatitude: Double,
    @SerialName("start_longitude") val startLongitude: Double,
    @SerialName("start_selfie_url") val startSelfieUrl: String,
    @SerialName("start_odometer_reading") val startOdometerReading: Double,
    @SerialName("start_odometer_photo_url") val startOdometerPhotoUrl: String,
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class EndShiftRequest(
    @SerialName("end_latitude") val endLatitude: Double,
    @SerialName("end_longitude") val endLongitude: Double,
    @SerialName("end_selfie_url") val endSelfieUrl: String,
    @SerialName("end_odometer_reading") val endOdometerReading: Double,
    @SerialName("end_odometer_photo_url") val endOdometerPhotoUrl: String,
    @SerialName("notes") val notes: String? = null
)

// ============================================================================
// VISITS & FIELD CHECK-INS
// ============================================================================

@Serializable
data class VisitDto(
    @SerialName("id") val id: String,
    @SerialName("attendance_id") val attendanceId: String? = null,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("client_address") val clientAddress: String? = null,
    @SerialName("sales_rep_id") val salesRepId: String,
    @SerialName("sales_rep_name") val salesRepName: String? = null,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("check_in_time") val checkInTime: String,
    @SerialName("check_out_time") val checkOutTime: String? = null,
    @SerialName("check_in_latitude") val checkInLatitude: Double,
    @SerialName("check_in_longitude") val checkInLongitude: Double,
    @SerialName("check_out_latitude") val checkOutLatitude: Double? = null,
    @SerialName("check_out_longitude") val checkOutLongitude: Double? = null,
    @SerialName("geofence_distance_meters") val geofenceDistanceMeters: Double? = null,
    @SerialName("is_mock_gps") val isMockGps: Boolean = false,
    @SerialName("outcome") val outcome: String = "NO_ORDER",
    @SerialName("notes") val notes: String? = null,
    @SerialName("recovery_slip_number") val recoverySlipNumber: String? = null,
    @SerialName("recovery_amount") val recoveryAmount: Double = 0.0,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CheckInVisitRequest(
    @SerialName("client_id") val clientId: String,
    @SerialName("attendance_id") val attendanceId: String? = null,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("check_in_latitude") val checkInLatitude: Double,
    @SerialName("check_in_longitude") val checkInLongitude: Double,
    @SerialName("is_mock_gps") val isMockGps: Boolean = false,
    @SerialName("outcome") val outcome: String = "NO_ORDER",
    @SerialName("notes") val notes: String? = null
)

@Serializable
data class CheckOutVisitRequest(
    @SerialName("check_out_latitude") val checkOutLatitude: Double,
    @SerialName("check_out_longitude") val checkOutLongitude: Double,
    @SerialName("outcome") val outcome: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("recovery_slip_number") val recoverySlipNumber: String? = null,
    @SerialName("recovery_amount") val recoveryAmount: Double = 0.0,
    @SerialName("order_id") val orderId: String? = null
)

// ============================================================================
// BREADCRUMBS & ROUTE PLAYBACK
// ============================================================================

@Serializable
data class BreadcrumbDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("attendance_id") val attendanceId: String? = null,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("accuracy_meters") val accuracyMeters: Double = 5.0,
    @SerialName("speed") val speed: Double? = null,
    @SerialName("bearing") val bearing: Double? = null,
    @SerialName("battery_level") val batteryLevel: Double? = null,
    @SerialName("is_mock_gps") val isMockGps: Boolean = false,
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BreadcrumbBatchRequest(
    @SerialName("attendance_id") val attendanceId: String? = null,
    @SerialName("breadcrumbs") val breadcrumbs: List<BreadcrumbDto>
)

@Serializable
data class RouteGeometryDto(
    @SerialName("type") val type: String = "LineString",
    @SerialName("coordinates") val coordinates: List<List<Double>> = emptyList()
)

@Serializable
data class RoutePropertiesDto(
    @SerialName("userId") val userId: String,
    @SerialName("date") val date: String,
    @SerialName("total_points") val totalPoints: Int = 0,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null
)

@Serializable
data class RouteGeoJsonDto(
    @SerialName("type") val type: String = "Feature",
    @SerialName("geometry") val geometry: RouteGeometryDto,
    @SerialName("properties") val properties: RoutePropertiesDto
)

// ============================================================================
// TADA CLAIMS & EXPENSES
// ============================================================================

@Serializable
data class TadaClaimDto(
    @SerialName("id") val id: String,
    @SerialName("attendance_id") val attendanceId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    @SerialName("vehicle_number") val vehicleNumber: String? = null,
    @SerialName("claim_date") val claimDate: String,
    @SerialName("start_odometer") val startOdometer: Double,
    @SerialName("end_odometer") val endOdometer: Double,
    @SerialName("odometer_delta_km") val odometerDeltaKm: Double = 0.0,
    @SerialName("gps_distance_km") val gpsDistanceKm: Double = 0.0,
    @SerialName("variance_percentage") val variancePercentage: Double = 0.0,
    @SerialName("fuel_rate_per_km") val fuelRatePerKm: Double = 0.0,
    @SerialName("fuel_claim_amount") val fuelClaimAmount: Double = 0.0,
    @SerialName("daily_allowance_amount") val dailyAllowanceAmount: Double = 0.0,
    @SerialName("toll_parking_amount") val tollParkingAmount: Double = 0.0,
    @SerialName("total_claim_amount") val totalClaimAmount: Double = 0.0,
    @SerialName("status") val status: String = "SUBMITTED",
    @SerialName("approved_by") val approvedBy: String? = null,
    @SerialName("approved_by_name") val approvedByName: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SubmitTadaClaimRequest(
    @SerialName("attendance_id") val attendanceId: String,
    @SerialName("claim_date") val claimDate: String,
    @SerialName("start_odometer") val startOdometer: Double,
    @SerialName("end_odometer") val endOdometer: Double,
    @SerialName("gps_distance_km") val gpsDistanceKm: Double,
    @SerialName("toll_parking_amount") val tollParkingAmount: Double = 0.0
)

@Serializable
data class ReviewTadaClaimRequest(
    @SerialName("status") val status: String,
    @SerialName("rejection_reason") val rejectionReason: String? = null
)

// ============================================================================
// KPIS & SCORECARDS
// ============================================================================

@Serializable
data class TargetKpiDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("period_type") val periodType: String,
    @SerialName("period_start") val periodStart: String,
    @SerialName("period_end") val periodEnd: String,
    @SerialName("target_volume_liters") val targetVolumeLiters: Double = 0.0,
    @SerialName("achieved_volume_liters") val achievedVolumeLiters: Double = 0.0,
    @SerialName("volume_achievement_pct") val volumeAchievementPct: Double? = null,
    @SerialName("target_revenue") val targetRevenue: Double = 0.0,
    @SerialName("achieved_revenue") val achievedRevenue: Double = 0.0,
    @SerialName("revenue_achievement_pct") val revenueAchievementPct: Double? = null,
    @SerialName("target_visits") val targetVisits: Int = 0,
    @SerialName("achieved_visits") val achievedVisits: Int = 0,
    @SerialName("target_new_clients") val targetNewClients: Int = 0,
    @SerialName("achieved_new_clients") val achievedNewClients: Int = 0,
    @SerialName("calculated_kpi_score") val calculatedKpiScore: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class KpiScorecardDto(
    @SerialName("success") val success: Boolean = true,
    @SerialName("user") val user: UserDto? = null,
    @SerialName("scorecards") val scorecards: List<TargetKpiDto> = emptyList()
)

// ============================================================================
// SALES COMPETITIONS & GAMIFICATION
// ============================================================================

@Serializable
data class CompetitionDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("target_metric") val targetMetric: String,
    @SerialName("reward_details") val rewardDetails: JsonElement? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CompetitionScoreDto(
    @SerialName("id") val id: String,
    @SerialName("competition_id") val competitionId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_role") val userRole: String? = null,
    @SerialName("current_metric_value") val currentMetricValue: Double = 0.0,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("points") val points: Int = 0,
    @SerialName("last_calculated_at") val lastCalculatedAt: String? = null
)

@Serializable
data class CompetitionLeaderboardDto(
    @SerialName("success") val success: Boolean = true,
    @SerialName("competition") val competition: CompetitionDto? = null,
    @SerialName("total_participants") val totalParticipants: Int = 0,
    @SerialName("leaderboard") val leaderboard: List<CompetitionScoreDto> = emptyList()
)

@Serializable
data class RecordCompetitionScoreRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("metric_value") val metricValue: Double,
    @SerialName("points") val points: Int? = null
)

// ============================================================================
// OFFLINE DELTA SYNC
// ============================================================================

@Serializable
data class DeltaPayloadDto(
    @SerialName("users") val users: List<UserDto> = emptyList(),
    @SerialName("districts") val districts: List<DistrictDto> = emptyList(),
    @SerialName("beats") val beats: List<BeatDto> = emptyList(),
    @SerialName("clients") val clients: List<ClientDto> = emptyList(),
    @SerialName("products") val products: List<ProductDto> = emptyList(),
    @SerialName("orders") val orders: List<OrderDto> = emptyList(),
    @SerialName("mechanics") val mechanics: List<MechanicDto> = emptyList(),
    @SerialName("kpis") val kpis: List<TargetKpiDto> = emptyList(),
    @SerialName("competitions") val competitions: List<CompetitionDto> = emptyList()
)

@Serializable
data class DeltaSyncResponseDto(
    @SerialName("success") val success: Boolean = true,
    @SerialName("sync_timestamp") val syncTimestamp: String,
    @SerialName("since") val since: String? = null,
    @SerialName("counts") val counts: Map<String, Int> = emptyMap(),
    @SerialName("delta") val delta: DeltaPayloadDto = DeltaPayloadDto()
)

// ============================================================================
// ADMIN API GOVERNANCE & WEBHOOKS
// ============================================================================

@Serializable
data class ApiKeyDto(
    @SerialName("id") val id: String,
    @SerialName("key_name") val keyName: String,
    @SerialName("key_prefix") val keyPrefix: String,
    @SerialName("scopes") val scopes: List<String> = emptyList(),
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreateApiKeyRequest(
    @SerialName("key_name") val keyName: String,
    @SerialName("scopes") val scopes: List<String> = emptyList(),
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class CreateApiKeyResponse(
    @SerialName("success") val success: Boolean = true,
    @SerialName("message") val message: String? = null,
    @SerialName("apiKey") val apiKey: String? = null,
    @SerialName("keyDetails") val keyDetails: ApiKeyDto? = null
)

@Serializable
data class WebhookSubscriptionDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("target_url") val targetUrl: String,
    @SerialName("events") val events: List<String> = emptyList(),
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CreateWebhookSubscriptionRequest(
    @SerialName("name") val name: String,
    @SerialName("target_url") val targetUrl: String,
    @SerialName("events") val events: List<String> = emptyList(),
    @SerialName("secret") val secret: String? = null
)

@Serializable
data class WebhookDeliveryLogDto(
    @SerialName("id") val id: String,
    @SerialName("subscription_id") val subscriptionId: String? = null,
    @SerialName("event_type") val eventType: String,
    @SerialName("target_url") val targetUrl: String? = null,
    @SerialName("status_code") val statusCode: Int? = null,
    @SerialName("duration_ms") val durationMs: Int? = null,
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("error_message") val errorMessage: String? = null
)
