package com.enol.salesforze.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.ProductEntity
import com.enol.salesforze.data.local.entities.UserEntity
import com.enol.salesforze.data.remote.api.EnterpriseFieldApi
import com.enol.salesforze.data.remote.models.BeatDto
import com.enol.salesforze.data.remote.models.CreateBeatRequest
import com.enol.salesforze.data.remote.models.CreateProductRequest
import com.enol.salesforze.data.remote.models.CreateUserRequest
import com.enol.salesforze.data.remote.models.DistrictDto
import com.enol.salesforze.data.remote.models.ProductDto
import com.enol.salesforze.data.remote.models.UpdateBeatRequest
import com.enol.salesforze.data.remote.models.UpdateDistrictRequest
import com.enol.salesforze.data.remote.models.UpdateProductRequest
import com.enol.salesforze.data.remote.models.UpdateUserRequest
import com.enol.salesforze.data.remote.models.UserDto
import com.enol.salesforze.data.remote.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminManagementUiState(
    val selectedTab: Int = 0, // 0: Users & Rights, 1: Products & Pricing, 2: Territories & Routes
    val users: List<UserDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val districts: List<DistrictDto> = emptyList(),
    val beats: List<BeatDto> = emptyList(),
    val userRoleFilter: String = "ALL",
    val productCategoryFilter: String = "ALL",
    val districtFilter: String = "ALL",
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Add User Dialog State
    val showAddUserDialog: Boolean = false,
    // Add Product Dialog State
    val showAddProductDialog: Boolean = false,
    // Edit User Dialog State
    val editingUser: UserDto? = null,
    // Edit Product Dialog State
    val editingProduct: ProductDto? = null,
    // Territory Dialog States
    val editingDistrict: DistrictDto? = null,
    val editingBeat: BeatDto? = null,
    val showCreateBeatDialog: Boolean = false,
    val targetDistrictForNewBeat: DistrictDto? = null
)

