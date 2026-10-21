package com.enol.salesforze.ui.order

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enol.salesforze.data.local.entities.ProductEntity
import com.enol.salesforze.ui.theme.EnolNavy
import com.enol.salesforze.ui.theme.EnolNavyLight
import com.enol.salesforze.ui.theme.ErrorCrimson
import com.enol.salesforze.ui.theme.LubricantAmber
import com.enol.salesforze.ui.theme.SlateSteel
import com.enol.salesforze.ui.theme.SlateSteelLight
import com.enol.salesforze.ui.theme.SuccessEmerald
import com.enol.salesforze.ui.theme.WarningAmber
import com.enol.salesforze.ui.theme.elevation_2
import com.enol.salesforze.ui.theme.elevation_4
import com.enol.salesforze.ui.theme.grid_0_5
import com.enol.salesforze.ui.theme.grid_1
import com.enol.salesforze.ui.theme.grid_2
import com.enol.salesforze.ui.theme.grid_3
import com.enol.salesforze.ui.theme.grid_4
import com.enol.salesforze.ui.theme.grid_6

/**
 * Built-In Multi-SKU Order Booking Cart & Credit Guardrail Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCartScreen(
    modifier: Modifier = Modifier,
    viewModel: OrderCartViewModel = hiltViewModel(),
    onOrderPlaced: (String) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var isCartExpanded by remember { mutableStateOf(false) }

    // If order is successfully submitted, navigate or show success confirmation
    if (uiState.orderSubmittedSuccess && uiState.createdOrderId != null) {
        OrderSuccessDialog(
            orderId = uiState.createdOrderId ?: "",
            isCreditHold = uiState.exceedsCreditLimit,
            onDismiss = { onOrderPlaced(uiState.createdOrderId ?: "") }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.client?.businessName ?: "Order Booking Cart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        uiState.client?.let { client ->
                            Text(
                                text = "Credit Limit: PKR %,.0f  |  Bal: PKR %,.0f".format(client.creditLimit, client.outstandingBalance),
                                style = MaterialTheme.typography.bodySmall,
                                color = LubricantAmber,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EnolNavy)
            )
        },
        bottomBar = {
            OrderBottomBar(
                uiState = uiState,
                isExpanded = isCartExpanded,
                onToggleExpand = { isCartExpanded = !isCartExpanded },
                onPaymentTermsChanged = viewModel::setPaymentTerms,
                onSubmitOrder = viewModel::submitOrder
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search product name, SKU or grade...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SlateSteel)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = SlateSteel)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = grid_4, vertical = grid_2),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LubricantAmber,
                    unfocusedBorderColor = SlateSteel.copy(alpha = 0.3f)
                )
            )

            // 2. Category Filter Chips Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = grid_4, vertical = grid_1),
                horizontalArrangement = Arrangement.spacedBy(grid_2)
            ) {
                uiState.categories.forEach { category ->
                    val isSelected = (category == "ALL" && uiState.selectedCategory == null) ||
                            (uiState.selectedCategory == category)

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LubricantAmber,
                            selectedLabelColor = EnolNavy
                        )
                    )
                }
            }

            // Error banner if any
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorCrimson.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = grid_4, vertical = grid_1)
                ) {
                    Row(
                        modifier = Modifier.padding(grid_3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = ErrorCrimson)
                        Spacer(modifier = Modifier.width(grid_2))
                        Text(text = error, style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
                    }
                }
            }

            // 3. Multi-SKU Product Catalog List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = grid_4),
                verticalArrangement = Arrangement.spacedBy(grid_2)
            ) {
                items(uiState.filteredProducts, key = { it.id }) { product ->
                    val cartons = uiState.cartCartons[product.id] ?: 0
                    ProductCartCard(
                        product = product,
                        cartons = cartons,
                        onIncrement = { viewModel.incrementCarton(product.id) },
                        onDecrement = { viewModel.decrementCarton(product.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp)) // Padding to prevent bottom sheet overlap
                }
            }
        }
    }
}

/**
 * Individual Product Card with carton increment/decrement controls and scheme indicators.
 */
