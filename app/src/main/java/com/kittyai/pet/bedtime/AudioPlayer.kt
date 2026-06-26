package com.kittyai.pet.bedtime

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Thin wrapper around Media3 ExoPlayer for bedtime story audio playback.
 *
 * Usage:
 *   val player = AudioPlayer(context)
 *   player.prepare(audioUrl)
 *   player.play()
 *   // later:
 *   player.pause()
 *   player.resume()
 *   player.stop()
 *   player.replay()
 *   player.release()  // in onCleared / DisposableEffect
 */
class AudioPlayer(context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    /** Current player state exposed for UI observation. */
    var onStateChanged: ((PlayerState) -> Unit)? = null

    /** One-shot error callback */
    var onError: ((String) -> Unit)? = null

    private var currentAudioUrl: String? = null

    enum class PlayerState {
        IDLE,
        BUFFERING,
        READY,
        PLAYING,
        PAUSED,
        ENDED,
        STOPPED
    }

    private var _state: PlayerState = PlayerState.IDLE
    val state: PlayerState get() = _state

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val newState = when (playbackState) {
                    Player.STATE_IDLE -> PlayerState.IDLE
                    Player.STATE_BUFFERING -> PlayerState.BUFFERING
                    Player.STATE_READY -> {
                        if (exoPlayer.playWhenReady) PlayerState.PLAYING
                        else PlayerState.READY
                    }
                    Player.STATE_ENDED -> PlayerState.ENDED
                    else -> PlayerState.IDLE
                }
                updateState(newState)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val newState = when {
                    isPlaying -> PlayerState.PLAYING
                    exoPlayer.playbackState == Player.STATE_ENDED -> PlayerState.ENDED
                    else -> _state  // keep current
                }
                updateState(newState)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}", error)
                onError?.invoke("唔，声音好像跑丢了，再试一次好不好？")
                updateState(PlayerState.IDLE)
            }
        })
    }

    private fun updateState(newState: PlayerState) {
        if (newState != _state) {
            _state = newState
            onStateChanged?.invoke(newState)
        }
    }

    fun prepare(audioUrl: String?) {
        if (audioUrl.isNullOrBlank()) {
            onError?.invoke("故事写好了，但是声音还没准备好。")
            return
        }
        if (!audioUrl.startsWith("http://") && !audioUrl.startsWith("https://")) {
            onError?.invoke("唔，声音链接不太对，再试一次好不好？")
            return
        }

        // Stop any existing playback
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        currentAudioUrl = audioUrl
        val mediaItem = MediaItem.fromUri(audioUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        updateState(PlayerState.BUFFERING)

        Log.d(TAG, "Prepared: $audioUrl")
    }

    fun play() {
        exoPlayer.play()
        Log.d(TAG, "play() called, playWhenReady=${exoPlayer.playWhenReady}")
    }

    fun pause() {
        exoPlayer.pause()
        updateState(PlayerState.PAUSED)
    }

    fun resume() {
        exoPlayer.play()
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentAudioUrl = null
        updateState(PlayerState.STOPPED)
    }

    fun replay() {
        if (currentAudioUrl != null) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            val mediaItem = MediaItem.fromUri(currentAudioUrl!!)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    fun seekToStart() {
        exoPlayer.seekTo(0)
    }

    fun release() {
        exoPlayer.release()
        Log.d(TAG, "Player released")
    }

    companion object {
        private const val TAG = "AudioPlayer"
    }
}