@HiltViewModel
class AdminManagementViewModel @Inject constructor(
    private val api: EnterpriseFieldApi,
    private val sessionManager: SessionManager,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminManagementUiState())
    val uiState: StateFlow<AdminManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index, searchQuery = "") }
    }

    fun setUserRoleFilter(role: String) {
        _uiState.update { it.copy(userRoleFilter = role) }
    }

    fun setProductCategoryFilter(category: String) {
        _uiState.update { it.copy(productCategoryFilter = category) }
    }

    fun setDistrictFilter(districtId: String) {
        _uiState.update { it.copy(districtFilter = districtId) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openAddUserDialog() {
        _uiState.update { it.copy(showAddUserDialog = true) }
    }

    fun closeAddUserDialog() {
        _uiState.update { it.copy(showAddUserDialog = false) }
    }

    fun openAddProductDialog() {
        _uiState.update { it.copy(showAddProductDialog = true) }
    }

    fun closeAddProductDialog() {
        _uiState.update { it.copy(showAddProductDialog = false) }
    }

    fun openEditUserDialog(user: UserDto) {
        _uiState.update { it.copy(editingUser = user) }
    }

    fun closeEditUserDialog() {
        _uiState.update { it.copy(editingUser = null) }
    }

    fun openEditProductDialog(product: ProductDto) {
        _uiState.update { it.copy(editingProduct = product) }
    }

    fun closeEditProductDialog() {
        _uiState.update { it.copy(editingProduct = null) }
    }

    fun openAssignDistrictManagersDialog(district: DistrictDto) {
        _uiState.update { it.copy(editingDistrict = district) }
    }

    fun closeAssignDistrictManagersDialog() {
        _uiState.update { it.copy(editingDistrict = null) }
    }

    fun openAssignBeatRepDialog(beat: BeatDto) {
        _uiState.update { it.copy(editingBeat = beat) }
    }

    fun closeAssignBeatRepDialog() {
        _uiState.update { it.copy(editingBeat = null) }
    }

    fun openCreateBeatDialog(district: DistrictDto? = null) {
        _uiState.update { it.copy(showCreateBeatDialog = true, targetDistrictForNewBeat = district) }
    }

    fun closeCreateBeatDialog() {
        _uiState.update { it.copy(showCreateBeatDialog = false, targetDistrictForNewBeat = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch Users
                val usersRes = runCatching { api.getUsers(limit = 100) }.getOrNull()
                val userList = usersRes?.data ?: emptyList()

                // Fetch Products
                val productsRes = runCatching { api.getProducts(limit = 100) }.getOrNull()
                val productList = productsRes?.data ?: emptyList()

                // Fetch Districts
                val districtsRes = runCatching { api.getDistricts(limit = 100) }.getOrNull()
                val districtList = districtsRes?.data ?: emptyList()

                // Fetch Beats
                val beatsRes = runCatching { api.getBeats(limit = 200) }.getOrNull()
                val beatList = beatsRes?.data ?: emptyList()

                _uiState.update {
                    it.copy(
                        users = userList,
                        products = productList,
                        districts = districtList,
                        beats = beatList,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load management data: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun createUser(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String,
        managerId: String?,
        vehicleType: String,
        vehicleNumber: String,
        fuelRate: Double,
        dailyAllowance: Double
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val req = CreateUserRequest(
                    email = email.trim(),
                    password = password,
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    role = role,
                    managerId = managerId?.ifBlank { null },
                    vehicleType = vehicleType,
                    vehicleNumber = vehicleNumber.ifBlank { null },
                    fuelRatePerKm = fuelRate,
                    dailyAllowance = dailyAllowance
                )
                val res = api.createUser(req)
                if (res.success && res.data != null) {
                    val newUser = res.data
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            showAddUserDialog = false,
                            users = listOf(newUser) + it.users,
                            successMessage = "User '${newUser.fullName}' created with role '$role'!"
                        )
                    }
                    // Cache in Room
                    database.userDao().insertUser(
                        UserEntity(
                            id = newUser.id,
                            email = newUser.email,
                            fullName = newUser.fullName,
                            phone = newUser.phone,
                            role = newUser.role,
                            managerId = newUser.managerId,
                            hierarchyPath = newUser.hierarchyPath,
                            vehicleType = newUser.vehicleType,
                            vehicleNumber = newUser.vehicleNumber,
                            fuelRatePerKm = newUser.fuelRatePerKm,
                            dailyAllowance = newUser.dailyAllowance,
                            isActive = newUser.isActive,
                            updatedAt = newUser.updatedAt ?: ""
                        )
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = res.error ?: "Could not create user"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        errorMessage = "Error creating user: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updateUserRights(
        userId: String,
        newRole: String?,
        fullName: String?,
        phone: String?,
        managerId: String?,
        vehicleType: String?,
        vehicleNumber: String?,
        fuelRate: Double?,
        dailyAllowance: Double?,
        isActive: Boolean?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val req = UpdateUserRequest(
                    fullName = fullName,
                    phone = phone,
                    role = newRole,
                    managerId = managerId,
                    vehicleType = vehicleType,
                    vehicleNumber = vehicleNumber,
                    fuelRatePerKm = fuelRate,
                    dailyAllowance = dailyAllowance,
                    isActive = isActive
                )
                val res = api.updateUser(userId, req)
                if (res.success && res.data != null) {
                    val updated = res.data
                    _uiState.update { state ->
                        state.copy(
                            isActionLoading = false,
                            editingUser = null,
                            users = state.users.map { if (it.id == userId) updated else it },
                            successMessage = "User rights updated for '${updated.fullName}'!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = res.error ?: "Update failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Error updating user: ${e.localizedMessage}")
                }
            }
        }
    }

    fun deactivateUser(userId: String, userName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val res = api.deleteUser(userId)
                if (res.success) {
                    _uiState.update { state ->
                        state.copy(
                            isActionLoading = false,
                            users = state.users.map {
                                if (it.id == userId) it.copy(isActive = false) else it
                            },
                            successMessage = "User '$userName' deactivated."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = res.error ?: "Deactivation failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun assignDistrictManagers(
        districtId: String,
        zsmId: String?,
        asmId: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val req = UpdateDistrictRequest(
                    assignedZsmId = zsmId,
                    assignedAsmId = asmId
                )
                val res = api.updateDistrict(districtId, req)
                if (res.success && res.data != null) {
                    val updated = res.data
                    _uiState.update { state ->
                        state.copy(
                            isActionLoading = false,
                            editingDistrict = null,
                            districts = state.districts.map { if (it.id == districtId) updated else it },
                            successMessage = "District '${updated.name}' managers updated successfully!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = res.error ?: "Failed to assign managers")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun assignBeatRep(
        beatId: String,
        repId: String?,
        dayOfWeek: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val req = UpdateBeatRequest(
                    assignedRepId = repId,
                    dayOfWeek = dayOfWeek
                )
                val res = api.updateBeat(beatId, req)
                if (res.success && res.data != null) {
                    val updated = res.data
                    _uiState.update { state ->
                        state.copy(
                            isActionLoading = false,
                            editingBeat = null,
                            beats = state.beats.map { if (it.id == beatId) updated else it },
                            successMessage = "Beat '${updated.name}' rep updated!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = res.error ?: "Failed to assign rep")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun createBeat(
        districtId: String,
        name: String,
        code: String,
        dayOfWeek: String,
        assignedRepId: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val req = CreateBeatRequest(
                    districtId = districtId,
                    name = name.trim(),
                    code = code.trim().uppercase(),
                    dayOfWeek = dayOfWeek,
                    assignedRepId = assignedRepId?.ifBlank { null }
                )
                val res = api.createBeat(req)
                if (res.success && res.data != null) {
                    val newBeat = res.data
                    _uiState.update { state ->
                        state.copy(
                            isActionLoading = false,
                            showCreateBeatDialog = false,
                            targetDistrictForNewBeat = null,
                            beats = listOf(newBeat) + state.beats,
                            districts = state.districts.map { d ->
                                if (d.id == districtId) d.copy(totalBeats = d.totalBeats + 1) else d
                            },
                            successMessage = "Beat route '${newBeat.name}' created successfully!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = res.error ?: "Failed to create beat route")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun createProduct(
        sku: String,
        name: String,
        description: String,
        category: String,
        viscosityGrade: String,
        packSizeLiters: Double,
        unitsPerCarton: Int,
        tradePrice: Double,
        retailPrice: Double,
        stockQuantity: Int
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val req = CreateProductRequest(
                    sku = sku.trim().uppercase(),
                    name = name.trim(),
                    description = description.ifBlank { null },
                    category = category,
                    viscosityGrade = viscosityGrade.trim(),
                    packSizeLiters = packSizeLiters,
                    unitsPerCarton = unitsPerCarton,
                    tradePrice = tradePrice,
                    retailPrice = retailPrice,
                    stockQuantity = stockQuantity
                )
                val res = api.createProduct(req)
                if (res.success && res.data != null) {
                    val newProd = res.data
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            showAddProductDialog = false,
                            products = listOf(newProd) + it.products,
                            successMessage = "Lubricant SKU '${newProd.sku}' added to catalog!"
                        )
                    }
                    // Cache in Room
                    database.productDao().insertProduct(
                        ProductEntity(
                            id = newProd.id,
                            sku = newProd.sku,
                            name = newProd.name,
                            description = newProd.description,
                            category = newProd.category,
                            viscosityGrade = newProd.viscosityGrade,
                            packSizeLiters = newProd.packSizeLiters,
                            unitsPerCarton = newProd.unitsPerCarton,
                            tradePrice = newProd.tradePrice,
                            retailPrice = newProd.retailPrice,
                            stockQuantity = newProd.stockQuantity,
                            isActive = newProd.isActive,
                            updatedAt = newProd.updatedAt ?: ""
                        )
                    )
                } else {
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = res.error ?: "Could not add product")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Error creating product: ${e.localizedMessage}")
                }
            }
        }
    }

    fun updateProductPricing(
        productId: String,
        name: String?,
        tradePrice: Double?,
        retailPrice: Double?,
        stockQuantity: Int?,
        isActive: Boolean?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val req = UpdateProductRequest(
                    name = name,
                    tradePrice = tradePrice,
                    retailPrice = retailPrice,
                    stockQuantity = stockQuantity,
                    isActive = isActive
                )
                val res = api.updateProduct(productId, req)
                if (res.success && res.data != null) {
                    val updated = res.data
                    _uiState.update { state ->
                        state.copy(
                            isActionLoading = false,
                            editingProduct = null,
                            products = state.products.map { if (it.id == productId) updated else it },
                            successMessage = "Product '${updated.name}' updated!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = res.error ?: "Update failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Error updating product: ${e.localizedMessage}")
                }
            }
        }
    }

    fun deactivateProduct(productId: String, sku: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            try {
                val res = api.deleteProduct(productId)
                if (res.success) {
                    _uiState.update { state ->
                        state.copy(
                            isActionLoading = false,
                            products = state.products.map {
                                if (it.id == productId) it.copy(isActive = false) else it
                            },
                            successMessage = "Product '$sku' deactivated."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isActionLoading = false, errorMessage = res.error ?: "Deactivation failed")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Error: ${e.localizedMessage}")
                }
            }
        }
    }
}
