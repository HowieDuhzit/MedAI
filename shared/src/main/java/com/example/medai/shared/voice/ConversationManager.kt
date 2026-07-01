package com.example.medai.shared.voice

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConversationState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class ConversationManager(appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var speechRecognitionManager: SpeechRecognitionManager? = null
    private var ollamaApiClient: OllamaApiClient? = null
    private var ttsPlayer: TtsPlayer? = null
    private val context = appContext

    private val _state = MutableStateFlow(ConversationState.IDLE)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val _transcript = MutableStateFlow<String?>(null)
    val transcript: StateFlow<String?> = _transcript.asStateFlow()

    private val _response = MutableStateFlow<String?>(null)
    val response: StateFlow<String?> = _response.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var conversationHistory = mutableListOf<Pair<String, String>>()
    private var processingJob: Job? = null

    var ollamaBaseUrl: String = "http://10.0.2.2:11434/"
    var modelName: String = "llama3.2"

    fun initialize() {
        speechRecognitionManager = SpeechRecognitionManager(context).apply {
            initialize()
        }

        ollamaApiClient = OllamaApiClient(ollamaBaseUrl)

        ttsPlayer = TtsPlayer(context).apply {
            initialize()
        }

        scope.launch {
            delay(500)
            _isReady.value = true
        }
    }

    fun startListening() {
        if (_state.value != ConversationState.IDLE && _state.value != ConversationState.ERROR) {
            return
        }

        _error.value = null
        _transcript.value = null
        _response.value = null

        _state.value = ConversationState.LISTENING
        speechRecognitionManager?.startListening()

        scope.launch {
            speechRecognitionManager?.isListening?.collect { isListening ->
                if (!isListening && _state.value == ConversationState.LISTENING) {
                    val transcriptValue = speechRecognitionManager?.transcript?.value
                    if (transcriptValue != null) {
                        _transcript.value = transcriptValue
                        processWithOllama(transcriptValue)
                    } else if (speechRecognitionManager?.error?.value != null) {
                        _error.value = speechRecognitionManager?.error?.value
                        _state.value = ConversationState.ERROR
                        scheduleNextListening()
                    } else {
                        _state.value = ConversationState.IDLE
                    }
                }
            }
        }
    }

    fun stopListening() {
        speechRecognitionManager?.stopListening()
        if (_state.value == ConversationState.LISTENING) {
            _state.value = ConversationState.IDLE
        }
    }

    private fun processWithOllama(userMessage: String) {
        _state.value = ConversationState.PROCESSING

        processingJob = scope.launch {
            try {
                val fullContext = buildContext()
                val contextMessage = if (fullContext.isNotEmpty()) {
                    "$fullContext\n\nUser: $userMessage"
                } else {
                    userMessage
                }

                val responseText = ollamaApiClient?.chat(modelName, contextMessage, false)
                    ?: throw Exception("Ollama client not initialized")

                responseText.collect { text ->
                    _response.value = text
                    conversationHistory.add(userMessage to text)
                    speakResponse(text)
                }
            } catch (e: Exception) {
                _error.value = "Failed to get response: ${e.message}"
                _state.value = ConversationState.ERROR
                scheduleNextListening()
            }
        }
    }

    private fun buildContext(): String {
        if (conversationHistory.isEmpty()) return ""
        return conversationHistory.takeLast(5).joinToString("\n") { (user, assistant) ->
            "User: $user\nAssistant: $assistant"
        }
    }

    private fun speakResponse(text: String) {
        _state.value = ConversationState.SPEAKING
        ttsPlayer?.speak(text) {
            _state.value = ConversationState.IDLE
            // For Automotive, we might not want to automatically schedule next listening
            // to avoid distraction, or we might want to.
            // scheduleNextListening()
        }
    }

    private fun scheduleNextListening() {
        scope.launch {
            delay(500)
            if (_state.value == ConversationState.IDLE || _state.value == ConversationState.ERROR) {
                startListening()
            }
        }
    }

    fun stopConversation() {
        processingJob?.cancel()
        speechRecognitionManager?.stopListening()
        ttsPlayer?.stop()
        _state.value = ConversationState.IDLE
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    fun destroy() {
        processingJob?.cancel()
        speechRecognitionManager?.destroy()
        ttsPlayer?.destroy()
        _state.value = ConversationState.IDLE
    }
}
