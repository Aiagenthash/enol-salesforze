package com.enol.salesforze.data.remote.api

import com.enol.salesforze.data.remote.models.ApiResponse
import com.enol.salesforze.data.remote.models.AttendanceDto
import com.enol.salesforze.data.remote.models.AwardPointsRequest
import com.enol.salesforze.data.remote.models.BeatDto
import com.enol.salesforze.data.remote.models.BreadcrumbBatchRequest
import com.enol.salesforze.data.remote.models.BreadcrumbDto
import com.enol.salesforze.data.remote.models.CheckInVisitRequest
import com.enol.salesforze.data.remote.models.CheckOutVisitRequest
import com.enol.salesforze.data.remote.models.ClientDto
import com.enol.salesforze.data.remote.models.ClientLedgerResponse
import com.enol.salesforze.data.remote.models.CompetitionDto
import com.enol.salesforze.data.remote.models.CompetitionLeaderboardDto
import com.enol.salesforze.data.remote.models.CompetitionScoreDto
import com.enol.salesforze.data.remote.models.CreateClientRequest
import com.enol.salesforze.data.remote.models.CreateLedgerEntryRequest
import com.enol.salesforze.data.remote.models.CreateOrderRequest
import com.enol.salesforze.data.remote.models.DeltaSyncResponseDto
import com.enol.salesforze.data.remote.models.DistrictDto
import com.enol.salesforze.data.remote.models.EndShiftRequest
import com.enol.salesforze.data.remote.models.KpiScorecardDto
import com.enol.salesforze.data.remote.models.LedgerEntryDto
import com.enol.salesforze.data.remote.models.LoginRequest
import com.enol.salesforze.data.remote.models.LoginResponse
import com.enol.salesforze.data.remote.models.MechanicDto
import com.enol.salesforze.data.remote.models.OrderDto
import com.enol.salesforze.data.remote.models.PaginatedResponse
import com.enol.salesforze.data.remote.models.ProductDto
import com.enol.salesforze.data.remote.models.RecordCompetitionScoreRequest
import com.enol.salesforze.data.remote.models.RegisterMechanicRequest
import com.enol.salesforze.data.remote.models.ReviewTadaClaimRequest
import com.enol.salesforze.data.remote.models.RouteGeoJsonDto
import com.enol.salesforze.data.remote.models.StartShiftRequest
import com.enol.salesforze.data.remote.models.SubmitTadaClaimRequest
import com.enol.salesforze.data.remote.models.ApiKeyDto
import com.enol.salesforze.data.remote.models.CreateApiKeyRequest
import com.enol.salesforze.data.remote.models.CreateApiKeyResponse
import com.enol.salesforze.data.remote.models.WebhookSubscriptionDto
import com.enol.salesforze.data.remote.models.CreateWebhookSubscriptionRequest
import com.enol.salesforze.data.remote.models.WebhookDeliveryLogDto
import com.enol.salesforze.data.remote.models.SubordinatesResponse
import com.enol.salesforze.data.remote.models.TadaClaimDto
import com.enol.salesforze.data.remote.models.TargetKpiDto
import com.enol.salesforze.data.remote.models.UpdateOrderStatusRequest
import com.enol.salesforze.data.remote.models.UserDto
import com.enol.salesforze.data.remote.models.UserProfileResponse
import com.enol.salesforze.data.remote.models.VisitDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Enterprise Retrofit2 API interface matching all Phase 1 /api/v1 endpoints.
 */
interface EnterpriseFieldApi {

    // ========================================================================
    // AUTHENTICATION & IDENTITY
    // ========================================================================

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/v1/auth/me")
    suspend fun getMe(): UserProfileResponse

    // ========================================================================
    // USERS & HIERARCHY
    // ========================================================================

    @GET("api/v1/users")
    suspend fun getUsers(
        @Query("role") role: String? = null,
        @Query("manager_id") managerId: String? = null,
        @Query("is_active") isActive: Boolean? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null
    ): PaginatedResponse<UserDto>

    @GET("api/v1/users/{id}")
    suspend fun getUserById(
        @Path("id") id: String
    ): ApiResponse<UserDto>

    @GET("api/v1/users/{id}/subordinates")
    suspend fun getSubordinates(
        @Path("id") id: String
    ): SubordinatesResponse

    // ========================================================================
    // TERRITORIES & BEATS
    // ========================================================================