@Composable
private fun ProductCartCard(
    product: ProductEntity,
    cartons: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (cartons > 0) LubricantAmber.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (cartons > 0) 1.5.dp else 1.dp,
            color = if (cartons > 0) LubricantAmber else SlateSteel.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(grid_3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EnolNavy
                    )
                    Text(
                        text = "SKU: ${product.sku} • Grade: ${product.viscosityGrade} • ${product.packSizeLiters}L (${product.unitsPerCarton} units/ctn)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteel,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EnolNavy.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EnolNavy,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(grid_2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val ctnPrice = product.tradePrice * product.unitsPerCarton
                    Text(
                        text = "PKR %,.0f / carton".format(ctnPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LubricantAmber
                    )
                    Text(
                        text = "Trade: PKR %,.0f/unit".format(product.tradePrice),
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteelLight,
                        fontSize = 10.sp
                    )
                }

                // Stepper Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(grid_2)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        enabled = cartons > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (cartons > 0) LubricantAmber.copy(alpha = 0.2f) else SlateSteel.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Minus",
                            tint = if (cartons > 0) EnolNavy else SlateSteelLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(34.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                            .border(1.dp, if (cartons > 0) LubricantAmber else SlateSteel.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cartons.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (cartons > 0) LubricantAmber else SlateSteel
                        )
                    }

                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier
                            .size(36.dp)
                            .background(LubricantAmber, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Plus",
                            tint = EnolNavy,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Collapsible Bottom Sheet displaying real-time calculations and Credit Guardrail enforcement.
 */
@Composable
private fun OrderBottomBar(
    uiState: OrderCartUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPaymentTermsChanged: (String) -> Unit,
    onSubmitOrder: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation_4),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(grid_4)) {
            // 1. Expandable summary header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = LubricantAmber
                    )
                    Spacer(modifier = Modifier.width(grid_2))
                    Column {
                        Text(
                            text = "Net Payable: PKR %,.0f".format(uiState.netAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EnolNavy
                        )
                        Text(
                            text = "${uiState.totalCartons.toInt()} Cartons  •  %.1f Liters".format(uiState.totalLiters),
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteel,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = "Expand"
                    )
                }
            }

            // 2. Expandable calculation breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = grid_3)) {
                    HorizontalDivider(color = SlateSteel.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(grid_2))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Gross Total", style = MaterialTheme.typography.bodySmall, color = SlateSteel)
                        Text(text = "PKR %,.0f".format(uiState.grossAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Promotional Discounts / Rebates", style = MaterialTheme.typography.bodySmall, color = SuccessEmerald)
                        Text(text = "- PKR %,.0f".format(uiState.discountAmount), style = MaterialTheme.typography.bodySmall, color = SuccessEmerald, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(grid_3))

                    // Payment Terms Selector
                    Text(text = "Payment Terms", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = grid_1),
                        horizontalArrangement = Arrangement.spacedBy(grid_2)
                    ) {
                        listOf(
                            "CREDIT_15_DAYS" to "15d Credit",
                            "CREDIT_30_DAYS" to "30d Credit",
                            "CASH_ON_DELIVERY" to "COD"
                        ).forEach { (termsKey, label) ->
                            val isSelected = uiState.paymentTerms == termsKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) EnolNavy else SlateSteel.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPaymentTermsChanged(termsKey) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else SlateSteel,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = grid_2)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_3))

            // 3. CREDIT LIMIT GUARDRAIL BANNER
            if (uiState.exceedsCreditLimit) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorCrimson.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(grid_2),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ErrorCrimson, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(grid_2))
                        Column {
                            Text(
                                text = "CREDIT LIMIT EXCEEDED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ErrorCrimson
                            )
                            Text(
                                text = "Exposure: PKR %,.0f (Limit: %,.0f) • Exceeds by PKR %,.0f. Will require ASM Approval."
                                    .format(uiState.effectiveExposure, uiState.client?.creditLimit ?: 0.0, uiState.creditVariancePkr),
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorCrimson,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SuccessEmerald.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = grid_3, vertical = grid_1),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(grid_1))
                        val remainingCredit = ((uiState.client?.creditLimit ?: 0.0) - uiState.effectiveExposure).coerceAtLeast(0.0)
                        Text(
                            text = "Credit OK • PKR %,.0f headroom available".format(remainingCredit),
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessEmerald,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_3))

            // 4. Submit Order Button
            val canSubmit = uiState.totalCartons > 0 && !uiState.isSubmitting
            Button(
                onClick = onSubmitOrder,
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.exceedsCreditLimit) WarningAmber else LubricantAmber,
                    contentColor = EnolNavy
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(color = EnolNavy, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(text = "Booking Order...", fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = if (uiState.exceedsCreditLimit) "BOOK ORDER (REQUIRES ASM APPROVAL)" else "CONFIRM & BOOK ORDER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Order Success Dialog.
 */
@Composable
private fun OrderSuccessDialog(
    orderId: String,
    isCreditHold: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(grid_6)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_4),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(grid_6),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isCreditHold) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isCreditHold) WarningAmber else SuccessEmerald,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(modifier = Modifier.height(grid_3))

                    Text(
                        text = if (isCreditHold) "Order Placed on Credit Hold" else "Order Booked Successfully",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EnolNavy
                    )

                    Spacer(modifier = Modifier.height(grid_2))

                    Text(
                        text = if (isCreditHold)
                            "Order exceeds customer credit limit. It has been saved locally, queued for sync, and flagged for Area Sales Manager (ASM) authorization."
                        else
                            "Order has been persisted to local database and queued for automatic sync with external ERP/OMS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteel,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(grid_6))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = EnolNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Return to Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
