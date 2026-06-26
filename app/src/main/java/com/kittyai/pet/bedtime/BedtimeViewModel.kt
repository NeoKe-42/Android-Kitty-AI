package com.kittyai.pet.bedtime

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

/**
 * UI state for the unified chat screen.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = listOf(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String = "想和 Kitty 说什么？",
    val errorMessage: String? = null,
    val currentAudioUrl: String? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false
)

/**
 * ViewModel for the unified Kitty AI chat screen.
 * Handles both normal chat and bedtime story via POST /api/chat.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val api: KittyApiService = KittyApiService.create()
    val audioPlayer: AudioPlayer = AudioPlayer(application)

    init {
        audioPlayer.onStateChanged = { newState ->
            _uiState.update {
                it.copy(
                    isPlaying = newState == AudioPlayer.PlayerState.PLAYING,
                    isPaused = newState == AudioPlayer.PlayerState.PAUSED
                )
            }
            updateStatusFromPlayerState(newState)
        }
        audioPlayer.onError = { msg ->
            _uiState.update { it.copy(errorMessage = msg, statusMessage = msg) }
        }
    }

    private fun updateStatusFromPlayerState(state: AudioPlayer.PlayerState) {
        val msg = when (state) {
            AudioPlayer.PlayerState.PLAYING -> "正在播放"
            AudioPlayer.PlayerState.PAUSED -> "已暂停"
            AudioPlayer.PlayerState.STOPPED -> "已停止"
            AudioPlayer.PlayerState.ENDED -> "故事讲完了，晚安呀"
            AudioPlayer.PlayerState.BUFFERING -> "Kitty 正在准备声音..."
            else -> null
        }
        if (msg != null) {
            _uiState.update { it.copy(statusMessage = msg) }
        }
    }

    // ---- user actions ----

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun sendMessage() {
        val state = _uiState.value
        if (state.isLoading) {
            Log.d(TAG, "Already loading, ignoring duplicate request")
            return
        }

        val message = state.inputText.trim().ifBlank { ApiConfig.DEFAULT_MESSAGE }

        // Add user message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            text = message
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                inputText = "",
                isLoading = true,
                errorMessage = null,
                statusMessage = "Kitty 正在想..."
            )
        }

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.sendChatMessage(ChatRequest(message))
                }

                if (!response.isSuccessful) {
                    handleHttpError(response.code())
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    handleError("Kitty 刚刚没讲出来，再试一次好不好？")
                    return@launch
                }

                if (body.success != true) {
                    handleError(body.error ?: "Kitty 走神了，再试一次好不好？")
                    return@launch
                }

                val mode = body.mode ?: "chat"
                val reply = body.reply ?: ""
                val title = body.title
                val audioUrl = body.audioUrl

                // Add Kitty message
                val kittyMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.KITTY,
                    text = reply,
                    title = title,
                    mode = mode,
                    audioUrl = audioUrl,
                    storyId = body.storyId
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + kittyMsg,
                        isLoading = false,
                        statusMessage = if (mode == "bedtime") "故事准备好了" else "",
                        currentAudioUrl = audioUrl,
                        errorMessage = null
                    )
                }

                // Auto-play for bedtime stories
                if (mode == "bedtime" && !audioUrl.isNullOrBlank()) {
                    audioPlayer.prepare(audioUrl)
                    audioPlayer.play()
                }

                Log.d(TAG, "Reply ready: mode=$mode, audioUrl=$audioUrl")

            } catch (e: UnknownHostException) {
                handleError("网络好像不太好，Kitty 连不上服务器。")
            } catch (e: ConnectException) {
                handleError("网络好像不太好，Kitty 连不上服务器。")
            } catch (e: SocketTimeoutException) {
                handleError("Kitty 等了好久都没等到回复，再试一次好不好？")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                handleError("唔，Kitty 刚刚没讲出来，再试一次好不好？")
            }
        }
    }

    fun sendQuickMessage(text: String) {
        _uiState.update { it.copy(inputText = text) }
        sendMessage()
    }

    private fun handleHttpError(code: Int) {
        val msg = when {
            code in 500..599 -> "Kitty 的服务器刚刚打了个喷嚏，再试一次好不好？"
            code == 404 -> "Kitty 找不到这条路了。"
            else -> "网络好像不太好，Kitty 连不上服务器。"
        }
        handleError(msg)
    }

    private fun handleError(msg: String) {
        // Add error as a Kitty message
        val errorMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.KITTY,
            text = msg
        )
        _uiState.update {
            it.copy(
                messages = it.messages + errorMsg,
                isLoading = false,
                errorMessage = msg,
                statusMessage = msg
            )
        }
    }

    // ---- audio controls ----

    fun play(audioUrl: String?) {
        val url = audioUrl ?: _uiState.value.currentAudioUrl
        if (url != null) {
            audioPlayer.prepare(url)
            audioPlayer.play()
        } else {
            _uiState.update {
                it.copy(errorMessage = "还没有声音可以播放哦~", statusMessage = "还没有声音可以播放哦~")
            }
        }
    }

    fun pause() = audioPlayer.pause()
    fun resume() = audioPlayer.resume()
    fun stop() = audioPlayer.stop()
    fun replay() = audioPlayer.replay()

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        Log.d(TAG, "ViewModel cleared, player released")
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
