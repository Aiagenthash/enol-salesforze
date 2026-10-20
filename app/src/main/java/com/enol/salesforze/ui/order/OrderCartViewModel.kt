package com.enol.salesforze.ui.order

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.ClientEntity
import com.enol.salesforze.data.local.entities.OrderEntity
import com.enol.salesforze.data.local.entities.OrderItemEntity
import com.enol.salesforze.data.local.entities.ProductEntity
import com.enol.salesforze.data.local.entities.ProductScheme
import com.enol.salesforze.data.local.entities.PendingSyncQueueEntity
import com.enol.salesforze.data.remote.models.CreateOrderItemRequest
import com.enol.salesforze.data.remote.models.CreateOrderRequest
import com.enol.salesforze.data.remote.session.SessionManager
import com.enol.salesforze.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

/**
 * Calculated line item preview inside the active order cart.
 */
data class CartCalculatedItem(
    val product: ProductEntity,
    val cartons: Int,
    val units: Int,
    val totalLiters: Double,
    val lineGrossPkr: Double,
    val lineDiscountPkr: Double,
    val netLineTotalPkr: Double,
    val appliedSchemeDesc: String? = null
)

/**
 * UI State for the Multi-SKU Order Booking Cart & Credit Guardrail Screen.
 */
data class OrderCartUiState(
    val clientId: String = "",
    val client: ClientEntity? = null,
    val products: List<ProductEntity> = emptyList(),
    val cartCartons: Map<String, Int> = emptyMap(), // ProductId -> Cartons
    val selectedCategory: String? = null, // null for ALL
    val searchQuery: String = "",
    val paymentTerms: String = "CREDIT_15_DAYS",
    val notes: String = "",
    val totalCartons: Double = 0.0,
    val totalLiters: Double = 0.0,
    val grossAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val effectiveExposure: Double = 0.0,
    val exceedsCreditLimit: Boolean = false,
    val creditVariancePkr: Double = 0.0,
    val isSubmitting: Boolean = false,
    val orderSubmittedSuccess: Boolean = false,
    val createdOrderId: String? = null,
    val errorMessage: String? = null
) {
    val categories: List<String> = listOf("ALL", "PCMO", "MCO", "DEO", "HYDRAULIC", "GEAR_OIL")

    val filteredProducts: List<ProductEntity>
        get() = products.filter { product ->
            val matchesCategory = selectedCategory == null || selectedCategory == "ALL" || product.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true) || product.sku.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }

    val calculatedItems: List<CartCalculatedItem>
        get() = cartCartons.filter { it.value > 0 }.mapNotNull { (productId, cartons) ->
            val product = products.find { it.id == productId } ?: return@mapNotNull null
            val units = cartons * product.unitsPerCarton
            val liters = units * product.packSizeLiters
            val lineGross = cartons * product.unitsPerCarton * product.tradePrice

            // Apply promotional scheme if available
            var discount = 0.0
            var schemeDesc: String? = null
            if (!product.activeSchemesJson.isNullOrBlank() && product.activeSchemesJson != "[]") {
                try {
                    val schemes = Json { ignoreUnknownKeys = true }.decodeFromString<List<ProductScheme>>(product.activeSchemesJson)
                    for (scheme in schemes) {
                        if (scheme.discountPct != null && scheme.discountPct > 0) {
                            discount += lineGross * (scheme.discountPct / 100.0)
                            schemeDesc = "${scheme.discountPct.toInt()}% Scheme Discount"
                        } else if (scheme.perCartonPkr != null && scheme.perCartonPkr > 0) {
                            discount += cartons * scheme.perCartonPkr
                            schemeDesc = "PKR ${scheme.perCartonPkr.toInt()}/ctn Trade Rebate"
                        }
                    }
                } catch (_: Exception) {}
            }

            CartCalculatedItem(
                product = product,
                cartons = cartons,
                units = units,
                totalLiters = liters,
                lineGrossPkr = lineGross,
                lineDiscountPkr = discount,
                netLineTotalPkr = (lineGross - discount).coerceAtLeast(0.0),
                appliedSchemeDesc = schemeDesc
            )
        }
}

