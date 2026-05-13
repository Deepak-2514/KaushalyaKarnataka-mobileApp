package com.kaushalyakarnataka.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.JobRequest
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.GradientPrimaryButton
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    profile: UserProfile,
    strings: Strings,
    repo: KaushalyaRepository,
    onOpenRequests: () -> Unit,
    onOpenChat: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val jobs by repo.observeAcceptedJobs(profile.userId).collectAsStateWithLifecycle(emptyList())
    val customers = remember { mutableStateMapOf<String, UserProfile>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(jobs) {
        jobs.forEach { job ->
            if (!customers.containsKey(job.customerId)) {
                val u = repo.fetchUser(job.customerId)
                if (u != null) customers[job.customerId] = u
            }
        }
    }

    Scaffold(
        containerColor = KaushalyaColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 56.dp, 20.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Text(
                    text = strings.dashboard,
                    style = MaterialTheme.typography.displayLarge,
                    color = KaushalyaColors.TextPrimary
                )
                Text(
                    text = "Overview of your professional activity",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KaushalyaColors.TextSecondary
                )
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = strings.jobsDone,
                        value = "${profile.jobsCompleted}",
                        icon = Icons.Rounded.Assignment,
                        color = KaushalyaColors.Primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = strings.myRating,
                        value = if (profile.rating > 0) String.format("%.1f", profile.rating) else strings.newLabel,
                        icon = Icons.Rounded.Star,
                        color = KaushalyaColors.Warning
                    )
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenRequests
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = strings.jobRequests,
                                style = MaterialTheme.typography.titleMedium,
                                color = KaushalyaColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.checkNewRequests,
                                style = MaterialTheme.typography.bodyMedium,
                                color = KaushalyaColors.TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = KaushalyaColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Active Jobs In-Progress",
                    style = MaterialTheme.typography.titleLarge,
                    color = KaushalyaColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (jobs.isEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.noActiveJobs,
                            style = MaterialTheme.typography.bodyLarge,
                            color = KaushalyaColors.TextMuted
                        )
                    }
                }
            } else {
                items(jobs, key = { it.requestId }) { job ->
                    ActiveJobCard(
                        job = job,
                        customer = customers[job.customerId],
                        strings = strings,
                        onCall = { phone ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }
                        },
                        onChat = {
                            scope.launch {
                                val id = repo.findChatIdBetween(profile.userId, job.customerId)
                                if (id != null) onOpenChat(id) else onMessage(strings.chatNotFound)
                            }
                        },
                        onComplete = {
                            scope.launch {
                                runCatching { repo.completeJob(job.requestId, profile.userId) }
                                    .onSuccess { onMessage(strings.jobDone) }
                                    .onFailure { onMessage(strings.tryAgain) }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = KaushalyaColors.TextPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = KaushalyaColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun ActiveJobCard(
    job: JobRequest,
    customer: UserProfile?,
    strings: Strings,
    onCall: (String) -> Unit,
    onChat: () -> Unit,
    onComplete: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AsyncImage(
                    model = customer?.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = customer?.name ?: strings.customer,
                        style = MaterialTheme.typography.titleMedium,
                        color = KaushalyaColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = customer?.location?.ifBlank { strings.locationUnknown } ?: strings.locationUnknown,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KaushalyaColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val phone = customer?.phone.orEmpty()
                    if (phone.isNotBlank()) {
                        IconButton(
                            onClick = { onCall(phone) },
                            modifier = Modifier.background(KaushalyaColors.Secondary, CircleShape)
                        ) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = KaushalyaColors.TextPrimary)
                        }
                    }
                    IconButton(
                        onClick = onChat,
                        modifier = Modifier.background(KaushalyaColors.Secondary, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = KaushalyaColors.TextPrimary)
                    }
                }
            }
            GradientPrimaryButton(
                text = strings.markDone,
                onClick = onComplete
            )
        }
    }
}
