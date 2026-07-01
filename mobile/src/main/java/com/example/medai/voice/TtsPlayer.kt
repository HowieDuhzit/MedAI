package com.example.medai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsPlayer(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var onSpeechCompleteListener: (() -> Unit)? = null

    fun initialize() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        onSpeechCompleteListener?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _error.value = "TTS playback error"
                        onSpeechCompleteListener?.invoke()
                    }
                })

                _isReady.value = true
            } else {
                _error.value = "TTS init failed: $status"
            }
        }
    }

    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (!isInitialized) {
            _error.value = "TTS not initialized"
            onComplete()
            return
        }

        if (_isSpeaking.value) {
            tts?.stop()
        }

        onSpeechCompleteListener = onComplete
        val utteranceId = UUID.randomUUID().toString()

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isReady.value = false
    }
}