package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalLayoutApi::class)
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(64.dp))
            
            Text(
                text = "Welcome to",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Text(
                text = strings.appName,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(40.dp))

            SectionTitle("Choose your role")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoleCard(
                    modifier = Modifier.weight(1f),
                    label = strings.roleCustomer,
                    isSelected = role == UserRole.customer,
                    onClick = { role = UserRole.customer }
                )
                RoleCard(
                    modifier = Modifier.weight(1f),
                    label = strings.roleWorker,
                    isSelected = role == UserRole.worker,
                    onClick = { role = UserRole.worker }
                )
            }

            if (role != null) {
                Spacer(Modifier.height(32.dp))
                SectionTitle("Your Details")
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
                    placeholder = "City or Neighborhood"
                )

                if (role == UserRole.worker) {
                    Spacer(Modifier.height(32.dp))
                    SectionTitle("Professional Category")
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WORK_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(strings.categories.label(cat)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = if (category == cat) null else FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = false,
                                    borderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(64.dp))

            GradientPrimaryButton(
                text = "Complete Profile",
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
                            "name" to (authDisplayName ?: "Professional User"),
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
                            onDone()
                        } catch (e: Exception) {
                            onMessage(e.localizedMessage ?: "Setup failed")
                        } finally {
                            submitting = false
                        }
                    }
                },
                loading = submitting,
                enabled = role != null,
            )
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleCard(modifier: Modifier, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Box(Modifier.padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
