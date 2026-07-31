package com.example.priv.ui.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioPlaybackState(
    val memoryId: String? = null,
    val attachmentId: String? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f
)

object AudioPlayerManager {

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun playOrPause(memoryId: String, attachmentId: String, totalDurationMs: Long) {
        val currentState = _playbackState.value
        if (currentState.memoryId == memoryId && currentState.attachmentId == attachmentId) {
            if (currentState.isPlaying) {
                pause()
            } else {
                resume()
            }
        } else {
            // Start new playback
            playbackJob?.cancel()
            val initialDuration = if (totalDurationMs > 0) totalDurationMs else 30000L
            _playbackState.value = AudioPlaybackState(
                memoryId = memoryId,
                attachmentId = attachmentId,
                isPlaying = true,
                progress = 0f,
                currentPositionMs = 0L,
                totalDurationMs = initialDuration,
                playbackSpeed = currentState.playbackSpeed
            )
            startTicker()
        }
    }

    fun pause() {
        playbackJob?.cancel()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun resume() {
        val state = _playbackState.value
        if (state.memoryId != null) {
            _playbackState.value = state.copy(isPlaying = true)
            startTicker()
        }
    }

    fun seekTo(progressFraction: Float) {
        val state = _playbackState.value
        val newPos = (state.totalDurationMs * progressFraction.coerceIn(0f, 1f)).toLong()
        _playbackState.value = state.copy(
            progress = progressFraction.coerceIn(0f, 1f),
            currentPositionMs = newPos
        )
    }

    fun toggleSpeed() {
        val currentSpeed = _playbackState.value.playbackSpeed
        val nextSpeed = when (currentSpeed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _playbackState.value = _playbackState.value.copy(playbackSpeed = nextSpeed)
    }

    private fun startTicker() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive) {
                delay(100)
                val state = _playbackState.value
                if (!state.isPlaying) break

                val increment = (100 * state.playbackSpeed).toLong()
                val newPos = state.currentPositionMs + increment

                if (newPos >= state.totalDurationMs) {
                    _playbackState.value = state.copy(
                        isPlaying = false,
                        progress = 1.0f,
                        currentPositionMs = state.totalDurationMs
                    )
                    break
                } else {
                    val newProgress = newPos.toFloat() / state.totalDurationMs
                    _playbackState.value = state.copy(
                        progress = newProgress,
                        currentPositionMs = newPos
                    )
                }
            }
        }
    }
}
