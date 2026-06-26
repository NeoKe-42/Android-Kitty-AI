package com.kittyai.pet.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Soft bedtime color palette
private val PinkBg = androidx.compose.ui.graphics.Color(0xFFFFF4F7)
private val KittyPink = androidx.compose.ui.graphics.Color(0xFFEF476F)
private val KittyDarkPink = androidx.compose.ui.graphics.Color(0xFFD83D61)
private val TextPrimary = androidx.compose.ui.graphics.Color(0xFF333333)
private val TextSecondary = androidx.compose.ui.graphics.Color(0xFF777777)
private val CardBg = androidx.compose.ui.graphics.Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedtimeScreen(viewModel: BedtimeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = PinkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kitty 睡前故事",
                        fontWeight = FontWeight.Bold,
                        color = KittyPink
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PinkBg
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---- Input area ----
            OutlinedTextField(
                value = state.inputText,
                onValueChange = { viewModel.updateInput(it) },
                placeholder = {
                    Text("想听什么故事呀？", color = TextSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KittyPink,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // ---- Quick-fill chips ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickChip("小猫 三分钟") { viewModel.quickFill("小猫 三分钟") }
                QuickChip("月亮 五分钟") { viewModel.quickFill("月亮 五分钟") }
                QuickChip("小兔子和星星") { viewModel.quickFill("小兔子和星星") }
            }

            // ---- Tell Story button ----
            Button(
                onClick = { viewModel.generateStory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KittyPink,
                    disabledContainerColor = KittyPink.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = CardBg,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Kitty 正在准备故事...", color = CardBg)
                } else {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = CardBg)
                    Spacer(Modifier.width(8.dp))
                    Text("讲故事", color = CardBg, fontSize = 16.sp)
                }
            }

            // ---- Status message ----
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (!state.errorMessage.isNullOrBlank()) KittyDarkPink else TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // ---- Story card ----
            if (state.title.isNotBlank() || state.storyText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        if (state.title.isNotBlank()) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = KittyPink
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (state.storyText.isNotBlank()) {
                            Text(
                                text = state.storyText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 26.sp
                            )
                        }
                    }
                }
            }

            // ---- Audio controls ----
            if (state.audioUrl != null || state.storyText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "播放控制",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PlayerButton(
                                icon = Icons.Default.PlayArrow,
                                label = "播放",
                                enabled = state.audioUrl != null &&
                                        state.playerState != AudioPlayer.PlayerState.PLAYING
                            ) { viewModel.play() }

                            PlayerButton(
                                icon = Icons.Default.Pause,
                                label = "暂停",
                                enabled = state.playerState == AudioPlayer.PlayerState.PLAYING
                            ) { viewModel.pause() }

                            PlayerButton(
                                icon = Icons.Default.SkipNext,
                                label = "继续",
                                enabled = state.playerState == AudioPlayer.PlayerState.PAUSED
                            ) { viewModel.resume() }

                            PlayerButton(
                                icon = Icons.Default.Stop,
                                label = "停止",
                                enabled = state.playerState == AudioPlayer.PlayerState.PLAYING ||
                                        state.playerState == AudioPlayer.PlayerState.PAUSED
                            ) { viewModel.stop() }

                            PlayerButton(
                                icon = Icons.Default.Replay,
                                label = "重播",
                                enabled = state.audioUrl != null
                            ) { viewModel.replay() }
                        }
                    }
                }
            }

            // ---- Debug info (compact) ----
            if (state.storyId != null) {
                Text(
                    text = "story_id: ${state.storyId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Error snackbar-like display
            val errorMsg = state.errorMessage
            if (!errorMsg.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = KittyPink.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = KittyPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = errorMsg,
                            color = KittyDarkPink,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = KittyPink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        shape = RoundedCornerShape(20.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = CardBg,
            labelColor = KittyPink
        )
    )
}

@Composable
private fun PlayerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) KittyPink else TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) TextSecondary else TextSecondary.copy(alpha = 0.4f)
        )
    }
}
