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

/**
 * UI state for the bedtime story screen.
 */
data class BedtimeUiState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val storyId: String? = null,
    val title: String = "",
    val storyText: String = "",
    val audioUrl: String? = null,
    val statusMessage: String = "想听什么故事呀？",
    val errorMessage: String? = null,
    val playerState: AudioPlayer.PlayerState = AudioPlayer.PlayerState.IDLE
)

/**
 * ViewModel for the bedtime story screen.
 * Owns the AudioPlayer, manages network calls, and exposes UI state.
 */
class BedtimeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BedtimeUiState())
    val uiState: StateFlow<BedtimeUiState> = _uiState.asStateFlow()

    private val api: KittyApiService = KittyApiService.create()
    val audioPlayer: AudioPlayer = AudioPlayer(application)

    init {
        audioPlayer.onStateChanged = { newState ->
            _uiState.update { it.copy(playerState = newState) }
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

    fun quickFill(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun generateStory() {
        val currentState = _uiState.value
        if (currentState.isLoading) {
            Log.d(TAG, "Already loading, ignoring duplicate request")
            return
        }

        // Stop any playing audio first
        audioPlayer.stop()

        val message = currentState.inputText.trim().ifBlank {
            ApiConfig.DEFAULT_MESSAGE
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                statusMessage = "Kitty 正在准备故事...",
                title = "",
                storyText = "",
                audioUrl = null,
                storyId = null,
                playerState = AudioPlayer.PlayerState.IDLE
            )
        }

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.generateBedtimeStory(BedtimeRequest(message))
                }

                if (!response.isSuccessful) {
                    val code = response.code()
                    val errorBody = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "HTTP $code: $errorBody")

                    val friendlyMsg = when {
                        code in 500..599 -> "Kitty 的服务器刚刚打了个喷嚏，再试一次好不好？"
                        code == 404 -> "Kitty 找不到这条路了。"
                        else -> "网络好像不太好，Kitty 连不上服务器。"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = friendlyMsg,
                            statusMessage = friendlyMsg
                        )
                    }
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    val msg = "Kitty 刚刚没讲出来，再试一次好不好？"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = msg,
                            statusMessage = msg
                        )
                    }
                    return@launch
                }

                // Check success flag
                if (body.success != true) {
                    val errMsg = body.error ?: "Kitty 刚刚没讲出来，再试一次好不好？"
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errMsg,
                            statusMessage = errMsg
                        )
                    }
                    return@launch
                }

                // Success — populate UI
                val title = body.title ?: "Kitty 的睡前故事"
                val storyText = body.text ?: ""
                val audioUrl = body.audioUrl

                if (storyText.isBlank() && audioUrl.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "故事内容空空的，Kitty 可能走神了。",
                            statusMessage = "故事内容空空的，Kitty 可能走神了。"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        storyId = body.storyId,
                        title = title,
                        storyText = storyText,
                        audioUrl = audioUrl,
                        statusMessage = "故事准备好了",
                        errorMessage = null
                    )
                }

                // Auto-play audio
                audioPlayer.prepare(audioUrl)
                audioPlayer.play()

                Log.d(TAG, "Story ready: storyId=${body.storyId}, audioUrl=$audioUrl")

            } catch (e: UnknownHostException) {
                handleNetworkError("网络好像不太好，Kitty 连不上服务器。")
            } catch (e: ConnectException) {
                handleNetworkError("网络好像不太好，Kitty 连不上服务器。")
            } catch (e: SocketTimeoutException) {
                handleNetworkError("Kitty 等了好久都没等到故事，再试一次好不好？")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                handleNetworkError("唔，Kitty 刚刚没讲出来，再试一次好不好？")
            }
        }
    }

    private fun handleNetworkError(msg: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = msg,
                statusMessage = msg
            )
        }
    }

    // ---- audio controls ----

    fun play() {
        val url = _uiState.value.audioUrl
        if (url != null) {
            audioPlayer.prepare(url)
            audioPlayer.play()
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = "还没有故事可以播放哦~",
                    statusMessage = "还没有故事可以播放哦~"
                )
            }
        }
    }

    fun pause() {
        audioPlayer.pause()
    }

    fun resume() {
        audioPlayer.resume()
    }

    fun stop() {
        audioPlayer.stop()
    }

    fun replay() {
        audioPlayer.replay()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        Log.d(TAG, "ViewModel cleared, player released")
    }

    companion object {
        private const val TAG = "BedtimeViewModel"
    }
}
