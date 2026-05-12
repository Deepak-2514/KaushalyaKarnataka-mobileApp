package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserRole
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.GradientPrimaryButton
import com.kaushalyakarnataka.app.ui.components.KKTextField
import kotlinx.coroutines.launch

private val WORK_CATEGORIES = listOf(
    "Electrician", "Plumber", "Carpenter", "Painter", "Cleaner", "Gardener", "Other",
)

@Composable
fun OnboardingScreen(
    uid: String,
    strings: Strings,
    repo: KaushalyaRepository,
    authDisplayName: String?,
    authPhotoUrl: String?,
    onDone: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var role by remember { mutableStateOf<UserRole?>(null) }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    GalaxyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            
            Text(
                text = strings.onboardingTitle,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = strings.setupProfile,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Select your role",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoleSelectionChip(
                        selected = role == UserRole.customer,
                        label = strings.roleCustomer,
                        onClick = { role = UserRole.customer }
                    )
                    RoleSelectionChip(
                        selected = role == UserRole.worker,
                        label = strings.roleWorker,
                        onClick = { role = UserRole.worker }
                    )
                }
            }

            if (role != null) {
                Spacer(Modifier.height(24.dp))
                
                KKTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                    label = strings.phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder = "10-digit mobile number"
                )
                
                Spacer(Modifier.height(16.dp))
                
                KKTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = strings.location,
                    placeholder = "e.g. Bangalore, Indiranagar"
                )

                if (role == UserRole.worker) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = strings.category,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WORK_CATEGORIES.chunked(2).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { cat ->
                                    FilterChip(
                                        selected = category == cat,
                                        onClick = { category = cat },
                                        label = { Text(strings.categories.label(cat)) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            GradientPrimaryButton(
                text = strings.startJourney,
                onClick = click@{
                    val r = role ?: return@click
                    if (phone.length != 10 || location.isBlank()) {
                        onMessage(strings.fillDetails)
                        return@click
                    }
                    if (r == UserRole.worker && category.isBlank()) {
                        onMessage(strings.selectCategory)
                        return@click
                    }
                    submitting = true
                    scope.launch {
                        val fields = mutableMapOf<String, Any>(
                            "userId" to uid,
                            "name" to (authDisplayName ?: "New User"),
                            "role" to r.name,
                            "phone" to phone,
                            "location" to location,
                            "rating" to 0.0,
                            "jobsCompleted" to 0L,
                            "profileImage" to (authPhotoUrl ?: ""),
                        )
                        if (r == UserRole.worker) fields["category"] = category
                        
                        try {
                            repo.mergeUserProfile(uid, fields)
                            onMessage("${strings.welcome} ${strings.appName}")
                            onDone()
                        } catch (e: Exception) {
                            onMessage("Error: ${e.localizedMessage ?: strings.onboardingFailed}")
                        } finally {
                            submitting = false
                        }
                    }
                },
                loading = submitting,
                enabled = role != null,
            )
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RoleSelectionChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { 
            Text(
                text = label,
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge
            ) 
        },
        modifier = Modifier.fillMaxWidth(),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp
        )
    )
}
