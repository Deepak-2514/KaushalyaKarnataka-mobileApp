package com.kaushalyakarnataka.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Phone
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
import com.google.firebase.Timestamp
import com.kaushalyakarnataka.app.data.JobRequest
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.RequestStatus
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.GradientPrimaryButton
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RequestsScreen(
    profile: UserProfile,
    strings: Strings,
    repo: KaushalyaRepository,
    onMessage: (String) -> Unit,
) {
    val requests by repo.observeRequestsForWorker(profile.userId).collectAsStateWithLifecycle(emptyList())
    val customers = remember { mutableStateMapOf<String, UserProfile>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(requests) {
        requests.forEach { r ->
            if (!customers.containsKey(r.customerId)) {
                val u = repo.fetchUser(r.customerId)
                if (u != null) customers[r.customerId] = u
            }
        }
    }

    val pending = requests.filter { it.status == RequestStatus.pending }
    val history = requests.filter { it.status != RequestStatus.pending }

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
                    text = strings.jobRequests,
                    style = MaterialTheme.typography.displayLarge,
                    color = KaushalyaColors.TextPrimary
                )
            }

            item {
                Text(
                    text = "${strings.newRequests} (${pending.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = KaushalyaColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (pending.isEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = strings.noNewRequests,
                            style = MaterialTheme.typography.bodyLarge,
                            color = KaushalyaColors.TextMuted
                        )
                    }
                }
            } else {
                items(pending, key = { it.requestId }) { req ->
                    PendingCard(req, customers[req.customerId], strings, timeFmt, onReject = {
                        scope.launch {
                            runCatching { repo.rejectRequest(req.requestId) }
                                .onSuccess { onMessage(strings.requestRejected) }
                                .onFailure { onMessage(strings.tryAgain) }
                        }
                    }, onAccept = {
                        scope.launch {
                            runCatching {
                                repo.acceptRequest(req.requestId, profile.userId, req.customerId)
                            }.onSuccess { onMessage(strings.requestAccepted) }
                                .onFailure { onMessage(strings.tryAgain) }
                        }
                    })
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Text(
                        text = strings.pastJobs,
                        style = MaterialTheme.typography.titleLarge,
                        color = KaushalyaColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                items(history, key = { it.requestId }) { req ->
                    HistoryCard(
                        req,
                        customers[req.customerId],
                        strings,
                        onCall = { phone ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }
                        },
                        onComplete = if (req.status == RequestStatus.accepted) {
                            {
                                scope.launch {
                                    runCatching { repo.completeJobFromRequestsList(req.requestId, profile.userId) }
                                        .onSuccess { onMessage(strings.jobDone) }
                                        .onFailure { onMessage(strings.tryAgain) }
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingCard(
    req: JobRequest,
    customer: UserProfile?,
    strings: Strings,
    timeFmt: SimpleDateFormat,
    onReject: () -> Unit,
    onAccept: () -> Unit,
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
                        text = customer?.location ?: strings.locationUnknown,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KaushalyaColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = KaushalyaColors.Primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "URGENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = KaushalyaColors.Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatTime(req.createdAt, timeFmt),
                        style = MaterialTheme.typography.labelSmall,
                        color = KaushalyaColors.TextMuted
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KaushalyaColors.Error),
                    border = BorderStroke(1.dp, KaushalyaColors.Error.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.reject, fontWeight = FontWeight.SemiBold)
                }
                GradientPrimaryButton(
                    text = strings.accept,
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(48.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    req: JobRequest,
    customer: UserProfile?,
    strings: Strings,
    onCall: (String) -> Unit,
    onComplete: (() -> Unit)?,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AsyncImage(
                model = customer?.profileImage,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
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
                val (statusLabel, statusColor) = when (req.status) {
                    RequestStatus.accepted -> strings.acceptedStatus to KaushalyaColors.Primary
                    RequestStatus.completed -> strings.completed to KaushalyaColors.Success
                    RequestStatus.rejected -> strings.rejectedStatus to KaushalyaColors.Error
                    else -> strings.pendingStatus to KaushalyaColors.TextMuted
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (req.status == RequestStatus.accepted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val phone = customer?.phone.orEmpty()
                    if (phone.isNotBlank()) {
                        IconButton(
                            onClick = { onCall(phone) },
                            modifier = Modifier.background(KaushalyaColors.Secondary, CircleShape)
                        ) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = KaushalyaColors.TextPrimary)
                        }
                    }
                    onComplete?.let { done ->
                        IconButton(
                            onClick = done,
                            modifier = Modifier
                                .background(KaushalyaColors.Primary, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ts: Timestamp?, fmt: SimpleDateFormat): String {
    val ms = ts?.toDate()?.time ?: return ""
    return fmt.format(Date(ms))
}
