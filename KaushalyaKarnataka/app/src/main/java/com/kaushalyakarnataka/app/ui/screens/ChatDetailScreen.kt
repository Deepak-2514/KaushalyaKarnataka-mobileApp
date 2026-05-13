package com.kaushalyakarnataka.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.ChatMessage
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.KKTextField
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    profile: UserProfile,
    strings: Strings,
    repo: KaushalyaRepository,
    onBack: () -> Unit,
) {
    val vm: ChatDetailViewModel = viewModel(
        key = "chat-$chatId-${profile.userId}",
        factory = ChatDetailViewModel.factory(
            chatId = chatId,
            selfUserId = profile.userId,
            repo = repo,
        ),
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val tf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val messages = uiState.messages

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = KaushalyaColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AsyncImage(
                            model = uiState.otherUser?.profileImage,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = uiState.otherUser?.name ?: strings.loading,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KaushalyaColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Active Now",
                                style = MaterialTheme.typography.labelSmall,
                                color = KaushalyaColors.Success,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = KaushalyaColors.TextPrimary)
                    }
                },
                actions = {
                    val phone = uiState.otherUser?.phone.orEmpty()
                    if (phone.isNotBlank()) {
                        IconButton(onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }
                        }) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, tint = KaushalyaColors.Primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KaushalyaColors.Background,
                    titleContentColor = KaushalyaColors.TextPrimary,
                )
            )
        },
        bottomBar = {
            Surface(
                color = KaushalyaColors.Background,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KKTextField(
                        value = uiState.draft,
                        onValueChange = vm::onDraftChange,
                        modifier = Modifier.weight(1f),
                        placeholder = strings.typeMessage,
                    )

                    Spacer(Modifier.width(12.dp))

                    IconButton(
                        onClick = vm::sendMessage,
                        enabled = uiState.draft.isNotBlank() && !uiState.isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (uiState.draft.isNotBlank()) KaushalyaColors.Primary else KaushalyaColors.Secondary)
                    ) {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                                tint = if (uiState.draft.isNotBlank()) Color.White else KaushalyaColors.TextMuted
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            state = listState
        ) {
            items(items = messages, key = { it.messageId }) { msg ->
                MessageBubble(msg, profile.userId, tf)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, selfId: String, tf: SimpleDateFormat) {
    val isMine = msg.senderId == selfId
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isMine) KaushalyaColors.Primary else KaushalyaColors.Secondary
    val textColor = if (isMine) Color.White else KaushalyaColors.TextPrimary
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMine) 16.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 16.dp
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = msg.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                val time = msg.timestamp?.toDate()?.let { tf.format(it) } ?: ""
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
