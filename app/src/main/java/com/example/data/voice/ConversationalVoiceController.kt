package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.AppSettings
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ConversationalVoiceController(
    private val context: Context,
    private val voiceCommandDispatcher: VoiceCommandDispatcher,
    private val preferencesRepository: PreferencesRepository
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

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

    // Protections against audio repeating and loops
    private var lastSpokenAssistantText: String = ""
    private var isStoppingTtsIntentionally = false
    private var consecutiveBusyErrors = 0
    private var startListeningRunnable: Runnable? = null

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
                    mainHandler.post {
                        _voiceState.value = VoiceState.SPEAKING
                        // Stop recognizer while TTS is speaking to prevent picking up assistant voice
                        try {
                            speechRecognizer?.stopListening()
                            speechRecognizer?.cancel()
                        } catch (e: Exception) {}
                    }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        abandonAudioFocus()
                        if (isSessionActive && activeSettings.isHandsFreeLoopEnabled && !isStoppingTtsIntentionally) {
                            // Cooldown delay so acoustic echo clears before mic opens
                            scheduleStartListening(650)
                        } else {
                            if (_voiceState.value == VoiceState.SPEAKING) {
                                _voiceState.value = VoiceState.IDLE
                            }
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        abandonAudioFocus()
                        if (isStoppingTtsIntentionally) {
                            return@post
                        }
                        if (isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                            scheduleStartListening(800)
                        } else {
                            if (_voiceState.value == VoiceState.SPEAKING) {
                                _voiceState.value = VoiceState.IDLE
                            }
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
        scheduleStartListening(300)
    }

    fun startListening() {
        if (!isSessionActive) return

        // 1. Verify Microphone Permission at Runtime
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            _voiceState.value = VoiceState.REQUESTING_PERMISSION
            _errorMessage.value = "Microphone permission is required for voice commands."
            VoiceLogger.logPermissionStatus(android.Manifest.permission.RECORD_AUDIO, false)
            return
        }

        if (speechRecognizer == null) {
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = "Speech recognition service is unavailable on this device."
            VoiceLogger.logError("SpeechRecognizer", "Service unavailable on device")
            return
        }

        if (_voiceState.value == VoiceState.SPEAKING || _voiceState.value == VoiceState.THINKING) {
            return
        }

        cancelPendingStartListening()

        if (tts?.isSpeaking == true) {
            isStoppingTtsIntentionally = true
            try {
                tts?.stop()
            } finally {
                mainHandler.postDelayed({ isStoppingTtsIntentionally = false }, 300)
            }
        }

        requestAudioFocus()
        _currentTranscript.value = ""
        _audioAmplitude.value = 0f
        _voiceState.value = VoiceState.LISTENING
        VoiceLogger.logSessionStart("Conversational Listening")

        val langCode = activeSettings.selectedLanguage.code
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = e.localizedMessage ?: "Failed to start audio recording."
            VoiceLogger.logError("SpeechRecognizer", e.message ?: "startListening failed")
        }
    }

    fun stopListening() {
        cancelPendingStartListening()
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {}
        if (_voiceState.value == VoiceState.LISTENING) {
            _voiceState.value = VoiceState.IDLE
        }
        abandonAudioFocus()
    }

    fun interrupt() {
        cancelPendingStartListening()
        processingJob?.cancel()
        isStoppingTtsIntentionally = true
        try {
            tts?.stop()
        } finally {
            mainHandler.postDelayed({ isStoppingTtsIntentionally = false }, 300)
        }
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {}

        _voiceState.value = VoiceState.INTERRUPTED
        abandonAudioFocus()

        if (isSessionActive) {
            scheduleStartListening(400)
        }
    }

    fun toggleOutputMute() {
        val next = !_isOutputMuted.value
        _isOutputMuted.value = next
        if (next) {
            isStoppingTtsIntentionally = true
            try {
                tts?.stop()
            } finally {
                mainHandler.postDelayed({ isStoppingTtsIntentionally = false }, 300)
            }
            if (_voiceState.value == VoiceState.SPEAKING && isSessionActive) {
                scheduleStartListening(400)
            }
        }
    }

    fun endSession() {
        isSessionActive = false
        cancelPendingStartListening()
        processingJob?.cancel()
        isStoppingTtsIntentionally = true
        try {
            tts?.stop()
        } finally {
            isStoppingTtsIntentionally = false
        }
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {}
        abandonAudioFocus()
        _voiceState.value = VoiceState.CLOSED
        _audioAmplitude.value = 0f
    }

    private fun scheduleStartListening(delayMs: Long) {
        cancelPendingStartListening()
        if (!isSessionActive) return
        val runnable = Runnable {
            if (isSessionActive && _voiceState.value != VoiceState.SPEAKING && _voiceState.value != VoiceState.THINKING && _voiceState.value != VoiceState.PROCESSING_AUDIO) {
                startListening()
            }
        }
        startListeningRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelPendingStartListening() {
        startListeningRunnable?.let { mainHandler.removeCallbacks(it) }
        startListeningRunnable = null
    }

    private fun isSelfEcho(recognizedText: String): Boolean {
        if (lastSpokenAssistantText.isBlank() || recognizedText.isBlank()) return false

        val cleanInput = recognizedText.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
        val cleanSpoken = lastSpokenAssistantText.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()

        if (cleanInput.isEmpty() || cleanSpoken.isEmpty()) return false

        if (cleanSpoken.contains(cleanInput) || cleanInput.contains(cleanSpoken)) return true

        val inputWords = cleanInput.split("\\s+".toRegex()).filter { it.length > 2 }.toSet()
        val spokenWords = cleanSpoken.split("\\s+".toRegex()).filter { it.length > 2 }.toSet()

        if (inputWords.isEmpty() || spokenWords.isEmpty()) return false

        val overlap = inputWords.intersect(spokenWords).size
        val minSize = minOf(inputWords.size, spokenWords.size)

        return minSize > 0 && (overlap.toFloat() / minSize.toFloat()) >= 0.65f
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (e: Exception) {}
    }

    private fun abandonAudioFocus() {
        try {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        } catch (e: Exception) {}
    }

    // RecognitionListener Callbacks
    override fun onRmsChanged(rmsdB: Float) {
        if (_voiceState.value == VoiceState.LISTENING) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _audioAmplitude.value = normalized
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""
        if (text.isNotBlank() && !isSelfEcho(text)) {
            _currentTranscript.value = text
        }
    }

    override fun onResults(results: Bundle?) {
        consecutiveBusyErrors = 0
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull() ?: ""

        if (text.isNotBlank()) {
            if (isSelfEcho(text)) {
                // Ignore self echo
                _currentTranscript.value = ""
                if (isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                    scheduleStartListening(500)
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
                return
            }

            _currentTranscript.value = text
            processUserTurn(text)
        } else {
            if (isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                scheduleStartListening(400)
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    private fun processUserTurn(text: String) {
        _voiceState.value = VoiceState.PROCESSING_AUDIO
        _audioAmplitude.value = 0f

        val userTurn = VoiceTurn(speaker = "user", text = text)
        _conversationHistory.value = _conversationHistory.value + userTurn

        processingJob = controllerScope.launch {
            _voiceState.value = VoiceState.THINKING
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
            lastSpokenAssistantText = responseText

            val assistantTurn = VoiceTurn(
                speaker = "assistant",
                text = responseText,
                actionType = actionType,
                actionDetail = actionDetail
            )
            _conversationHistory.value = _conversationHistory.value + assistantTurn

            if (!_isOutputMuted.value && isTtsReady && tts != null) {
                _voiceState.value = VoiceState.SPEAKING
                tts?.setSpeechRate(activeSettings.speechRate)
                tts?.setPitch(activeSettings.speechPitch)
                val utteranceId = "MAX_VOICE_${System.currentTimeMillis()}"
                requestAudioFocus()
                tts?.speak(responseText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                _voiceState.value = VoiceState.IDLE
                if (isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                    scheduleStartListening(1500)
                }
            }
        }
    }

    override fun onError(error: Int) {
        _audioAmplitude.value = 0f
        if (!isSessionActive) return

        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                consecutiveBusyErrors = 0
                if (activeSettings.isHandsFreeLoopEnabled && isSessionActive && _voiceState.value != VoiceState.SPEAKING) {
                    scheduleStartListening(500)
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                consecutiveBusyErrors++
                if (consecutiveBusyErrors < 3 && isSessionActive && activeSettings.isHandsFreeLoopEnabled) {
                    scheduleStartListening(800)
                } else {
                    consecutiveBusyErrors = 0
                    _voiceState.value = VoiceState.IDLE
                }
            }
            else -> {
                consecutiveBusyErrors = 0
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
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        controllerScope.cancel()
    }
}

