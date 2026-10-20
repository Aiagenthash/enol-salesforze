package com.enol.salesforze.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enol.salesforze.data.remote.models.ApiKeyDto
import com.enol.salesforze.data.remote.models.WebhookDeliveryLogDto
import com.enol.salesforze.data.remote.models.WebhookSubscriptionDto
import com.enol.salesforze.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApiPortalScreen(
    viewModel: AdminApiPortalViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showGenerateKeyDialog by remember { mutableStateOf(false) }
    var showRegisterWebhookDialog by remember { mutableStateOf(false) }

    val tabTitles = listOf(
        "API Keys (${uiState.apiKeys.size})",
        "Webhooks (${uiState.webhookSubscriptions.size})",
        "Delivery Logs (${uiState.webhookLogs.size})"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin API Governance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "B2B Keys, HMAC Webhooks & Outbound Gateways",
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
                    IconButton(onClick = { viewModel.loadGovernanceData() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = SlateSteelLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = LubricantAmber,
                divider = {
                    HorizontalDivider(color = SlateSteelDark)
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) LubricantAmber else SlateSteelLight
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> ApiKeysTabContent(
                    keys = uiState.apiKeys,
                    onOpenCreateDialog = { showGenerateKeyDialog = true }
                )
                1 -> WebhooksTabContent(
                    webhooks = uiState.webhookSubscriptions,
                    onOpenCreateDialog = { showRegisterWebhookDialog = true }
                )
                2 -> WebhookLogsTabContent(logs = uiState.webhookLogs)
            }
        }
    }

    // Modal: Generate API Key
    if (showGenerateKeyDialog) {
        GenerateApiKeyDialog(
            onDismiss = { showGenerateKeyDialog = false },
            onConfirm = { name, scopes ->
                viewModel.generateApiKey(name, scopes)
                showGenerateKeyDialog = false
            }
        )
    }

    // Modal: Register Webhook
    if (showRegisterWebhookDialog) {
        RegisterWebhookDialog(
            onDismiss = { showRegisterWebhookDialog = false },
            onConfirm = { name, url, events ->
                viewModel.registerWebhook(name, url, events)
                showRegisterWebhookDialog = false
            }
        )
    }

    // Modal: Newly Created Key / Secret Reveal
    if (uiState.newlyCreatedApiKey != null || uiState.newlyCreatedSecret != null) {
        val secretValue = uiState.newlyCreatedApiKey ?: uiState.newlyCreatedSecret ?: ""
        val isApiKey = uiState.newlyCreatedApiKey != null

        AlertDialog(
            onDismissRequest = { viewModel.dismissKeyDialog() },
            title = {
                Text(
                    text = if (isApiKey) "New API Key Generated" else "Webhook HMAC Secret Generated",
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Please copy and securely store this secret. For security reasons, it will never be displayed again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteelLight
                    )
                    Spacer(modifier = Modifier.height(grid_3))
                    Surface(
                        shape = RoundedCornerShape(grid_2),
                        color = SurfaceContainerDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = secretValue,
                            modifier = Modifier.padding(grid_3),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = LubricantAmber
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Enol Secret", secretValue))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        viewModel.dismissKeyDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = EnolNavyDark)
                    Spacer(modifier = Modifier.width(grid_1))
                    Text("Copy & Close", color = EnolNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

// =========================================================================
// 1. API KEYS TAB
// =========================================================================

@Composable
private fun ApiKeysTabContent(
    keys: List<ApiKeyDto>,
    onOpenCreateDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(grid_4),
        verticalArrangement = Arrangement.spacedBy(grid_3),
        contentPadding = PaddingValues(bottom = grid_6)
    ) {
        item {
            Button(
                onClick = onOpenCreateDialog,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(grid_2),
                colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = EnolNavyDark)
                Spacer(modifier = Modifier.width(grid_1_5))
                Text("Generate New B2B API Key", color = EnolNavyDark, fontWeight = FontWeight.Bold)
            }
        }

        items(keys) { key ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(grid_2),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(grid_3)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = key.keyName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Surface(
                            shape = RoundedCornerShape(grid_1),
                            color = SuccessEmerald.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessEmerald)
                        ) {
                            Text(
                                text = if (key.isActive) "ACTIVE" else "REVOKED",
                                modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(grid_1))

                    Text(
                        text = "Prefix: ${key.keyPrefix}••••••••",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = SlateSteelLight
                    )

                    Spacer(modifier = Modifier.height(grid_2))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(grid_1)) {
                        items(key.scopes) { scope ->
                            Surface(
                                shape = RoundedCornerShape(grid_1),
                                color = SurfaceContainerDark
                            ) {
                                Text(
                                    text = scope,
                                    modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LubricantAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. WEBHOOKS TAB
// =========================================================================

@Composable
private fun WebhooksTabContent(
    webhooks: List<WebhookSubscriptionDto>,
    onOpenCreateDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(grid_4),
        verticalArrangement = Arrangement.spacedBy(grid_3),
        contentPadding = PaddingValues(bottom = grid_6)
    ) {
        item {
            Button(
                onClick = onOpenCreateDialog,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(grid_2),
                colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = EnolNavyDark)
                Spacer(modifier = Modifier.width(grid_1_5))
                Text("Register Outbound Webhook", color = EnolNavyDark, fontWeight = FontWeight.Bold)
            }
        }

        items(webhooks) { webhook ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(grid_2),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(grid_3)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = webhook.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Surface(
                            shape = RoundedCornerShape(grid_1),
                            color = SuccessEmerald.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessEmerald)
                        ) {
                            Text(
                                text = "HMAC-SHA256",
                                modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(grid_1))

                    Text(
                        text = webhook.targetUrl,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )

                    Spacer(modifier = Modifier.height(grid_2))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(grid_1)) {
                        items(webhook.events) { event ->
                            Surface(
                                shape = RoundedCornerShape(grid_1),
                                color = SurfaceContainerDark
                            ) {
                                Text(
                                    text = event,
                                    modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LubricantAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. WEBHOOK LOGS TAB
// =========================================================================

@Composable
private fun WebhookLogsTabContent(logs: List<WebhookDeliveryLogDto>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(grid_4),
        verticalArrangement = Arrangement.spacedBy(grid_3),
        contentPadding = PaddingValues(bottom = grid_6)
    ) {
        items(logs) { log ->
            val isSuccess = log.statusCode in 200..299
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(grid_2)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(grid_3),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.eventType,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = log.targetUrl ?: "Target Gateway",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = SlateSteelLight,
                            maxLines = 1
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(grid_1),
                            color = if (isSuccess) SuccessEmerald.copy(alpha = 0.2f) else ErrorCrimson.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSuccess) SuccessEmerald else ErrorCrimson)
                        ) {
                            Text(
                                text = "${log.statusCode ?: 200} OK",
                                modifier = Modifier.padding(horizontal = grid_1_5, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSuccess) SuccessEmerald else ErrorCrimson
                            )
                        }
                        Text(
                            text = "${log.durationMs ?: 120}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateSteelLight
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// DIALOGS: CREATE API KEY & REGISTER WEBHOOK
// =========================================================================

@Composable
private fun GenerateApiKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var keyName by remember { mutableStateOf("") }
    val availableScopes = listOf("orders:read", "orders:write", "clients:read", "clients:write", "telemetry:read", "kpis:read")
    val selectedScopes = remember { mutableStateListOf("orders:read", "clients:read") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate B2B API Key", fontWeight = FontWeight.Bold, color = OnSurfaceDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(grid_2)) {
                OutlinedTextField(
                    value = keyName,
                    onValueChange = { keyName = it },
                    label = { Text("Key Name (e.g. SAP OMS Client)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Allowed Permissions & Scopes:", style = MaterialTheme.typography.labelMedium, color = SlateSteelLight)

                availableScopes.forEach { scope ->
                    val isChecked = selectedScopes.contains(scope)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedScopes.add(scope) else selectedScopes.remove(scope)
                            }
                        )
                        Spacer(modifier = Modifier.width(grid_1))
                        Text(text = scope, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDark)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (keyName.isNotBlank()) onConfirm(keyName, selectedScopes.toList()) },
                enabled = keyName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber)
            ) {
                Text("Generate", color = EnolNavyDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateSteelLight)
            }
        },
        containerColor = SurfaceDark
    )
}

@Composable
private fun RegisterWebhookDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetUrl by remember { mutableStateOf("https://") }
    val availableEvents = listOf("order.created", "order.approved", "client.enrolled", "visit.completed", "tada.flagged")
    val selectedEvents = remember { mutableStateListOf("order.created", "tada.flagged") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Webhook Gateway", fontWeight = FontWeight.Bold, color = OnSurfaceDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(grid_2)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Webhook Name (e.g. ERP Gateway)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetUrl,
                    onValueChange = { targetUrl = it },
                    label = { Text("Target Endpoint URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Event Subscriptions:", style = MaterialTheme.typography.labelMedium, color = SlateSteelLight)

                availableEvents.forEach { event ->
                    val isChecked = selectedEvents.contains(event)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedEvents.add(event) else selectedEvents.remove(event)
                            }
                        )
                        Spacer(modifier = Modifier.width(grid_1))
                        Text(text = event, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDark)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && targetUrl.isNotBlank()) onConfirm(name, targetUrl, selectedEvents.toList()) },
                enabled = name.isNotBlank() && targetUrl.startsWith("http"),
                colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber)
            ) {
                Text("Register", color = EnolNavyDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateSteelLight)
            }
        },
        containerColor = SurfaceDark
    )
}
