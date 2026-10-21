package com.enol.salesforze.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.enol.salesforze.data.remote.models.ProductDto
import com.enol.salesforze.data.remote.models.UserDto
import com.enol.salesforze.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(
    viewModel: AdminManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val tabTitles = listOf(
        "Users & Rights (${uiState.users.size})",
        "Products & Catalog (${uiState.products.size})"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Control Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "Configure Users, Assign Rights & Manage Products",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateSteelLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurfaceDark)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = SlateSteelLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddUserDialog() },
                    containerColor = LubricantAmber,
                    contentColor = EnolNavyDark,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Add User", fontWeight = FontWeight.Bold) }
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddProductDialog() },
                    containerColor = LubricantAmber,
                    contentColor = EnolNavyDark,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Product", fontWeight = FontWeight.Bold) }
                )
            }
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = SurfaceDark,
                contentColor = LubricantAmber,
                divider = { HorizontalDivider(color = SlateSteelDark) }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == index) LubricantAmber else SlateSteelLight
                            )
                        }
                    )
                }
            }

            // Messages Banner
            if (uiState.successMessage != null) {
                Surface(
                    color = SuccessEmerald.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, SuccessEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.successMessage ?: "",
                            color = SuccessEmerald,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SlateSteelLight)
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SlateSteelLight)
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        if (uiState.selectedTab == 0) "Search users by name, email, or phone..."
                        else "Search products by SKU or name..."
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateSteelLight) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateSteelLight)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LubricantAmber,
                    unfocusedBorderColor = SlateSteelDark,
                    focusedTextColor = OnSurfaceDark,
                    unfocusedTextColor = OnSurfaceDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content per Tab
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LubricantAmber)
                }
            } else if (uiState.selectedTab == 0) {
                UsersTabContent(
                    uiState = uiState,
                    onRoleFilterSelected = { viewModel.setUserRoleFilter(it) },
                    onEditUser = { viewModel.openEditUserDialog(it) },
                    onDeactivateUser = { id, name -> viewModel.deactivateUser(id, name) }
                )
            } else {
                ProductsTabContent(
                    uiState = uiState,
                    onCategoryFilterSelected = { viewModel.setProductCategoryFilter(it) },
                    onEditProduct = { viewModel.openEditProductDialog(it) },
                    onDeactivateProduct = { id, sku -> viewModel.deactivateProduct(id, sku) }
                )
            }
        }
    }

    // Add User Dialog
    if (uiState.showAddUserDialog) {
        AddUserDialog(
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeAddUserDialog() },
            onConfirm = { name, email, phone, pwd, role, vType, vNum, fuelRate, allowance ->
                viewModel.createUser(name, email, phone, pwd, role, vType, vNum, fuelRate, allowance)
            }
        )
    }

    // Edit User Dialog
    if (uiState.editingUser != null) {
        val user = uiState.editingUser!!
        EditUserRightsDialog(
            user = user,
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeEditUserDialog() },
            onConfirm = { role, name, phone, vType, vNum, fuel, allow, active ->
                viewModel.updateUserRights(user.id, role, name, phone, vType, vNum, fuel, allow, active)
            }
        )
    }

    // Add Product Dialog
    if (uiState.showAddProductDialog) {
        AddProductDialog(
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeAddProductDialog() },
            onConfirm = { sku, name, desc, cat, visc, pack, units, trade, retail, stock ->
                viewModel.createProduct(sku, name, desc, cat, visc, pack, units, trade, retail, stock)
            }
        )
    }

    // Edit Product Dialog
    if (uiState.editingProduct != null) {
        val prod = uiState.editingProduct!!
        EditProductDialog(
            product = prod,
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeEditProductDialog() },
            onConfirm = { name, trade, retail, stock, active ->
                viewModel.updateProductPricing(prod.id, name, trade, retail, stock, active)
            }
        )
    }
}

// ============================================================================
// USERS TAB
// ============================================================================

