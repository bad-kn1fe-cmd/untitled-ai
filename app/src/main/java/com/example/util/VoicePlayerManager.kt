package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object VoicePlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0)
    val currentPositionSec: StateFlow<Int> = _currentPositionSec.asStateFlow()

    fun togglePlay(context: Context, filePath: String) {
        if (_currentPlayingPath.value == filePath && _isPlaying.value) {
            pause()
        } else if (_currentPlayingPath.value == filePath && !_isPlaying.value && mediaPlayer != null) {
            resume()
        } else {
            play(context, filePath)
        }
    }

    fun play(context: Context, filePath: String) {
        stop()

        val file = File(filePath)
        if (!file.exists()) {
            Log.w("VoicePlayerManager", "Audio file not found: $filePath")
            return
        }

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                start()
            }

            mediaPlayer = player
            _currentPlayingPath.value = filePath
            _isPlaying.value = true
            _progress.value = 0f
            _currentPositionSec.value = 0

            player.setOnCompletionListener {
                _isPlaying.value = false
                _progress.value = 0f
                _currentPositionSec.value = 0
                progressJob?.cancel()
            }

            startProgressUpdates()
        } catch (e: Exception) {
            Log.e("VoicePlayerManager", "Failed to play audio: $filePath", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } catch (e: Exception) {
            Log.e("VoicePlayerManager", "Error pausing playback", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressUpdates()
        } catch (e: Exception) {
            Log.e("VoicePlayerManager", "Error resuming playback", e)
        }
    }

    fun seekTo(progressFraction: Float) {
        val player = mediaPlayer ?: return
        try {
            val duration = player.duration
            if (duration > 0) {
                val targetMs = (duration * progressFraction.coerceIn(0f, 1f)).toInt()
                player.seekTo(targetMs)
                _progress.value = progressFraction.coerceIn(0f, 1f)
                _currentPositionSec.value = targetMs / 1000
            }
        } catch (e: Exception) {
            Log.e("VoicePlayerManager", "Error seeking playback", e)
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingPath.value = null
        _progress.value = 0f
        _currentPositionSec.value = 0
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_isPlaying.value) {
                val player = mediaPlayer
                if (player != null) {
                    try {
                        val current = player.currentPosition
                        val duration = player.duration
                        if (duration > 0) {
                            _progress.value = (current.toFloat() / duration).coerceIn(0f, 1f)
                            _currentPositionSec.value = current / 1000
                        }
                    } catch (_: Exception) {}
                }
                delay(60)
            }
        }
    }
}
