package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object PluginAudioManager {
    private const val TAG = "PluginAudioManager"

    private var appContext: Context? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val activeMediaPlayers = ConcurrentHashMap<String, MediaPlayer>()
    private val soundToPluginMap = ConcurrentHashMap<String, String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var toneGenerator: ToneGenerator? = null

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init ToneGenerator: ${e.message}")
        }

        try {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    textToSpeech?.language = Locale("ru", "RU")
                } else {
                    Log.w(TAG, "TextToSpeech init failed with status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init TextToSpeech: ${e.message}")
        }
    }

    fun playSound(
        soundOrPreset: String,
        volume: Float = 1.0f,
        soundId: String = "sfx_${System.currentTimeMillis()}",
        pluginId: String = ""
    ) {
        val context = appContext ?: return
        if (pluginId.isNotBlank()) {
            soundToPluginMap[soundId] = pluginId
        }

        when (soundOrPreset.lowercase().trim()) {
            "pop", "click" -> {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                } catch (_: Exception) {}
            }
            "bell", "ding" -> {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                } catch (_: Exception) {}
            }
            "alert", "warn" -> {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
                } catch (_: Exception) {}
            }
            "levelup", "success" -> {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 220)
                } catch (_: Exception) {}
            }
            else -> {
                // Play from web URL or local file URI
                mainHandler.post {
                    try {
                        activeMediaPlayers[soundId]?.release()
                        val player = MediaPlayer().apply {
                            setAudioStreamType(AudioManager.STREAM_MUSIC)
                            setDataSource(context, Uri.parse(soundOrPreset))
                            setVolume(volume.coerceIn(0.0f, 1.0f), volume.coerceIn(0.0f, 1.0f))
                            setOnCompletionListener {
                                it.release()
                                activeMediaPlayers.remove(soundId)
                                soundToPluginMap.remove(soundId)
                            }
                            setOnErrorListener { mp, _, _ ->
                                mp.release()
                                activeMediaPlayers.remove(soundId)
                                soundToPluginMap.remove(soundId)
                                true
                            }
                            prepareAsync()
                            setOnPreparedListener { it.start() }
                        }
                        activeMediaPlayers[soundId] = player
                    } catch (e: Exception) {
                        Log.e(TAG, "Error playing audio $soundOrPreset: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopSound(soundId: String) {
        soundToPluginMap.remove(soundId)
        activeMediaPlayers.remove(soundId)?.let { player ->
            try {
                if (player.isPlaying) player.stop()
                player.release()
            } catch (_: Exception) {}
        }
    }

    fun stopForPlugin(pluginId: String) {
        if (pluginId.isBlank()) return
        val targetSounds = soundToPluginMap.filterValues { it == pluginId }.keys.toList()
        targetSounds.forEach { sid ->
            stopSound(sid)
        }
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
    }

    fun stopAll() {
        activeMediaPlayers.forEach { (_, player) ->
            try {
                if (player.isPlaying) player.stop()
                player.release()
            } catch (_: Exception) {}
        }
        activeMediaPlayers.clear()
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
    }

    fun speakText(text: String, lang: String = "ru") {
        if (!isTtsInitialized || textToSpeech == null) return
        mainHandler.post {
            try {
                val locale = when (lang.lowercase()) {
                    "en" -> Locale.US
                    "ja" -> Locale.JAPAN
                    else -> Locale("ru", "RU")
                }
                textToSpeech?.language = locale
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lua_tts_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e(TAG, "TTS speak error: ${e.message}")
            }
        }
    }
}
