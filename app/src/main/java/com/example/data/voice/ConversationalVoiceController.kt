package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.AppSettings
import com.example.data.model.VoiceEngineMode
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class ConversationalVoiceController(
    private val context: Context,
    private val voiceCommandDispatcher: VoiceCommandDispatcher,
    private val preferencesRepository: PreferencesRepository
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _assistantSpokenText = MutableStateFlow("")
    val assistantSpokenText: StateFlow<String> = _assistantSpokenText.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<VoiceTurn>>(emptyList())
    val conversationHistory: StateFlow<List<VoiceTurn>> = _conversationHistory.asStateFlow()

    private val _isOutputMuted = MutableStateFlow(false)
    val isOutputMuted: StateFlow<Boolean> = _isOutputMuted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var processingJob: Job? = null
    private var isSessionActive = false
    private var activeSettings: AppSettings = AppSettings()

    init {
        initSpeechAndTts()
        controllerScope.launch {
            preferencesRepository.settingsFlow.collect { settings ->
                activeSettings = settings
                _isOutputMuted.value = !settings.isSpeechOutputEnabled
            }
        }
    }

    private fun initSpeechAndTts() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@ConversationalVoiceController)
            }
        }
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        if (isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                            // Automatically transition to listening for the next turn!
                            startListening()
                        } else {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        if (isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                            startListening()
                        } else {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }
                }
            })
            isTtsReady = true
        } else {
            isTtsReady = false
        }
    }

    fun startSession() {
        isSessionActive = true
        _errorMessage.value = null
        _voiceState.value = VoiceState.CONNECTING
        
        mainHandler.postDelayed({
            startListening()
        }, 300)
    }

    fun startListening() {
        if (!isSessionActive) return
        if (speechRecognizer == null) {
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = "Speech recognition service is unavailable on this device."
            return
        }

        // Interrupt any ongoing TTS
        tts?.stop()
        _currentTranscript.value = ""
        _audioAmplitude.value = 0f
        _voiceState.value = VoiceState.LISTENING

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, activeSettings.selectedLanguage.code)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = e.localizedMessage ?: "Failed to start audio recording."
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun interrupt() {
        processingJob?.cancel()
        tts?.stop()
        _voiceState.value = VoiceState.INTERRUPTED
        
        if (isSessionActive) {
            mainHandler.postDelayed({
                startListening()
            }, 250)
        }
    }

    fun toggleOutputMute() {
        val next = !_isOutputMuted.value
        _isOutputMuted.value = next
        if (next) {
            tts?.stop()
            if (_voiceState.value == VoiceState.SPEAKING && isSessionActive) {
                startListening()
            }
        }
    }

    fun endSession() {
        isSessionActive = false
        processingJob?.cancel()
        speechRecognizer?.cancel()
        tts?.stop()
        _voiceState.value = VoiceState.CLOSED
        _audioAmplitude.value = 0f
    }

    // RecognitionListener Callbacks
    override fun onRmsChanged(rmsdB: Float) {
        if (_voiceState.value == VoiceState.LISTENING) {
            // Normalize -2dB..10dB to 0..1f
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _audioAmplitude.value = normalized
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _currentTranscript.value = matches[0]
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""

        if (text.isNotBlank()) {
            _currentTranscript.value = text
            processUserTurn(text)
        } else {
            // If empty in hands-free mode, keep listening
            if (isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                mainHandler.postDelayed({ startListening() }, 300)
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    private fun processUserTurn(text: String) {
        _voiceState.value = VoiceState.PROCESSING
        _audioAmplitude.value = 0f

        // Record User Turn
        val userTurn = VoiceTurn(speaker = "user", text = text)
        _conversationHistory.value = _conversationHistory.value + userTurn

        processingJob = controllerScope.launch {
            _voiceState.value = VoiceState.GENERATING
            val result = voiceCommandDispatcher.dispatchVoiceInput(text, _conversationHistory.value)

            val (responseText, actionType, actionDetail) = when (result) {
                is VoiceCommandResult.SpokenAnswer -> Triple(result.text, null, null)
                is VoiceCommandResult.TaskCreated -> Triple(result.text, "TASK_CREATED", result.task.title)
                is VoiceCommandResult.ImageCreated -> Triple(result.text, "IMAGE_CREATED", result.imagePath)
                is VoiceCommandResult.SearchAnswer -> Triple(result.text, "SEARCH_ANSWER", "${result.sourcesCount} sources")
                is VoiceCommandResult.AutomationProposed -> Triple(result.text, "AUTOMATION_PROPOSED", result.title)
                is VoiceCommandResult.Error -> Triple(result.text, "ERROR", null)
            }

            _assistantSpokenText.value = responseText

            val assistantTurn = VoiceTurn(
                speaker = "assistant",
                text = responseText,
                actionType = actionType,
                actionDetail = actionDetail
            )
            _conversationHistory.value = _conversationHistory.value + assistantTurn

            // Speak response aloud if not muted
            if (!_isOutputMuted.value && isTtsReady && tts != null) {
                _voiceState.value = VoiceState.SPEAKING
                tts?.setSpeechRate(activeSettings.speechRate)
                tts?.setPitch(activeSettings.speechPitch)
                val utteranceId = "MAX_VOICE_${System.currentTimeMillis()}"
                tts?.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                // If muted, wait a brief duration to let user read, then resume hands-free listening
                _voiceState.value = VoiceState.IDLE
                if (isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                    mainHandler.postDelayed({ startListening() }, 2000)
                }
            }
        }
    }

    override fun onError(error: Int) {
        _audioAmplitude.value = 0f
        if (!isSessionActive) return

        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // In continuous hands-free mode, rearm listening seamlessly
                if (activeSettings.isHandsFreeLoopEnabled && _voiceState.value == VoiceState.LISTENING) {
                    mainHandler.postDelayed({ startListening() }, 400)
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                mainHandler.postDelayed({ startListening() }, 600)
            }
            else -> {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording hardware error."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    else -> "Speech recognition error ($error)."
                }
                _errorMessage.value = message
                _voiceState.value = VoiceState.ERROR
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        endSession()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        controllerScope.cancel()
    }
}