    @GET("api/v1/territories/districts")
    suspend fun getDistricts(
        @Query("province") province: String? = null,
        @Query("assigned_zsm_id") assignedZsmId: String? = null,
        @Query("assigned_asm_id") assignedAsmId: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<DistrictDto>

    @GET("api/v1/territories/beats")
    suspend fun getBeats(
        @Query("district_id") districtId: String? = null,
        @Query("day_of_week") dayOfWeek: String? = null,
        @Query("assigned_rep_id") assignedRepId: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<BeatDto>

    // ========================================================================
    // CLIENTS & LEDGER
    // ========================================================================

    @GET("api/v1/clients")
    suspend fun getClients(
        @Query("beat_id") beatId: String? = null,
        @Query("district_id") districtId: String? = null,
        @Query("credit_hold") creditHold: Boolean? = null,
        @Query("approval_status") approvalStatus: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<ClientDto>

    @GET("api/v1/clients/{id}")
    suspend fun getClientById(
        @Path("id") id: String
    ): ApiResponse<ClientDto>

    @POST("api/v1/clients")
    suspend fun createClient(
        @Body request: CreateClientRequest
    ): ApiResponse<ClientDto>

    @GET("api/v1/clients/{id}/ledger")
    suspend fun getClientLedger(
        @Path("id") id: String
    ): ClientLedgerResponse

    @POST("api/v1/clients/{id}/ledger")
    suspend fun postLedgerEntry(
        @Path("id") id: String,
        @Body request: CreateLedgerEntryRequest
    ): ApiResponse<LedgerEntryDto>

    // ========================================================================
    // MECHANICS & LOYALTY
    // ========================================================================

    @GET("api/v1/mechanics")
    suspend fun getMechanics(
        @Query("specialty") specialty: String? = null,
        @Query("wallet_provider") walletProvider: String? = null,
        @Query("is_verified") isVerified: Boolean? = null,
        @Query("client_id") clientId: String? = null,
        @Query("qr_code_id") qrCodeId: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<MechanicDto>

    @POST("api/v1/mechanics")
    suspend fun registerMechanic(
        @Body request: RegisterMechanicRequest
    ): ApiResponse<MechanicDto>

    @POST("api/v1/mechanics/{id}/points")
    suspend fun awardMechanicPoints(
        @Path("id") id: String,
        @Body request: AwardPointsRequest
    ): ApiResponse<MechanicDto>

    // ========================================================================
    // PRODUCTS & SCHEMES
    // ========================================================================

    @GET("api/v1/products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("viscosity_grade") viscosityGrade: String? = null,
        @Query("is_active") isActive: Boolean? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<ProductDto>

    @GET("api/v1/products/{id}")
    suspend fun getProductById(
        @Path("id") id: String
    ): ApiResponse<ProductDto>

    // ========================================================================
    // ORDERS & LINE ITEMS
    // ========================================================================

    @GET("api/v1/orders")
    suspend fun getOrders(
        @Query("client_id") clientId: String? = null,
        @Query("sales_rep_id") salesRepId: String? = null,
        @Query("beat_id") beatId: String? = null,
        @Query("status") status: String? = null,
        @Query("credit_hold_flag") creditHoldFlag: Boolean? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<OrderDto>

    @GET("api/v1/orders/{id}")
    suspend fun getOrderById(
        @Path("id") id: String
    ): ApiResponse<OrderDto>

    @POST("api/v1/orders")
    suspend fun createOrder(
        @Header("X-Idempotency-Key") idempotencyKey: String?,
        @Body request: CreateOrderRequest
    ): ApiResponse<OrderDto>

    @PUT("api/v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: String,
        @Body request: UpdateOrderStatusRequest
    ): ApiResponse<OrderDto>

    // ========================================================================
    // ATTENDANCE & SHIFTS
    // ========================================================================

    @GET("api/v1/attendance")
    suspend fun getAttendance(
        @Query("user_id") userId: String? = null,
        @Query("status") status: String? = null,
        @Query("date") date: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<AttendanceDto>

    @POST("api/v1/attendance/start-shift")
    suspend fun startShift(
        @Body request: StartShiftRequest
    ): ApiResponse<AttendanceDto>

    @POST("api/v1/attendance/{id}/end-shift")
    suspend fun endShift(
        @Path("id") id: String,
        @Body request: EndShiftRequest
    ): ApiResponse<AttendanceDto>

    // ========================================================================
    // VISITS & FIELD CHECK-INS
    // ========================================================================

    @GET("api/v1/visits")
    suspend fun getVisits(
        @Query("client_id") clientId: String? = null,
        @Query("sales_rep_id") salesRepId: String? = null,
        @Query("attendance_id") attendanceId: String? = null,
        @Query("outcome") outcome: String? = null,
        @Query("date") date: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<VisitDto>

    @POST("api/v1/visits/check-in")
    suspend fun checkInVisit(
        @Body request: CheckInVisitRequest
    ): ApiResponse<VisitDto>

    @PUT("api/v1/visits/{id}/check-out")
    suspend fun checkOutVisit(
        @Path("id") id: String,
        @Body request: CheckOutVisitRequest
    ): ApiResponse<VisitDto>

    // ========================================================================
    // LOCATION BREADCRUMBS & PLAYBACK
    // ========================================================================

    @GET("api/v1/breadcrumbs")
    suspend fun getBreadcrumbs(
        @Query("user_id") userId: String? = null,
        @Query("attendance_id") attendanceId: String? = null,
        @Query("date") date: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<BreadcrumbDto>

    @POST("api/v1/breadcrumbs")
    suspend fun batchUploadBreadcrumbs(
        @Body request: BreadcrumbBatchRequest
    ): ApiResponse<Map<String, String>>

    @GET("api/v1/breadcrumbs/user/{userId}/route")
    suspend fun getRoutePlayback(
        @Path("userId") userId: String,
        @Query("date") date: String? = null
    ): ApiResponse<RouteGeoJsonDto>

    // ========================================================================
    // TADA CLAIMS & TRAVEL ALLOWANCES
    // ========================================================================

    @GET("api/v1/tada-claims")
    suspend fun getTadaClaims(
        @Query("user_id") userId: String? = null,
        @Query("status") status: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<TadaClaimDto>

    @POST("api/v1/tada-claims")
    suspend fun submitTadaClaim(
        @Body request: SubmitTadaClaimRequest
    ): ApiResponse<TadaClaimDto>

    @PUT("api/v1/tada-claims/{id}/review")
    suspend fun reviewTadaClaim(
        @Path("id") id: String,
        @Body request: ReviewTadaClaimRequest
    ): ApiResponse<TadaClaimDto>

    // ========================================================================
    // TARGETS & KPIS
    // ========================================================================

    @GET("api/v1/kpis")
    suspend fun getKpis(
        @Query("user_id") userId: String? = null,
        @Query("period_type") periodType: String? = null,
        @Query("period_start") periodStart: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<TargetKpiDto>

    @GET("api/v1/kpis/user/{userId}")
    suspend fun getUserKpiScorecard(
        @Path("userId") userId: String
    ): KpiScorecardDto

    // ========================================================================
    // SALES COMPETITIONS & GAMIFICATION
    // ========================================================================

    @GET("api/v1/competitions")
    suspend fun getCompetitions(
        @Query("is_active") isActive: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): PaginatedResponse<CompetitionDto>

    @GET("api/v1/competitions/{id}/leaderboard")
    suspend fun getCompetitionLeaderboard(
        @Path("id") id: String
    ): CompetitionLeaderboardDto

    @POST("api/v1/competitions/{id}/scores")
    suspend fun recordCompetitionScore(
        @Path("id") id: String,
        @Body request: RecordCompetitionScoreRequest
    ): ApiResponse<CompetitionScoreDto>

    // ========================================================================
    // OFFLINE DELTA SYNC
    // ========================================================================

    @GET("api/v1/sync/delta")
    suspend fun getDeltaSync(
        @Query("since") since: String? = null
    ): DeltaSyncResponseDto

    // ========================================================================
    // ADMIN API GOVERNANCE & WEBHOOKS
    // ========================================================================

    @GET("api/v1/auth/api-keys")
    suspend fun getApiKeys(): ApiResponse<List<ApiKeyDto>>

    @POST("api/v1/auth/api-keys")
    suspend fun createApiKey(
        @Body request: CreateApiKeyRequest
    ): CreateApiKeyResponse

    @GET("api/v1/webhooks/subscriptions")
    suspend fun getWebhookSubscriptions(): ApiResponse<List<WebhookSubscriptionDto>>

    @POST("api/v1/webhooks/subscriptions")
    suspend fun createWebhookSubscription(
        @Body request: CreateWebhookSubscriptionRequest
    ): ApiResponse<WebhookSubscriptionDto>

    @GET("api/v1/webhooks/logs")
    suspend fun getWebhookLogs(
        @Query("limit") limit: Int? = 50
    ): ApiResponse<List<WebhookDeliveryLogDto>>
}
