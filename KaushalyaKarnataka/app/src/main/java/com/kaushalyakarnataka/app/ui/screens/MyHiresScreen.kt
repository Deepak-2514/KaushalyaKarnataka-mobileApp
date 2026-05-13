package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    LaunchedEffect(hires) {
        hires.forEach { h ->
            if (!workers.containsKey(h.workerId)) {
                repo.fetchUser(h.workerId)?.let { workers[h.workerId] = it }
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Active", "Completed", "Cancelled")

    val filteredHires = remember(hires, selectedTab) {
        when (selectedTab) {
            0 -> hires.filter { it.status == RequestStatus.pending || it.status == RequestStatus.accepted }
            1 -> hires.filter { it.status == RequestStatus.completed }
            else -> hires.filter { it.status == RequestStatus.rejected }
        }
    }

    var ratingWorkerId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    GalaxyBackground {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            Text(
                text = strings.myHires,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(Modifier.height(24.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 24.dp, 16.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredHires.isEmpty()) {
                    item {
                        EmptyHiresState(strings.noHiresYet)
                    }
                } else {
                    items(filteredHires, key = { it.requestId }) { hire ->
                        HireModernCard(
                            hire = hire,
                            worker = workers[hire.workerId],
                            strings = strings,
                            fmt = fmt,
                            onChat = {
                                scope.launch {
                                    val id = repo.findChatIdBetween(profile.userId, hire.workerId)
                                    if (id != null) onOpenChat(id) else onMessage(strings.chatNotFound)
                                }
                            },
                            onRate = { ratingWorkerId = hire.workerId },
                            showRate = hire.status == RequestStatus.completed && !ratedIds.contains(hire.workerId)
                        )
                    }
                }
            }
        }
    }

    if (ratingWorkerId != null) {
        RatingDialog(
            strings = strings,
            onDismiss = { ratingWorkerId = null },
            onSubmit = { stars, comment ->
                scope.launch {
                    runCatching {
                        repo.submitReview(workerId = ratingWorkerId!!, customerId = profile.userId, rating = stars, comment = comment)
                    }.onSuccess {
                        onMessage(strings.ratingSubmitted)
                        ratingWorkerId = null
                    }.onFailure { e ->
                        onMessage(e.message ?: strings.ratingFailed)
                        ratingWorkerId = null
                    }
                }
            }
        )
    }
}

@Composable
private fun HireModernCard(
    hire: JobRequest,
    worker: UserProfile?,
    strings: Strings,
    fmt: SimpleDateFormat,
    onChat: () -> Unit,
    onRate: () -> Unit,
    showRate: Boolean
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(
                model = worker?.profileImage,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        worker?.name ?: strings.loading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    StatusBadge(hire.status, strings)
                }
                Text(
                    strings.categoryTitle(worker?.category ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        fmt.format(hire.createdAt?.toDate() ?: Date()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Service Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = when(hire.status) {
                        RequestStatus.pending -> "Waiting for confirmation"
                        RequestStatus.accepted -> "Worker is assigned"
                        RequestStatus.completed -> "Service completed"
                        RequestStatus.rejected -> "Request declined"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hire.status == RequestStatus.accepted) {
                    IconButton(
                        onClick = onChat,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (showRate) {
                    Button(
                        onClick = onRate,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Rate", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: RequestStatus, strings: Strings) {
    val (color, text) = when (status) {
        RequestStatus.pending -> Color(0xFFF59E0B) to strings.pendingStatus
        RequestStatus.accepted -> Color(0xFF3B82F6) to strings.acceptedStatus
        RequestStatus.completed -> Color(0xFF10B981) to strings.completed
        RequestStatus.rejected -> Color(0xFFEF4444) to strings.rejectedStatus
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyHiresState(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.History, 
            contentDescription = null, 
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
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
        title = { Text("Rate Service", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(5) { idx ->
                        val v = idx + 1
                        IconButton(onClick = { stars = v }) {
                            Icon(
                                if (v <= stars) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (v <= stars) Color(0xFFFBBF24) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tell us about your experience...") },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(stars, comment) }) {
                Text("Submit Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}
