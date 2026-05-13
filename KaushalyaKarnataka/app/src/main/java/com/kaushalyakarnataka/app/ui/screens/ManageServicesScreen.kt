package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.ServiceItem
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.KKTextField
import com.kaushalyakarnataka.app.ui.components.ScreenLoading
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageServicesScreen(
    repo: KaushalyaRepository,
    strings: Strings,
    onBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    val uid = repo.currentUserId ?: return
    val services by repo.observeServices(uid).collectAsStateWithLifecycle(emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ServiceItem?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = KaushalyaColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(strings.manageServices, color = KaushalyaColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = KaushalyaColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KaushalyaColors.Background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = KaushalyaColors.Primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (services.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(strings.noServices, color = KaushalyaColors.TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(services, key = { it.serviceId }) { service ->
                    ServiceEditCard(
                        service = service,
                        strings = strings,
                        onEdit = { editingService = service },
                        onDelete = {
                            scope.launch {
                                runCatching { repo.deleteService(service.serviceId) }
                                    .onSuccess { onMessage(strings.serviceDeleted) }
                                    .onFailure { onMessage(strings.deleteFailed) }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ServiceDialog(
            strings = strings,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, price, desc ->
                scope.launch {
                    runCatching { repo.addService(uid, title, price, desc) }
                        .onSuccess { 
                            onMessage(strings.serviceAdded)
                            showAddDialog = false
                        }
                        .onFailure { onMessage(strings.serviceAddFailed) }
                }
            }
        )
    }

    if (editingService != null) {
        val s = editingService!!
        ServiceDialog(
            strings = strings,
            initialTitle = s.title,
            initialPrice = s.price.toString(),
            initialDesc = s.description,
            onDismiss = { editingService = null },
            onConfirm = { title, price, desc ->
                scope.launch {
                    runCatching {
                        repo.updateService(s.serviceId, mapOf(
                            "title" to title,
                            "price" to price,
                            "description" to desc
                        ))
                    }.onSuccess {
                        onMessage(strings.profileUpdated)
                        editingService = null
                    }.onFailure { onMessage(strings.updateFailed) }
                }
            }
        )
    }
}

@Composable
private fun ServiceEditCard(
    service: ServiceItem,
    strings: Strings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(service.title, style = MaterialTheme.typography.titleMedium, color = KaushalyaColors.TextPrimary, fontWeight = FontWeight.Bold)
                Text("₹${service.price.toInt()}", style = MaterialTheme.typography.bodyLarge, color = KaushalyaColors.Primary, fontWeight = FontWeight.SemiBold)
                if (service.description.isNotBlank()) {
                    Text(service.description, style = MaterialTheme.typography.bodySmall, color = KaushalyaColors.TextMuted)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = KaushalyaColors.TextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = KaushalyaColors.Error)
            }
        }
    }
}

@Composable
private fun ServiceDialog(
    strings: Strings,
    initialTitle: String = "",
    initialPrice: String = "",
    initialDesc: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var price by remember { mutableStateOf(initialPrice) }
    var desc by remember { mutableStateOf(initialDesc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KaushalyaColors.Elevated,
        title = { Text(if (initialTitle.isEmpty()) "Add Service" else "Edit Service", color = KaushalyaColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KKTextField(value = title, onValueChange = { title = it }, label = strings.serviceName)
                KKTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                    label = strings.price,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                KKTextField(value = desc, onValueChange = { desc = it }, label = strings.serviceDetails, singleLine = false)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank()) onConfirm(title, p, desc)
                },
                colors = ButtonDefaults.buttonColors(containerColor = KaushalyaColors.Primary)
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = KaushalyaColors.TextSecondary)
            }
        }
    )
}
