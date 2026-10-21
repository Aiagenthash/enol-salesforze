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

@Composable
fun AdminManagementScreen(
    viewModel: AdminManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sleek 3-Tab Header (Single Spot Color)
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = SurfaceDark,
                contentColor = LubricantAmber,
                divider = { HorizontalDivider(color = SlateSteelDark) }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            text = "Users (${uiState.users.size})",
                            fontSize = 13.sp,
                            fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.selectedTab == 0) LubricantAmber else SlateSteelLight
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            text = "Products (${uiState.products.size})",
                            fontSize = 13.sp,
                            fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.selectedTab == 1) LubricantAmber else SlateSteelLight
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = {
                        Text(
                            text = "Territories (${uiState.districts.size})",
                            fontSize = 13.sp,
                            fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (uiState.selectedTab == 2) LubricantAmber else SlateSteelLight
                        )
                    }
                )
            }

            // Message Banner (Clean single line)
            if (uiState.successMessage != null) {
                Surface(
                    color = SurfaceContainerDark,
                    border = BorderStroke(1.dp, LubricantAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LubricantAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.successMessage ?: "",
                            color = OnSurfaceDark,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SlateSteelLight, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Surface(
                    color = ErrorCrimsonContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, ErrorCrimson),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorCrimson, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = ErrorCrimson,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(18.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SlateSteelLight, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Compact Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        when (uiState.selectedTab) {
                            0 -> "Search users by name, email, phone..."
                            1 -> "Search products by SKU or name..."
                            else -> "Search districts or routes..."
                        },
                        fontSize = 13.sp,
                        color = SlateSteel
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateSteelLight, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = SlateSteelLight, modifier = Modifier.size(16.dp))
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
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(48.dp)
            )

            // Content per Tab
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LubricantAmber, modifier = Modifier.size(28.dp))
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

        // Floating Action Button (Spot Color, Single Bottom Right)
        FloatingActionButton(
            onClick = {
                when (uiState.selectedTab) {
                    0 -> viewModel.openAddUserDialog()
                    1 -> viewModel.openAddProductDialog()
                    2 -> viewModel.openCreateBeatDialog()
                }
            },
            containerColor = LubricantAmber,
            contentColor = EnolNavyDark,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
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
// USERS TAB - Clean Minimalist Cards
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
        // Filter Chips (Monochrome + Spot Color)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(roles) { role ->
                val selected = uiState.userRoleFilter == role
                FilterChip(
                    selected = selected,
                    onClick = { onRoleFilterSelected(role) },
                    label = { Text(role, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LubricantAmber,
                        selectedLabelColor = EnolNavyDark,
                        containerColor = SurfaceDark,
                        labelColor = SlateSteelLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) LubricantAmber else SlateSteelDark
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
                Text("No users found", color = SlateSteelLight, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
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
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateSteelDark, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        text = "${user.email} • ${user.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteelLight
                    )
                }

                // Minimalist Spot Color Role Pill
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, LubricantAmber)
                ) {
                    Text(
                        text = user.role,
                        color = LubricantAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reports to: ${managerName ?: "Top Level"}",
                    fontSize = 11.sp,
                    color = SlateSteelLight
                )
                Text(
                    text = if (user.isActive) "Active" else "Inactive",
                    fontSize = 11.sp,
                    color = if (user.isActive) OnSurfaceDark else SlateSteel
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LubricantAmber),
                    border = BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Edit Rights", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (user.isActive && !user.role.equals("DIRECTOR", ignoreCase = true)) {
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onDeactivate,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateSteelLight),
                        border = BorderStroke(1.dp, SlateSteelDark),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Deactivate", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// PRODUCTS TAB - Clean Minimalist Catalog
// ============================================================================

@Composable
fun ProductsTabContent(
    uiState: AdminManagementUiState,
    onCategoryFilterSelected: (String) -> Unit,
    onEditProduct: (ProductDto) -> Unit,
    onDeactivateProduct: (String, String) -> Unit
) {
    val categories = listOf("ALL", "PASSENGER_CAR_MOTOR_OIL", "HEAVY_DUTY_DIESEL", "MOTORCYCLE_4T", "GEAR_OIL", "COOLANT", "GREASE")

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
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { cat ->
                val selected = uiState.productCategoryFilter == cat
                val displayLabel = if (cat == "ALL") "ALL" else cat.replace("_", " ").take(12)
                FilterChip(
                    selected = selected,
                    onClick = { onCategoryFilterSelected(cat) },
                    label = { Text(displayLabel, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LubricantAmber,
                        selectedLabelColor = EnolNavyDark,
                        containerColor = SurfaceDark,
                        labelColor = SlateSteelLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) LubricantAmber else SlateSteelDark
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
                Text("No products found", color = SlateSteelLight, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
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
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateSteelDark, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        text = "SKU: ${product.sku} • Grade: ${product.viscosityGrade} • ${product.packSizeLiters}L",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteelLight
                    )
                }

                Text(
                    text = "PKR ${product.tradePrice.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = LubricantAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stock: ${product.stockQuantity} ctns",
                    fontSize = 11.sp,
                    color = SlateSteelLight
                )
                Text(
                    text = "Retail: PKR ${product.retailPrice.toInt()}",
                    fontSize = 11.sp,
                    color = SlateSteelLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LubricantAmber),
                    border = BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Edit Price / Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (product.isActive) {
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onDeactivate,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateSteelLight),
                        border = BorderStroke(1.dp, SlateSteelDark),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Deactivate", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ============================================================================
// TERRITORIES TAB - Clean Minimalist Routes
// ============================================================================

@Composable
fun TerritoriesTabContent(
    uiState: AdminManagementUiState,
    onDistrictFilterSelected: (String) -> Unit,
    onAssignManagers: (DistrictDto) -> Unit,
    onAssignRep: (BeatDto) -> Unit,
    onCreateBeatForDistrict: (DistrictDto) -> Unit
) {
    val filteredDistricts = uiState.districts.filter { dist ->
        val matchesFilter = if (uiState.districtFilter == "ALL") true else dist.id == uiState.districtFilter
        val matchesSearch = if (uiState.searchQuery.isBlank()) true else {
            dist.name.contains(uiState.searchQuery, ignoreCase = true) ||
            dist.code.contains(uiState.searchQuery, ignoreCase = true) ||
            uiState.beats.any { b ->
                b.districtId == dist.id && b.name.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
        matchesFilter && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.districtFilter == "ALL",
                    onClick = { onDistrictFilterSelected("ALL") },
                    label = { Text("ALL", fontSize = 11.sp, fontWeight = if (uiState.districtFilter == "ALL") FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LubricantAmber,
                        selectedLabelColor = EnolNavyDark,
                        containerColor = SurfaceDark,
                        labelColor = SlateSteelLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.districtFilter == "ALL",
                        borderColor = if (uiState.districtFilter == "ALL") LubricantAmber else SlateSteelDark
                    )
                )
            }
            items(uiState.districts) { d ->
                val selected = uiState.districtFilter == d.id
                FilterChip(
                    selected = selected,
                    onClick = { onDistrictFilterSelected(d.id) },
                    label = { Text(d.name, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LubricantAmber,
                        selectedLabelColor = EnolNavyDark,
                        containerColor = SurfaceDark,
                        labelColor = SlateSteelLight
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = if (selected) LubricantAmber else SlateSteelDark
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
                Text("No territories found", color = SlateSteelLight, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
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
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateSteelDark, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${district.name} (${district.code})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = "${district.province} • ${beats.size} Routes",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteelLight
                    )
                }

                OutlinedButton(
                    onClick = onAssignManagers,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LubricantAmber),
                    border = BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Assign Mgrs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Minimalist Manager Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ZSM: ${district.zsmName ?: "Unassigned"}",
                    fontSize = 11.sp,
                    color = SlateSteelLight
                )
                Text(
                    text = "ASM: ${district.asmName ?: "Unassigned"}",
                    fontSize = 11.sp,
                    color = SlateSteelLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Beat Routes Sub-list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Routes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )

                Text(
                    text = "+ Add Route",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber,
                    modifier = Modifier.clickable { onCreateBeat() }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (beats.isEmpty()) {
                Text(
                    text = "No routes yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteel
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    beats.forEach { beat ->
                        Surface(
                            color = BackgroundDark,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, SlateSteelDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${beat.name} (${beat.dayOfWeek.take(3)})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = OnSurfaceDark
                                    )
                                    Text(
                                        text = "Rep: ${beat.assignedRepName ?: "Unassigned"}",
                                        fontSize = 11.sp,
                                        color = SlateSteelLight
                                    )
                                }

                                Text(
                                    text = "Assign",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LubricantAmber,
                                    modifier = Modifier.clickable { onAssignRep(beat) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMPACT DIALOGS (Single Spot Color)
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
                    text = "Add User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (+92...)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Default Password") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Role:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
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

                Text("Reporting Manager:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedManagerId == null,
                            onClick = { selectedManagerId = null },
                            label = { Text("None", fontSize = 11.sp) },
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

                Spacer(modifier = Modifier.height(16.dp))

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
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save User", fontWeight = FontWeight.Bold)
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
                    color = OnSurfaceDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Role:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
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

                Text("Reporting Manager:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedManagerId == null,
                            onClick = { selectedManagerId = null },
                            label = { Text("None", fontSize = 11.sp) },
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Account Active", fontSize = 13.sp, color = OnSurfaceDark)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = LubricantAmber, checkedTrackColor = LubricantAmber.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(selectedRole, fullName, phone, selectedManagerId, null, null, null, null, isActive)
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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
    var category by remember { mutableStateOf("PASSENGER_CAR_MOTOR_OIL") }
    var viscosityGrade by remember { mutableStateOf("5W-30") }
    var packSizeLiters by remember { mutableStateOf("4.0") }
    var unitsPerCarton by remember { mutableStateOf("4") }
    var tradePrice by remember { mutableStateOf("4500") }
    var retailPrice by remember { mutableStateOf("5200") }
    var stockQuantity by remember { mutableStateOf("500") }

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
                    text = "Add Product",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU Code") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viscosityGrade,
                        onValueChange = { viscosityGrade = it },
                        label = { Text("Grade") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = packSizeLiters,
                        onValueChange = { packSizeLiters = it },
                        label = { Text("Pack (L)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tradePrice,
                        onValueChange = { tradePrice = it },
                        label = { Text("Trade PKR") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = retailPrice,
                        onValueChange = { retailPrice = it },
                        label = { Text("Retail PKR") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            onConfirm(sku, name, "", category, viscosityGrade, pack, units, tPrice, rPrice, stock)
                        },
                        enabled = sku.isNotBlank() && name.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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
                    text = "Edit: ${product.sku}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tradePrice,
                        onValueChange = { tradePrice = it },
                        label = { Text("Trade PKR") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = retailPrice,
                        onValueChange = { retailPrice = it },
                        label = { Text("Retail PKR") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = stockQuantity,
                    onValueChange = { stockQuantity = it },
                    label = { Text("Stock (Cartons)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
                    text = "Assign Managers: ${district.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("ZSM Manager:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedZsmId == null,
                            onClick = { selectedZsmId = null },
                            label = { Text("None", fontSize = 11.sp) },
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
                            label = { Text(zsm.fullName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("ASM Manager:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedAsmId == null,
                            onClick = { selectedAsmId = null },
                            label = { Text("None", fontSize = 11.sp) },
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
                            label = { Text(asm.fullName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedZsmId, selectedAsmId) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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

    val daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
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
                    text = "Assign Rep: ${beat.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Sales Rep:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedRepId == null,
                            onClick = { selectedRepId = null },
                            label = { Text("None", fontSize = 11.sp) },
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
                            label = { Text(rep.fullName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Day of Week:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(daysOfWeek) { day ->
                        FilterChip(
                            selected = selectedDay.equals(day, ignoreCase = true),
                            onClick = { selectedDay = day },
                            label = { Text(day.take(3), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedRepId, selectedDay) },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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

    val daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
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
                    color = OnSurfaceDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("District:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(districts) { dist ->
                        FilterChip(
                            selected = selectedDistrictId == dist.id,
                            onClick = { selectedDistrictId = dist.id },
                            label = { Text(dist.name, fontSize = 11.sp) },
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
                    label = { Text("Route Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Route Code (e.g. LHR-02)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LubricantAmber, unfocusedBorderColor = SlateSteelDark),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Day:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(daysOfWeek) { day ->
                        FilterChip(
                            selected = selectedDay == day,
                            onClick = { selectedDay = day },
                            label = { Text(day.take(3), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Assigned Rep:", fontSize = 11.sp, color = SlateSteelLight)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedRepId == null,
                            onClick = { selectedRepId = null },
                            label = { Text("None", fontSize = 11.sp) },
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
                            label = { Text(rep.fullName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LubricantAmber,
                                selectedLabelColor = EnolNavyDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SlateSteelLight)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedDistrictId, name, code, selectedDay, selectedRepId) },
                        enabled = name.isNotBlank() && code.isNotBlank() && selectedDistrictId.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavyDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Route", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
