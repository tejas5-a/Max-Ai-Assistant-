package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(private val context: Context) : RecognitionListener, TextToSpeech.OnInitListener {

    private val speechRecognizer: SpeechRecognizer? = if (SpeechRecognizer.isRecognitionAvailable(context)) {
        SpeechRecognizer.createSpeechRecognizer(context).apply { setRecognitionListener(this@VoiceManager) }
    } else null

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _speechText = MutableStateFlow("")
    val speechText: StateFlow<String> = _speechText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startListening(languageCode: String = "en-US") {
        if (speechRecognizer == null) {
            _errorMessage.value = "Speech recognition is not available on this device."
            return
        }

        _speechText.value = ""
        _errorMessage.value = null
        _isListening.value = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _errorMessage.value = e.localizedMessage ?: "Failed to start listening."
        }
    }

    fun stopListening() {
        _isListening.value = false
        speechRecognizer?.stopListening()
    }

    fun speakText(text: String, rate: Float = 1.0f) {
        if (!isTtsReady || tts == null) return
        tts?.setSpeechRate(rate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MAX_TTS_ID_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
        } else {
            isTtsReady = false
        }
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _speechText.value = matches[0]
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _speechText.value = matches[0]
        }
    }

    override fun onError(error: Int) {
        _isListening.value = false
        val message = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak again."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during speech recognition."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            else -> "Voice input error ($error)."
        }
        _errorMessage.value = message
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { _isListening.value = false }
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
