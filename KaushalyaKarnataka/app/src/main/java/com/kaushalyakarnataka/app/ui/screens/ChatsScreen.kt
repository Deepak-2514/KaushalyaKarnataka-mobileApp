package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
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
import com.kaushalyakarnataka.app.data.ChatThread
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.data.UserRole
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatsScreen(
    profile: UserProfile,
    strings: Strings,
    repo: KaushalyaRepository,
    onOpenChat: (String) -> Unit,
) {
    val chats by repo.observeChats(profile.userId).collectAsStateWithLifecycle(emptyList())
    val others = remember { mutableStateMapOf<String, UserProfile>() }

    LaunchedEffect(chats) {
        chats.forEach { chat ->
            val otherId = chat.participants.firstOrNull { it != profile.userId } ?: return@forEach
            if (!others.containsKey(otherId)) {
                val u = repo.fetchUser(otherId)
                if (u != null) others[otherId] = u
            }
        }
    }

    val tf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = KaushalyaColors.Background
    ) { padding ->
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(56.dp))
            
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.messages,
                        style = MaterialTheme.typography.displayLarge,
                        color = KaushalyaColors.TextPrimary
                    )
                    if (chats.isNotEmpty()) {
                        Text(
                            text = "${chats.size} Active Connections",
                            style = MaterialTheme.typography.labelLarge,
                            color = KaushalyaColors.Primary,
                        )
                    }
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier.background(KaushalyaColors.Secondary, CircleShape)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = KaushalyaColors.TextPrimary)
                }
            }

            Spacer(Modifier.height(32.dp))

            if (chats.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyChatsState()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(chats, key = { it.chatId }) { chat ->
                        val otherId = chat.participants.firstOrNull { it != profile.userId }
                        val other = otherId?.let { others[it] }

                        ChatRowModern(
                            chat = chat,
                            other = other,
                            strings = strings,
                            tf = tf,
                            onOpenChat = onOpenChat
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatRowModern(
    chat: ChatThread,
    other: UserProfile?,
    strings: Strings,
    tf: SimpleDateFormat,
    onOpenChat: (String) -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenChat(chat.chatId) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box {
                AsyncImage(
                    model = other?.profileImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
                // Online Indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(KaushalyaColors.Background)
                        .padding(2.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(KaushalyaColors.Success))
                }
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = other?.name ?: strings.loading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaushalyaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "12:30", // Placeholder for timestamp
                        style = MaterialTheme.typography.labelSmall,
                        color = KaushalyaColors.TextMuted
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val subtitle = if (other?.role == UserRole.worker) {
                        strings.categoryTitle(other.category ?: "")
                    } else {
                        "Customer"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = KaushalyaColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatsState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Chat,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = KaushalyaColors.TextMuted.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Your inbox is silent",
            style = MaterialTheme.typography.titleLarge,
            color = KaushalyaColors.TextSecondary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Messages from experts will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = KaushalyaColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