@Composable
fun UsersTabContent(
    uiState: AdminManagementUiState,
    onRoleFilterSelected: (String) -> Unit,
    onEditUser: (UserDto) -> Unit,
    onDeactivateUser: (String, String) -> Unit
) {
    val roles = listOf("ALL", "DIRECTOR", "ZSM", "ASM", "SALES_REP", "ADMIN")

    val filteredUsers = uiState.users.filter { user ->
        val matchesRole = if (uiState.userRoleFilter == "ALL") true else user.role.equals(uiState.userRoleFilter, ignoreCase = true)
        val matchesSearch = if (uiState.searchQuery.isBlank()) true else {
            user.fullName.contains(uiState.searchQuery, ignoreCase = true) ||
            user.email.contains(uiState.searchQuery, ignoreCase = true) ||
            user.phone.contains(uiState.searchQuery, ignoreCase = true)
        }
        matchesRole && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(roles) { role ->
                val selected = uiState.userRoleFilter == role
                FilterChip(
                    selected = selected,
                    onClick = { onRoleFilterSelected(role) },
                    label = { Text(role, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LubricantAmber,
                        selectedLabelColor = EnolNavyDark,
                        containerColor = SurfaceDark,
                        labelColor = SlateSteelLight
                    )
                )
            }
        }

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No users found matching filter.",
                    color = SlateSteelLight,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
            ) {
                items(filteredUsers) { user ->
                    UserCard(
                        user = user,
                        onEdit = { onEditUser(user) },
                        onDeactivate = { onDeactivateUser(user.id, user.fullName) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserCard(
    user: UserDto,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit
) {
    val roleColor = when (user.role.uppercase()) {
        "DIRECTOR" -> LubricantAmber
        "ADMIN" -> LubricantAmberLight
        "ZSM" -> EnolNavyMuted
        "ASM" -> EnolNavyLight
        else -> SuccessEmerald
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateSteelDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteelLight
                    )
                }

                // Role Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = roleColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, roleColor)
                ) {
                    Text(
                        text = user.role,
                        color = roleColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Phone: ${user.phone}",
                    fontSize = 12.sp,
                    color = SlateSteelLight
                )
                Text(
                    text = "Vehicle: ${user.vehicleType} (${user.vehicleNumber ?: "N/A"})",
                    fontSize = 12.sp,
                    color = SlateSteelLight
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Daily Allowance: PKR ${user.dailyAllowance.toInt()}",
                    fontSize = 12.sp,
                    color = SlateSteelLight
                )
                Text(
                    text = if (user.isActive) "Status: Active" else "Status: Inactive",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (user.isActive) SuccessEmerald else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LubricantAmber),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.7f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Rights", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (user.isActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onDeactivate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Deactivate", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// PRODUCTS TAB
// ============================================================================

@Composable
fun ProductsTabContent(
    uiState: AdminManagementUiState,
    onCategoryFilterSelected: (String) -> Unit,
    onEditProduct: (ProductDto) -> Unit,
    onDeactivateProduct: (String, String) -> Unit
) {
    val categories = listOf(
        "ALL",
        "PASSENGER_CAR_MOTOR_OIL",
        "HEAVY_DUTY_DIESEL",
        "MOTORCYCLE_4T",
        "GEAR_OIL",
        "COOLANT",
        "GREASE"
    )

    val filteredProducts = uiState.products.filter { prod ->
        val matchesCat = if (uiState.productCategoryFilter == "ALL") true else prod.category.equals(uiState.productCategoryFilter, ignoreCase = true)
        val matchesSearch = if (uiState.searchQuery.isBlank()) true else {
            prod.sku.contains(uiState.searchQuery, ignoreCase = true) ||
            prod.name.contains(uiState.searchQuery, ignoreCase = true) ||
            prod.viscosityGrade.contains(uiState.searchQuery, ignoreCase = true)
        }
        matchesCat && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val selected = uiState.productCategoryFilter == cat
                val displayLabel = cat.replace("_", " ")
                FilterChip(
                    selected = selected,
                    onClick = { onCategoryFilterSelected(cat) },
                    label = { Text(displayLabel, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LubricantAmber,
                        selectedLabelColor = EnolNavyDark,
                        containerColor = SurfaceDark,
                        labelColor = SlateSteelLight
                    )
                )
            }
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No products found matching filter.",
                    color = SlateSteelLight,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductManagementCard(
                        product = product,
                        onEdit = { onEditProduct(product) },
                        onDeactivate = { onDeactivateProduct(product.id, product.sku) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductManagementCard(
    product: ProductDto,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateSteelDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = "SKU: ${product.sku} | Grade: ${product.viscosityGrade}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LubricantAmber
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (product.isActive) SuccessEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (product.isActive) SuccessEmerald else MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = if (product.isActive) "Active" else "Deactivated",
                        color = if (product.isActive) SuccessEmerald else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pack: ${product.packSizeLiters}L (${product.unitsPerCarton} units/ctn)",
                    fontSize = 12.sp,
                    color = SlateSteelLight
                )
                Text(
                    text = "Stock: ${product.stockQuantity} ctns",
                    fontSize = 12.sp,
                    color = SlateSteelLight
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Trade: PKR ${product.tradePrice.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                Text(
                    text = "Retail: PKR ${product.retailPrice.toInt()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LubricantAmber),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.7f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Price / Stock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (product.isActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onDeactivate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Deactivate", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// DIALOGS: ADD USER, EDIT USER, ADD PRODUCT, EDIT PRODUCT
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        email: String,
        phone: String,
        pwd: String,
        role: String,
        vehicleType: String,
        vehicleNumber: String,
        fuelRate: Double,
        dailyAllowance: Double
    ) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("Password123!") }
    var selectedRole by remember { mutableStateOf("SALES_REP") }
    var vehicleType by remember { mutableStateOf("MOTORCYCLE") }
    var vehicleNumber by remember { mutableStateOf("") }
    var fuelRate by remember { mutableStateOf("7.50") }
    var dailyAllowance by remember { mutableStateOf("500") }

    val roles = listOf("SALES_REP", "ASM", "ZSM", "DIRECTOR", "ADMIN")
    val vehicleTypes = listOf("MOTORCYCLE", "CAR", "VAN", "OTHER")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, SlateSteelDark, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add New User & Assign Rights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
                Text(
                    text = "Configure role, mobile access, vehicle rates and permissions",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Work Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Phone (+92...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Default Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Assign Role / Access Level:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(roles) { r ->
                        FilterChip(
                            selected = selectedRole == r,
                            onClick = { selectedRole = r },
                            label = { Text(r, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Vehicle Type:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(vehicleTypes) { v ->
                        FilterChip(
                            selected = vehicleType == v,
                            onClick = { vehicleType = v },
                            label = { Text(v, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it },
                    label = { Text("Vehicle Registration No (e.g. LEA-2024)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fuelRate,
                        onValueChange = { fuelRate = it },
                        label = { Text("Fuel PKR/km") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dailyAllowance,
                        onValueChange = { dailyAllowance = it },
                        label = { Text("Allowance PKR/day") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val fRate = fuelRate.toDoubleOrNull() ?: 7.50
                            val allow = dailyAllowance.toDoubleOrNull() ?: 500.0
                            onConfirm(fullName, email, phone, password, selectedRole, vehicleType, vehicleNumber, fRate, allow)
                        },
                        enabled = fullName.isNotBlank() && email.isNotBlank() && phone.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EnolNavyDark)
                        } else {
                            Text("Grant Rights & Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditUserRightsDialog(
    user: UserDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        role: String?,
        fullName: String?,
        phone: String?,
        vehicleType: String?,
        vehicleNumber: String?,
        fuelRate: Double?,
        dailyAllowance: Double?,
        isActive: Boolean?
    ) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.role) }
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var vehicleNumber by remember { mutableStateOf(user.vehicleNumber ?: "") }
    var fuelRate by remember { mutableStateOf(user.fuelRatePerKm.toString()) }
    var allowance by remember { mutableStateOf(user.dailyAllowance.toInt().toString()) }
    var isActive by remember { mutableStateOf(user.isActive) }

    val roles = listOf("SALES_REP", "ASM", "ZSM", "DIRECTOR", "ADMIN")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, SlateSteelDark, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit Rights: ${user.fullName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Assign Role / Hierarchy Level:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(roles) { r ->
                        FilterChip(
                            selected = selectedRole == r,
                            onClick = { selectedRole = r },
                            label = { Text(r, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it },
                    label = { Text("Vehicle Plate No") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fuelRate,
                        onValueChange = { fuelRate = it },
                        label = { Text("Fuel PKR/km") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = allowance,
                        onValueChange = { allowance = it },
                        label = { Text("Allowance PKR/day") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Account Active Status:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = LubricantAmber, checkedTrackColor = LubricantAmber.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val fRate = fuelRate.toDoubleOrNull()
                            val allow = allowance.toDoubleOrNull()
                            onConfirm(selectedRole, fullName, phone, null, vehicleNumber, fRate, allow, isActive)
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EnolNavyDark)
                        } else {
                            Text("Save Rights", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
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
    ) -> Unit
) {
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("PASSENGER_CAR_MOTOR_OIL") }
    var viscosityGrade by remember { mutableStateOf("5W-30") }
    var packSizeLiters by remember { mutableStateOf("4.0") }
    var unitsPerCarton by remember { mutableStateOf("4") }
    var tradePrice by remember { mutableStateOf("4500") }
    var retailPrice by remember { mutableStateOf("5200") }
    var stockQuantity by remember { mutableStateOf("500") }

    val categories = listOf(
        "PASSENGER_CAR_MOTOR_OIL",
        "HEAVY_DUTY_DIESEL",
        "MOTORCYCLE_4T",
        "GEAR_OIL",
        "COOLANT",
        "GREASE"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, SlateSteelDark, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add Lubricant Product SKU",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
                Text(
                    text = "Configure automotive grade, packing, trade & retail pricing",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU (e.g. ENOL-PCMO-5W30-4L)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name (e.g. ENOL Synth-Max 5W-30)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.replace("_", " "), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viscosityGrade,
                        onValueChange = { viscosityGrade = it },
                        label = { Text("Viscosity Grade") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = packSizeLiters,
                        onValueChange = { packSizeLiters = it },
                        label = { Text("Pack Size (L)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tradePrice,
                        onValueChange = { tradePrice = it },
                        label = { Text("Trade Price (PKR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = retailPrice,
                        onValueChange = { retailPrice = it },
                        label = { Text("Retail Price (PKR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitsPerCarton,
                        onValueChange = { unitsPerCarton = it },
                        label = { Text("Units / Carton") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Initial Stock") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val packL = packSizeLiters.toDoubleOrNull() ?: 4.0
                            val uCtn = unitsPerCarton.toIntOrNull() ?: 4
                            val tPrice = tradePrice.toDoubleOrNull() ?: 4500.0
                            val rPrice = retailPrice.toDoubleOrNull() ?: 5200.0
                            val stock = stockQuantity.toIntOrNull() ?: 500
                            onConfirm(sku, name, description, category, viscosityGrade, packL, uCtn, tPrice, rPrice, stock)
                        },
                        enabled = sku.isNotBlank() && name.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EnolNavyDark)
                        } else {
                            Text("Add to Catalog", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditProductDialog(
    product: ProductDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String?,
        tradePrice: Double?,
        retailPrice: Double?,
        stockQuantity: Int?,
        isActive: Boolean?
    ) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var tradePrice by remember { mutableStateOf(product.tradePrice.toInt().toString()) }
    var retailPrice by remember { mutableStateOf(product.retailPrice.toInt().toString()) }
    var stockQuantity by remember { mutableStateOf(product.stockQuantity.toString()) }
    var isActive by remember { mutableStateOf(product.isActive) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, SlateSteelDark, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Edit Product: ${product.sku}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
                Text(
                    text = "Grade: ${product.viscosityGrade} | Pack: ${product.packSizeLiters}L",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tradePrice,
                        onValueChange = { tradePrice = it },
                        label = { Text("Trade Price (PKR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = retailPrice,
                        onValueChange = { retailPrice = it },
                        label = { Text("Retail Price (PKR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stockQuantity,
                    onValueChange = { stockQuantity = it },
                    label = { Text("Available Stock (Cartons)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Product Active in Catalog:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = LubricantAmber, checkedTrackColor = LubricantAmber.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val tPrice = tradePrice.toDoubleOrNull()
                            val rPrice = retailPrice.toDoubleOrNull()
                            val stock = stockQuantity.toIntOrNull()
                            onConfirm(name, tPrice, rPrice, stock, isActive)
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EnolNavyDark)
                        } else {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
