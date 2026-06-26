package com.kittyai.pet.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private val PinkBg = Color(0xFFFFF4F7)
private val KittyPink = Color(0xFFEF476F)
private val KittyDarkPink = Color(0xFFD83D61)
private val TextPrimary = Color(0xFF333333)
private val TextSecondary = Color(0xFF777777)
private val CardBg = Color.White
private val UserBubble = Color(0xFFE3F2FD)
private val KittyBubble = Color(0xFFFFF0F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = PinkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Kitty AI", fontWeight = FontWeight.Bold, color = KittyPink)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PinkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ---- Message list ----
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Text(
                            "想和 Kitty 说什么？\n\n试试 \"你好呀\" 或者 \"讲个睡前故事\"",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp)
                        )
                    }
                }
                items(state.messages, key = { it.id }) { msg ->
                    MessageBubble(msg, viewModel)
                }
                if (state.isLoading) {
                    item {
                        LoadingBubble()
                    }
                }
            }

            // ---- Quick reply chips ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickChip("讲个睡前故事") { viewModel.sendQuickMessage("讲个睡前故事") }
                QuickChip("小猫 三分钟") { viewModel.sendQuickMessage("小猫 三分钟") }
                QuickChip("月亮 五分钟") { viewModel.sendQuickMessage("月亮 五分钟") }
            }
            Spacer(Modifier.height(6.dp))

            // ---- Input bar ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { viewModel.updateInput(it) },
                    placeholder = { Text("想和 Kitty 说什么？", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KittyPink,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                FilledIconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = !state.isLoading,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = KittyPink,
                        disabledContainerColor = KittyPink.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "发送", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, viewModel: ChatViewModel) {
    val isUser = msg.role == MessageRole.USER
    val bubbleColor = if (isUser) UserBubble else KittyBubble
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 0.5.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Title for bedtime stories
                if (!msg.title.isNullOrBlank()) {
                    Text(
                        text = msg.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KittyPink
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // Story / chat text
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 24.sp
                )

                // Audio controls for bedtime stories
                if (msg.mode == "bedtime" && !msg.audioUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    AudioControlBar(viewModel)
                }
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = KittyBubble,
            shadowElevation = 0.5.dp
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = KittyPink,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Kitty 正在想...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AudioControlBar(viewModel: ChatViewModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value

        IconButton(onClick = { viewModel.play() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.PlayArrow, "播放", tint = KittyPink, modifier = Modifier.size(20.dp))
        }
        IconButton(
            onClick = { viewModel.pause() },
            enabled = state.isPlaying,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Pause, "暂停", tint = if (state.isPlaying) KittyPink else TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = { viewModel.stop() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Stop, "停止", tint = KittyPink, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = { viewModel.replay() }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Replay, "重播", tint = KittyPink, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(16.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = CardBg,
            labelColor = KittyPink
        ),
        modifier = Modifier.height(32.dp)
    )
}
