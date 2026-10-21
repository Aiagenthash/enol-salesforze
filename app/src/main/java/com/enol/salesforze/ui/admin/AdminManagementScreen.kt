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
import com.enol.salesforze.data.remote.models.BeatDto
import com.enol.salesforze.data.remote.models.DistrictDto
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
        "Products (${uiState.products.size})",
        "Territories & Beats (${uiState.districts.size})"
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
                            text = "Configure Users, Assign Rights & Territories",
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
            when (uiState.selectedTab) {
                0 -> {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openAddUserDialog() },
                        containerColor = LubricantAmber,
                        contentColor = EnolNavyDark,
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                        text = { Text("Add User", fontWeight = FontWeight.Bold) }
                    )
                }
                1 -> {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openAddProductDialog() },
                        containerColor = LubricantAmber,
                        contentColor = EnolNavyDark,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Add Product", fontWeight = FontWeight.Bold) }
                    )
                }
                2 -> {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openCreateBeatDialog() },
                        containerColor = LubricantAmber,
                        contentColor = EnolNavyDark,
                        icon = { Icon(Icons.Default.AddLocation, contentDescription = null) },
                        text = { Text("Add Beat Route", fontWeight = FontWeight.Bold) }
                    )
                }
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
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = SurfaceDark,
                contentColor = LubricantAmber,
                divider = { HorizontalDivider(color = SlateSteelDark) },
                edgePadding = 12.dp
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
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
                        when (uiState.selectedTab) {
                            0 -> "Search users by name, email, or phone..."
                            1 -> "Search products by SKU or name..."
                            else -> "Search districts or beat routes..."
                        }
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
            } else {
                when (uiState.selectedTab) {
                    0 -> UsersTabContent(
                        uiState = uiState,
                        onRoleFilterSelected = { viewModel.setUserRoleFilter(it) },
                        onEditUser = { viewModel.openEditUserDialog(it) },
                        onDeactivateUser = { id, name -> viewModel.deactivateUser(id, name) }
                    )
                    1 -> ProductsTabContent(
                        uiState = uiState,
                        onCategoryFilterSelected = { viewModel.setProductCategoryFilter(it) },
                        onEditProduct = { viewModel.openEditProductDialog(it) },
                        onDeactivateProduct = { id, sku -> viewModel.deactivateProduct(id, sku) }
                    )
                    2 -> TerritoriesTabContent(
                        uiState = uiState,
                        onDistrictFilterSelected = { viewModel.setDistrictFilter(it) },
                        onAssignManagers = { viewModel.openAssignDistrictManagersDialog(it) },
                        onAssignRep = { viewModel.openAssignBeatRepDialog(it) },
                        onCreateBeatForDistrict = { viewModel.openCreateBeatDialog(it) }
                    )
                }
            }
        }
    }

    // Add User Dialog
    if (uiState.showAddUserDialog) {
        AddUserDialog(
            users = uiState.users,
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeAddUserDialog() },
            onConfirm = { name, email, phone, pwd, role, mgrId, vType, vNum, fuelRate, allowance ->
                viewModel.createUser(name, email, phone, pwd, role, mgrId, vType, vNum, fuelRate, allowance)
            }
        )
    }

    // Edit User Dialog
    if (uiState.editingUser != null) {
        val user = uiState.editingUser!!
        EditUserRightsDialog(
            user = user,
            users = uiState.users,
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeEditUserDialog() },
            onConfirm = { role, name, phone, mgrId, vType, vNum, fuel, allow, active ->
                viewModel.updateUserRights(user.id, role, name, phone, mgrId, vType, vNum, fuel, allow, active)
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

    // Assign District Managers Dialog
    if (uiState.editingDistrict != null) {
        val dist = uiState.editingDistrict!!
        AssignDistrictManagersDialog(
            district = dist,
            users = uiState.users,
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeAssignDistrictManagersDialog() },
            onConfirm = { zsmId, asmId ->
                viewModel.assignDistrictManagers(dist.id, zsmId, asmId)
            }
        )
    }

    // Assign Beat Rep Dialog
    if (uiState.editingBeat != null) {
        val beat = uiState.editingBeat!!
        AssignBeatRepDialog(
            beat = beat,
            users = uiState.users,
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeAssignBeatRepDialog() },
            onConfirm = { repId, dayOfWeek ->
                viewModel.assignBeatRep(beat.id, repId, dayOfWeek)
            }
        )
    }

    // Create Beat Dialog
    if (uiState.showCreateBeatDialog) {
        CreateBeatDialog(
            districts = uiState.districts,
            initialDistrict = uiState.targetDistrictForNewBeat,
            users = uiState.users,
            isLoading = uiState.isActionLoading,
            onDismiss = { viewModel.closeCreateBeatDialog() },
            onConfirm = { districtId, name, code, dayOfWeek, assignedRepId ->
                viewModel.createBeat(districtId, name, code, dayOfWeek, assignedRepId)
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
                    val managerName = remember(user.managerId, uiState.users) {
                        user.managerId?.let { id -> uiState.users.find { it.id == id }?.fullName }
                    }
                    UserCard(
                        user = user,
                        managerName = managerName,
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
    managerName: String?,
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
                    border = BorderStroke(1.dp, roleColor)
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
                    text = "Reporting Mgr: ${managerName ?: "Top Level"}",
                    fontSize = 12.sp,
                    color = if (managerName != null) LubricantAmber else SlateSteelLight
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
                    border = BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.7f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Rights", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (user.isActive && !user.role.equals("DIRECTOR", ignoreCase = true)) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onDeactivate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
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
                    border = BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.7f)),
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
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
// TERRITORIES & BEAT ROUTES TAB
// ============================================================================

@Composable
fun TerritoriesTabContent(
    uiState: AdminManagementUiState,
    onDistrictFilterSelected: (String) -> Unit,
    onAssignManagers: (DistrictDto) -> Unit,
    onAssignRep: (BeatDto) -> Unit,
    onCreateBeatForDistrict: (DistrictDto) -> Unit
) {
    val districtOptions = listOf("ALL") + uiState.districts.map { it.id }

    val filteredDistricts = uiState.districts.filter { dist ->
        val matchesFilter = if (uiState.districtFilter == "ALL") true else dist.id == uiState.districtFilter
        val matchesSearch = if (uiState.searchQuery.isBlank()) true else {
            dist.name.contains(uiState.searchQuery, ignoreCase = true) ||
            dist.code.contains(uiState.searchQuery, ignoreCase = true) ||
            dist.province.contains(uiState.searchQuery, ignoreCase = true) ||
            uiState.beats.any { b ->
                b.districtId == dist.id && (
                    b.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    b.code.contains(uiState.searchQuery, ignoreCase = true) ||
                    (b.assignedRepName?.contains(uiState.searchQuery, ignoreCase = true) == true)
                )
            }
        }
        matchesFilter && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // District Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.districtFilter == "ALL",
                    onClick = { onDistrictFilterSelected("ALL") },
                    label = { Text("ALL DISTRICTS", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LubricantAmber,
                        selectedLabelColor = EnolNavyDark,
                        containerColor = SurfaceDark,
                        labelColor = SlateSteelLight
                    )
                )
            }
            items(uiState.districts) { d ->
                FilterChip(
                    selected = uiState.districtFilter == d.id,
                    onClick = { onDistrictFilterSelected(d.id) },
                    label = { Text(d.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LubricantAmber,
                        selectedLabelColor = EnolNavyDark,
                        containerColor = SurfaceDark,
                        labelColor = SlateSteelLight
                    )
                )
            }
        }

        if (filteredDistricts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No territories found matching criteria.",
                    color = SlateSteelLight,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
            ) {
                items(filteredDistricts) { district ->
                    val districtBeats = uiState.beats.filter { it.districtId == district.id }
                    DistrictCard(
                        district = district,
                        beats = districtBeats,
                        onAssignManagers = { onAssignManagers(district) },
                        onAssignRep = onAssignRep,
                        onCreateBeat = { onCreateBeatForDistrict(district) }
                    )
                }
            }
        }
    }
}

