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
import com.kaushalyakarnataka.app.ui.components.GradientPrimaryButton
import com.kaushalyakarnataka.app.ui.components.KKTextField
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
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

    Scaffold(
        containerColor = KaushalyaColors.Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(64.dp))
            
            Text(
                text = "Personalize your experience",
                style = MaterialTheme.typography.headlineLarge,
                color = KaushalyaColors.TextSecondary
            )
            Text(
                text = "Welcome to ${strings.appName}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = KaushalyaColors.TextPrimary
            )
            
            Spacer(Modifier.height(40.dp))

            SectionTitle("Identify your role")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                SectionTitle("Basic Information")
                KKTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                    label = strings.phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder = "10-digit primary contact"
                )
                
                Spacer(Modifier.height(16.dp))
                
                KKTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = strings.location,
                    placeholder = "Your current city/area"
                )

                if (role == UserRole.worker) {
                    Spacer(Modifier.height(32.dp))
                    SectionTitle("Professional Vertical")
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WORK_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(strings.categories.label(cat)) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = KaushalyaColors.Primary.copy(alpha = 0.2f),
                                    selectedLabelColor = KaushalyaColors.Primary,
                                    containerColor = KaushalyaColors.Secondary,
                                    labelColor = KaushalyaColors.TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = category == cat,
                                    borderColor = if (category == cat) KaushalyaColors.Primary else KaushalyaColors.Border,
                                    selectedBorderColor = KaushalyaColors.Primary,
                                    disabledBorderColor = KaushalyaColors.Border,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(64.dp))

            GradientPrimaryButton(
                text = "Initialize Profile",
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
                            "name" to (authDisplayName ?: "Verified Member"),
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
                            onMessage(e.localizedMessage ?: "Deployment failed")
                        } finally {
                            submitting = false
                        }
                    }
                },
                loading = submitting,
                enabled = role != null,
            )
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = KaushalyaColors.TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun RoleCard(modifier: Modifier, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) KaushalyaColors.Primary.copy(alpha = 0.1f) else KaushalyaColors.Secondary,
        border = BorderStroke(1.dp, if (isSelected) KaushalyaColors.Primary else KaushalyaColors.Border),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(Modifier.padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) KaushalyaColors.Primary else KaushalyaColors.TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
