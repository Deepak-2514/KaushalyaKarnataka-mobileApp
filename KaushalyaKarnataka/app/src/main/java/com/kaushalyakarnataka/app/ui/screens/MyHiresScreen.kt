package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Timestamp
import com.kaushalyakarnataka.app.data.JobRequest
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.RequestStatus
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GlassCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyHiresScreen(
    profile: UserProfile,
    strings: Strings,
    repo: KaushalyaRepository,
    onOpenChat: (String) -> Unit,
    onMessage: (String) -> Unit,
) {
    val hires by repo.observeRequestsForCustomer(profile.userId).collectAsStateWithLifecycle(emptyList())
    val myReviews by repo.observeReviewsByCustomer(profile.userId).collectAsStateWithLifecycle(emptyList())
    val ratedIds = remember(myReviews) { myReviews.map { it.workerId }.toSet() }

    val workers = remember { mutableStateMapOf<String, UserProfile>() }
    LaunchedEffect(hires.map { it.workerId }.joinToString()) {
        hires.forEach { h ->
            if (!workers.containsKey(h.workerId)) {
                repo.fetchUser(h.workerId)?.let { workers[h.workerId] = it }
            }
        }
    }

    var ratingWorkerId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(strings.myHires, style = MaterialTheme.typography.headlineLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        if (hires.isEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            strings.noHiresYet,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            strings.findWorkersHint,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            items(hires, key = { it.requestId }) { hire ->
                val w = workers[hire.workerId]
                HireCard(
                    hire = hire,
                    worker = w,
                    strings = strings,
                    fmt = fmt,
                    onChat = {
                        scope.launch {
                            val id = repo.findChatIdBetween(profile.userId, hire.workerId)
                            if (id != null) onOpenChat(id) else onMessage(strings.chatNotFound)
                        }
                    },
                    onRate = {
                        ratingWorkerId = hire.workerId
                    },
                    showRate = hire.status == RequestStatus.completed && !ratedIds.contains(hire.workerId),
                )
            }
        }
    }

    if (ratingWorkerId != null) {
        val wid = ratingWorkerId!!
        RatingDialog(
            strings = strings,
            onDismiss = { ratingWorkerId = null },
            onSubmit = { stars, comment ->
                scope.launch {
                    runCatching {
                        repo.submitReview(workerId = wid, customerId = profile.userId, rating = stars, comment = comment)
                    }.onSuccess {
                        onMessage(strings.ratingSubmitted)
                        ratingWorkerId = null
                    }.onFailure { e ->
                        val msg = when (e.message) {
                            "already_rated" -> strings.alreadyRated
                            else -> strings.ratingFailed
                        }
                        onMessage(msg)
                        ratingWorkerId = null
                    }
                }
            },
        )
    }
}

@Composable
private fun HireCard(
    hire: JobRequest,
    worker: UserProfile?,
    strings: Strings,
    fmt: SimpleDateFormat,
    onChat: () -> Unit,
    onRate: () -> Unit,
    showRate: Boolean,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        worker?.name ?: strings.customer,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        strings.categoryTitle(worker?.category ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                val statusText = when (hire.status) {
                    RequestStatus.pending -> strings.pendingStatus
                    RequestStatus.accepted -> strings.acceptedStatus
                    RequestStatus.completed -> strings.completed
                    RequestStatus.rejected -> strings.rejectedStatus
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTs(hire.createdAt, fmt),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                when (hire.status) {
                    RequestStatus.accepted -> IconButton(onClick = onChat) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = strings.messages)
                    }
                    RequestStatus.completed -> if (showRate) {
                        OutlinedButton(onClick = onRate, modifier = Modifier.padding(start = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Outlined.Star, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(strings.rateWorker, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    } else {
                        Spacer(Modifier.height(0.dp))
                    }
                    else -> Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun RatingDialog(
    strings: Strings,
    onDismiss: () -> Unit,
    onSubmit: (stars: Int, comment: String) -> Unit,
) {
    var stars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.rateWorker) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    repeat(5) { idx ->
                        val v = idx + 1
                        TextButton(onClick = { stars = v }) {
                            Icon(
                                Icons.Outlined.Star,
                                contentDescription = "$v",
                                tint = if (v <= stars) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.reviewHint) },
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(stars, comment) }) {
                Text(strings.submitReview)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}

private fun formatTs(ts: Timestamp?, fmt: SimpleDateFormat): String {
    val ms = ts?.toDate()?.time ?: return ""
    return fmt.format(Date(ms))
}