@HiltViewModel
class OrderCartViewModel @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val json: Json,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clientId: String = savedStateHandle.get<String>("clientId") ?: ""

    private val _uiState = MutableStateFlow(OrderCartUiState(clientId = clientId))
    val uiState: StateFlow<OrderCartUiState> = _uiState.asStateFlow()

    init {
        loadClientAndProducts()
    }

    fun setClientId(id: String) {
        _uiState.update { it.copy(clientId = id) }
        loadClient(id)
    }

    private fun loadClientAndProducts() {
        if (clientId.isNotBlank()) {
            loadClient(clientId)
        } else {
            // Load first client as default fallback if not passed via navigation argument
            viewModelScope.launch {
                database.clientDao().getAllClients().collect { clients ->
                    if (clients.isNotEmpty() && _uiState.value.client == null) {
                        val first = clients.first()
                        _uiState.update { it.copy(clientId = first.id, client = first) }
                        recalculateTotals()
                    }
                }
            }
        }

        viewModelScope.launch {
            database.productDao().getActiveProducts().collect { productList ->
                _uiState.update { it.copy(products = productList) }
                recalculateTotals()
            }
        }
    }

    private fun loadClient(id: String) {
        viewModelScope.launch {
            val client = database.clientDao().getClientByIdOnce(id)
            _uiState.update { it.copy(client = client) }
            recalculateTotals()
        }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = if (category == "ALL") null else category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setPaymentTerms(terms: String) {
        _uiState.update { it.copy(paymentTerms = terms) }
        recalculateTotals()
    }

    fun setNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun incrementCarton(productId: String) {
        _uiState.update { state ->
            val current = state.cartCartons[productId] ?: 0
            val updated = state.cartCartons.toMutableMap().apply {
                put(productId, current + 1)
            }
            state.copy(cartCartons = updated)
        }
        recalculateTotals()
    }

    fun decrementCarton(productId: String) {
        _uiState.update { state ->
            val current = state.cartCartons[productId] ?: 0
            val updated = state.cartCartons.toMutableMap().apply {
                if (current <= 1) {
                    remove(productId)
                } else {
                    put(productId, current - 1)
                }
            }
            state.copy(cartCartons = updated)
        }
        recalculateTotals()
    }

    fun setCartonQuantity(productId: String, count: Int) {
        _uiState.update { state ->
            val updated = state.cartCartons.toMutableMap().apply {
                if (count <= 0) {
                    remove(productId)
                } else {
                    put(productId, count)
                }
            }
            state.copy(cartCartons = updated)
        }
        recalculateTotals()
    }

    fun clearCart() {
        _uiState.update { it.copy(cartCartons = emptyMap()) }
        recalculateTotals()
    }

    private fun recalculateTotals() {
        val state = _uiState.value
        val items = state.calculatedItems

        val totalCartons = items.sumOf { it.cartons.toDouble() }
        val totalLiters = items.sumOf { it.totalLiters }
        val grossAmount = items.sumOf { it.lineGrossPkr }
        val discountAmount = items.sumOf { it.lineDiscountPkr }
        val netAmount = (grossAmount - discountAmount).coerceAtLeast(0.0)

        val client = state.client
        val outstanding = client?.outstandingBalance ?: 0.0
        val creditLimit = client?.creditLimit ?: 0.0
        val effectiveExposure = outstanding + netAmount

        val isCash = state.paymentTerms == "CASH_ON_DELIVERY"
        val exceedsCredit = !isCash && (effectiveExposure > creditLimit)
        val variance = if (exceedsCredit) effectiveExposure - creditLimit else 0.0

        _uiState.update {
            it.copy(
                totalCartons = totalCartons,
                totalLiters = totalLiters,
                grossAmount = grossAmount,
                discountAmount = discountAmount,
                netAmount = netAmount,
                effectiveExposure = effectiveExposure,
                exceedsCreditLimit = exceedsCredit,
                creditVariancePkr = variance
            )
        }
    }

    /**
     * Submits the booked order: validates items and credit terms, writes to local Room DB
     * within a transaction, and enqueues to OrderSyncWorker for background sync.
     */
    fun submitOrder() {
        val state = _uiState.value
        val client = state.client

        if (client == null) {
            _uiState.update { it.copy(errorMessage = "Please select a valid customer for order booking.") }
            return
        }

        val calculatedItems = state.calculatedItems
        if (calculatedItems.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Cart is empty. Add at least 1 carton to book an order.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val repId = sessionManager.getCachedUserId() ?: sessionManager.getUserId()
                    ?: throw IllegalStateException("Sales representative session not found.")

                val orderId = UUID.randomUUID().toString()
                val idempotencyKey = "ord_${orderId}_${System.currentTimeMillis()}"
                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
                val dateCode = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                val orderNumber = "ORD-$dateCode-${orderId.take(5).uppercase()}"

                val status = if (state.exceedsCreditLimit) "CREDIT_HOLD" else "PENDING"

                val orderEntity = OrderEntity(
                    id = orderId,
                    orderNumber = orderNumber,
                    clientId = client.id,
                    salesRepId = repId,
                    beatId = client.beatId,
                    totalCartons = state.totalCartons,
                    totalLiters = state.totalLiters,
                    grossAmount = state.grossAmount,
                    discountAmount = state.discountAmount,
                    netAmount = state.netAmount,
                    paymentTerms = state.paymentTerms,
                    creditHoldFlag = state.exceedsCreditLimit,
                    status = status,
                    externalOmsReference = null,
                    idempotencyKey = idempotencyKey,
                    notes = state.notes.ifBlank { null },
                    createdAt = nowIso,
                    updatedAt = nowIso
                )

                val lineEntities = calculatedItems.map { item ->
                    OrderItemEntity(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        productId = item.product.id,
                        cartons = item.cartons.toDouble(),
                        units = item.units,
                        packSizeLiters = item.product.packSizeLiters,
                        totalLiters = item.totalLiters,
                        unitPrice = item.product.tradePrice,
                        lineTotal = item.lineGrossPkr,
                        schemeDiscount = item.lineDiscountPkr,
                        netLineTotal = item.netLineTotalPkr
                    )
                }

                val requestItems = calculatedItems.map { item ->
                    CreateOrderItemRequest(
                        productId = item.product.id,
                        cartons = item.cartons.toDouble(),
                        units = item.units,
                        schemeDiscount = item.lineDiscountPkr
                    )
                }

                val orderRequest = CreateOrderRequest(
                    clientId = client.id,
                    beatId = client.beatId,
                    idempotencyKey = idempotencyKey,
                    paymentTerms = state.paymentTerms,
                    notes = state.notes.ifBlank { null },
                    externalOmsReference = null,
                    items = requestItems
                )

                val syncQueueEntity = PendingSyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "order",
                    entityId = orderId,
                    action = "CREATE_ORDER",
                    payloadJson = json.encodeToString(CreateOrderRequest.serializer(), orderRequest),
                    idempotencyKey = idempotencyKey,
                    attemptCount = 0,
                    lastAttemptAt = null,
                    errorMessage = null,
                    createdAt = nowIso
                )

                withContext(Dispatchers.IO) {
                    // Atomic transaction saving order & line items in Room
                    database.orderDao().insertOrderWithItems(orderEntity, lineEntities)
                    database.pendingSyncQueueDao().insertSyncItem(syncQueueEntity)
                }

                // Trigger background upload worker immediately
                syncManager.triggerImmediateUploadSync()

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        orderSubmittedSuccess = true,
                        createdOrderId = orderId,
                        cartCartons = emptyMap(),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to book order."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
