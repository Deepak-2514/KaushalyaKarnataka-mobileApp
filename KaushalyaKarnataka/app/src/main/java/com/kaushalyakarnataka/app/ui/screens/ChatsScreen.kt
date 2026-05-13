package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Search
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
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
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

    GalaxyBackground {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(48.dp))
            
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.messages,
                        style = MaterialTheme.typography.displayLarge,
                    )
                    if (chats.isNotEmpty()) {
                        Text(
                            text = "${chats.size} Active Conversations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (chats.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyChatsState()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                // Online Indicator (Dummy)
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .padding(2.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF10B981)))
                }
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = other?.name ?: strings.loading,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your inbox is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Conversations with workers will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
