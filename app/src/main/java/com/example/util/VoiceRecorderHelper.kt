package com.example.util

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class VoiceRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _currentAmplitudes = MutableStateFlow<List<Float>>(List(16) { 0.15f })
    val currentAmplitudes: StateFlow<List<Float>> = _currentAmplitudes.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    data class RecordingResult(
        val audioFile: File,
        val durationSec: Int,
        val recognizedText: String
    )

    fun startRecording(): Boolean {
        if (_isRecording.value) return false

        val dir = File(context.filesDir, "voice_notes")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        currentOutputFile = file
        _recognizedText.value = ""
        _recordingDurationSec.value = 0

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordStartTime = SystemClock.elapsedRealtime()
            _isRecording.value = true

            startAmplitudePolling()
            startSpeechRecognition()

            true
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Failed to start audio recording", e)
            cancelRecording()
            false
        }
    }

    private fun startAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            val history = ArrayDeque<Float>(List(16) { 0.15f })
            while (_isRecording.value) {
                val elapsed = ((SystemClock.elapsedRealtime() - recordStartTime) / 1000).toInt()
                _recordingDurationSec.value = elapsed

                val maxAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (_: Exception) { 0 }

                // Normalize 0..32767 to 0.15 .. 1.0
                val normalized = (maxAmp / 28000f).coerceIn(0.12f, 1.0f)
                if (history.size >= 16) history.removeFirst()
                history.addLast(normalized)
                _currentAmplitudes.value = history.toList()

                delay(80)
            }
        }
    }

    private fun startSpeechRecognition() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer?.destroy()
                    val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } else {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }

                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            Log.w("VoiceRecorderHelper", "Speech recognition error code: $error")
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val best = matches[0]
                                if (best.isNotBlank()) {
                                    _recognizedText.value = best.trim()
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val partial = matches[0]
                                if (partial.isNotBlank()) {
                                    _recognizedText.value = partial.trim()
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })

                    val lang = Locale.getDefault().toLanguageTag().ifBlank { "ru-RU" }
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }
                    recognizer.startListening(intent)
                    speechRecognizer = recognizer
                }
            } catch (e: Exception) {
                Log.w("VoiceRecorderHelper", "Could not start SpeechRecognizer", e)
            }
        }
    }

    fun stopRecording(): RecordingResult? {
        if (!_isRecording.value) return null
        _isRecording.value = false
        amplitudeJob?.cancel()

        val elapsedSec = ((SystemClock.elapsedRealtime() - recordStartTime) / 1000).toInt().coerceAtLeast(1)

        val activeRecognizer = speechRecognizer
        try {
            activeRecognizer?.stopListening()
        } catch (_: Exception) {}

        mainHandler.postDelayed({
            try {
                activeRecognizer?.destroy()
                if (speechRecognizer == activeRecognizer) {
                    speechRecognizer = null
                }
            } catch (_: Exception) {}
        }, 1500)

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Error stopping MediaRecorder", e)
        }

        val file = currentOutputFile
        currentOutputFile = null

        return if (file != null && file.exists() && file.length() > 0) {
            RecordingResult(
                audioFile = file,
                durationSec = elapsedSec,
                recognizedText = _recognizedText.value.trim()
            )
        } else {
            file?.delete()
            null
        }
    }

    fun cancelRecording() {
        _isRecording.value = false
        amplitudeJob?.cancel()

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null

        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
        _recognizedText.value = ""
        _recordingDurationSec.value = 0
    }

    fun release() {
        cancelRecording()
        scope.cancel()
    }
}