@Composable
fun DistrictCard(
    district: DistrictDto,
    beats: List<BeatDto>,
    onAssignManagers: () -> Unit,
    onAssignRep: (BeatDto) -> Unit,
    onCreateBeat: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateSteelDark, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = district.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = "Code: ${district.code} • ${district.province} • ${beats.size} Routes",
                        style = MaterialTheme.typography.bodySmall,
                        color = LubricantAmber
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (district.isActive) SuccessEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (district.isActive) SuccessEmerald else MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = if (district.isActive) "Active" else "Inactive",
                        color = if (district.isActive) SuccessEmerald else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Assigned Managers Section
            Surface(
                color = SurfaceContainerHighDark.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ZSM Manager:", fontSize = 11.sp, color = SlateSteelLight)
                            Text(
                                text = district.zsmName ?: "Unassigned",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (district.zsmName != null) OnSurfaceDark else MaterialTheme.colorScheme.error
                            )
                        }
                        Column {
                            Text("ASM Manager:", fontSize = 11.sp, color = SlateSteelLight)
                            Text(
                                text = district.asmName ?: "Unassigned",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (district.asmName != null) OnSurfaceDark else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onAssignManagers,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Assign Managers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Beats Header & Add Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Beat Routes (${beats.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )

                TextButton(
                    onClick = onCreateBeat,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = LubricantAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+ Add Route", fontSize = 11.sp, color = LubricantAmber, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (beats.isEmpty()) {
                Text(
                    text = "No beat routes configured for this district yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    beats.forEach { beat ->
                        BeatItemRow(
                            beat = beat,
                            onAssignRep = { onAssignRep(beat) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BeatItemRow(
    beat: BeatDto,
    onAssignRep: () -> Unit
) {
    Surface(
        color = BackgroundDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SlateSteelDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = beat.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LubricantAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = beat.dayOfWeek,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LubricantAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Assigned Rep: ",
                        fontSize = 11.sp,
                        color = SlateSteelLight
                    )
                    Text(
                        text = beat.assignedRepName ?: "Unassigned",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (beat.assignedRepName != null) SuccessEmerald else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = " • ${beat.totalClients} Shops",
                        fontSize = 11.sp,
                        color = SlateSteelLight
                    )
                }
            }

            OutlinedButton(
                onClick = onAssignRep,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LubricantAmber),
                border = BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Assign Rep", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============================================================================
// DIALOGS: ADD USER, EDIT USER, ADD PRODUCT, EDIT PRODUCT
// ============================================================================

@Composable
fun AddUserDialog(
    users: List<UserDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        email: String,
        phone: String,
        pwd: String,
        role: String,
        managerId: String?,
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
    var selectedManagerId by remember { mutableStateOf<String?>(null) }
    var vehicleType by remember { mutableStateOf("MOTORCYCLE") }
    var vehicleNumber by remember { mutableStateOf("") }
    var fuelRate by remember { mutableStateOf("7.50") }
    var dailyAllowance by remember { mutableStateOf("500") }

    val roles = listOf("SALES_REP", "ASM", "ZSM", "DIRECTOR", "ADMIN")
    val vehicleTypes = listOf("MOTORCYCLE", "CAR", "VAN", "OTHER")

    val eligibleManagers = remember(users) {
        users.filter { it.role in listOf("DIRECTOR", "ZSM", "ASM", "ADMIN") }
    }

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
                    text = "Configure role, mobile access, manager and vehicle rates",
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

                Text("Reporting Manager (Hierarchy):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedManagerId == null,
                            onClick = { selectedManagerId = null },
                            label = { Text("None (Top Level)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                    items(eligibleManagers) { mgr ->
                        FilterChip(
                            selected = selectedManagerId == mgr.id,
                            onClick = { selectedManagerId = mgr.id },
                            label = { Text("${mgr.fullName} (${mgr.role})", fontSize = 11.sp) },
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
                            onConfirm(fullName, email, phone, password, selectedRole, selectedManagerId, vehicleType, vehicleNumber, fRate, allow)
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
    users: List<UserDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        role: String?,
        fullName: String?,
        phone: String?,
        managerId: String?,
        vehicleType: String?,
        vehicleNumber: String?,
        fuelRate: Double?,
        dailyAllowance: Double?,
        isActive: Boolean?
    ) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.role) }
    var selectedManagerId by remember { mutableStateOf<String?>(user.managerId) }
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var vehicleNumber by remember { mutableStateOf(user.vehicleNumber ?: "") }
    var fuelRate by remember { mutableStateOf(user.fuelRatePerKm.toString()) }
    var allowance by remember { mutableStateOf(user.dailyAllowance.toInt().toString()) }
    var isActive by remember { mutableStateOf(user.isActive) }

    val roles = listOf("SALES_REP", "ASM", "ZSM", "DIRECTOR", "ADMIN")
    val eligibleManagers = remember(users, user.id) {
        users.filter { it.id != user.id && it.role in listOf("DIRECTOR", "ZSM", "ASM", "ADMIN") }
    }

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

                Text("Reporting Manager:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedManagerId == null,
                            onClick = { selectedManagerId = null },
                            label = { Text("None (Top Level)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                    items(eligibleManagers) { mgr ->
                        FilterChip(
                            selected = selectedManagerId == mgr.id,
                            onClick = { selectedManagerId = mgr.id },
                            label = { Text("${mgr.fullName} (${mgr.role})", fontSize = 11.sp) },
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
                            onConfirm(selectedRole, fullName, phone, selectedManagerId, null, vehicleNumber, fRate, allow, isActive)
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
                    text = "Add Lubricant SKU to Catalog",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
                Text(
                    text = "Define SKU, grade, packing, trade & retail prices",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU Code (e.g. ENOL-PCMO-5W30-4L)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Commercial Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Performance Standards") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Product Category:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.replace("_", " "), fontSize = 10.sp) },
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
                        value = unitsPerCarton,
                        onValueChange = { unitsPerCarton = it },
                        label = { Text("Units / Carton") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Initial Stock (Ctns)") },
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

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val pack = packSizeLiters.toDoubleOrNull() ?: 4.0
                            val units = unitsPerCarton.toIntOrNull() ?: 4
                            val tPrice = tradePrice.toDoubleOrNull() ?: 4500.0
                            val rPrice = retailPrice.toDoubleOrNull() ?: 5200.0
                            val stock = stockQuantity.toIntOrNull() ?: 500
                            onConfirm(sku, name, description, category, viscosityGrade, pack, units, tPrice, rPrice, stock)
                        },
                        enabled = sku.isNotBlank() && name.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EnolNavyDark)
                        } else {
                            Text("Add SKU to Catalog", fontWeight = FontWeight.Bold)
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

// ============================================================================
// TERRITORY DIALOGS: ASSIGN MANAGERS, ASSIGN REP, CREATE BEAT
// ============================================================================

@Composable
fun AssignDistrictManagersDialog(
    district: DistrictDto,
    users: List<UserDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (zsmId: String?, asmId: String?) -> Unit
) {
    var selectedZsmId by remember { mutableStateOf<String?>(district.assignedZsmId) }
    var selectedAsmId by remember { mutableStateOf<String?>(district.assignedAsmId) }

    val zsmCandidates = remember(users) {
        users.filter { it.role in listOf("ZSM", "DIRECTOR", "ADMIN") }
    }
    val asmCandidates = remember(users) {
        users.filter { it.role in listOf("ASM", "ZSM") }
    }

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
                    text = "Assign District Managers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
                Text(
                    text = "District: ${district.name} (${district.code})",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Zonal Sales Manager (ZSM):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedZsmId == null,
                            onClick = { selectedZsmId = null },
                            label = { Text("None (Unassigned)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                    items(zsmCandidates) { zsm ->
                        FilterChip(
                            selected = selectedZsmId == zsm.id,
                            onClick = { selectedZsmId = zsm.id },
                            label = { Text("${zsm.fullName} (${zsm.role})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Area Sales Manager (ASM):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAsmId == null,
                            onClick = { selectedAsmId = null },
                            label = { Text("None (Unassigned)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                    items(asmCandidates) { asm ->
                        FilterChip(
                            selected = selectedAsmId == asm.id,
                            onClick = { selectedAsmId = asm.id },
                            label = { Text("${asm.fullName} (${asm.role})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedZsmId, selectedAsmId) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EnolNavyDark)
                        } else {
                            Text("Save Assignments", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AssignBeatRepDialog(
    beat: BeatDto,
    users: List<UserDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (repId: String?, dayOfWeek: String) -> Unit
) {
    var selectedRepId by remember { mutableStateOf<String?>(beat.assignedRepId) }
    var selectedDay by remember { mutableStateOf(beat.dayOfWeek) }

    val daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val repCandidates = remember(users) {
        users.filter { it.role in listOf("SALES_REP", "ASM") }
    }

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
                    text = "Assign Beat Route Rep",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
                Text(
                    text = "Route: ${beat.name} (${beat.code})",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Assigned Sales Staff / Rep:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedRepId == null,
                            onClick = { selectedRepId = null },
                            label = { Text("None (Unassigned)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                    items(repCandidates) { rep ->
                        FilterChip(
                            selected = selectedRepId == rep.id,
                            onClick = { selectedRepId = rep.id },
                            label = { Text("${rep.fullName} (${rep.role})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Scheduled Day of Week:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(daysOfWeek) { day ->
                        FilterChip(
                            selected = selectedDay.equals(day, ignoreCase = true),
                            onClick = { selectedDay = day },
                            label = { Text(day, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedRepId, selectedDay) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EnolNavyDark)
                        } else {
                            Text("Save Assignment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateBeatDialog(
    districts: List<DistrictDto>,
    initialDistrict: DistrictDto?,
    users: List<UserDto>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        districtId: String,
        name: String,
        code: String,
        dayOfWeek: String,
        assignedRepId: String?
    ) -> Unit
) {
    var selectedDistrictId by remember {
        mutableStateOf(initialDistrict?.id ?: districts.firstOrNull()?.id ?: "")
    }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf("MONDAY") }
    var selectedRepId by remember { mutableStateOf<String?>(null) }

    val daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val repCandidates = remember(users) {
        users.filter { it.role in listOf("SALES_REP", "ASM") }
    }

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
                    text = "Add Beat Route",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber
                )
                Text(
                    text = "Create new customer beat route and assign staff",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("Select District:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(districts) { dist ->
                        FilterChip(
                            selected = selectedDistrictId == dist.id,
                            onClick = { selectedDistrictId = dist.id },
                            label = { Text("${dist.name} (${dist.code})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Route Name (e.g. Badami Bagh Route B)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Route Code (e.g. LHR-BB-02)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Visit Day of Week:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(daysOfWeek) { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Assign Sales Rep:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSurfaceDark)
                LazyRow(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedRepId == null,
                            onClick = { selectedRepId = null },
                            label = { Text("None (Assign Later)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                    items(repCandidates) { rep ->
                        FilterChip(
                            selected = selectedRepId == rep.id,
                            onClick = { selectedRepId = rep.id },
                            label = { Text("${rep.fullName} (${rep.role})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedDistrictId, name, code, selectedDay, selectedRepId) },
                        enabled = name.isNotBlank() && code.isNotBlank() && selectedDistrictId.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EnolNavyDark)
                        } else {
                            Text("Create Beat Route", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
