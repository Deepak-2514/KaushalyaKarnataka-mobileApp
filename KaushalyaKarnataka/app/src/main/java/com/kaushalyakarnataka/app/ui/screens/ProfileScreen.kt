package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.data.UserRole
import com.kaushalyakarnataka.app.ui.AppLanguage
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors

@Composable
fun ProfileScreen(
    profile: UserProfile,
    strings: Strings,
    language: AppLanguage,
    repo: KaushalyaRepository,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit,
    onManageServices: () -> Unit,
    onMessage: (String) -> Unit,
) {
    Scaffold(
        containerColor = KaushalyaColors.Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(64.dp))
            
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.profile,
                    style = MaterialTheme.typography.displayLarge,
                    color = KaushalyaColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.background(KaushalyaColors.Error.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = KaushalyaColors.Error)
                }
            }

            Spacer(Modifier.height(32.dp))

            // User Info Card
            ProfileInfoCard(profile, strings)

            Spacer(Modifier.height(28.dp))

            // Stats Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = strings.myHires,
                    value = "${profile.jobsCompleted}",
                    icon = Icons.Outlined.History,
                    color = KaushalyaColors.Primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Account Rating",
                    value = if (profile.rating > 0) String.format("%.1f", profile.rating) else "4.8",
                    icon = Icons.Outlined.StarRate,
                    color = KaushalyaColors.Warning
                )
            }

            Spacer(Modifier.height(32.dp))

            // Settings Sections
            SettingsGroup(title = "Account Settings") {
                SettingsItem(
                    icon = Icons.Outlined.Person,
                    title = "Personal Information",
                    subtitle = "Manage your professional identity",
                    onClick = {}
                )
                if (profile.role == UserRole.worker) {
                    SettingsItem(
                        icon = Icons.Outlined.Category,
                        title = strings.manageServices,
                        subtitle = "Update your service rates and details",
                        onClick = onManageServices
                    )
                }
                SettingsItem(
                    icon = Icons.Outlined.Language,
                    title = "Interface Language",
                    subtitle = if (language == AppLanguage.EN) "English" else "ಕನ್ನಡ",
                    onClick = onToggleLanguage
                )
            }

            Spacer(Modifier.height(140.dp))
        }
    }
}

@Composable
private fun ProfileInfoCard(profile: UserProfile, strings: Strings) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box {
                AsyncImage(
                    model = profile.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    onClick = {},
                    shape = CircleShape,
                    color = KaushalyaColors.Primary,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Column {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KaushalyaColors.TextPrimary
                )
                Text(
                    text = if (profile.role == UserRole.worker) strings.roleWorker else strings.roleCustomer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaushalyaColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = profile.phone,
                    style = MaterialTheme.typography.labelSmall,
                    color = KaushalyaColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KaushalyaColors.TextPrimary)
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = KaushalyaColors.TextMuted)
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = KaushalyaColors.TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KaushalyaColors.Secondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = KaushalyaColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = KaushalyaColors.TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = KaushalyaColors.TextMuted)
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = KaushalyaColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
