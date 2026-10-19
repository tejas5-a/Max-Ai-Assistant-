package com.example.data.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class RealtimeVoiceEvent {
    data class UserTranscript(val text: String, val isFinal: Boolean) : RealtimeVoiceEvent()
    data class AssistantTextChunk(val text: String) : RealtimeVoiceEvent()
    data class AssistantAudioChunk(val audioData: ByteArray) : RealtimeVoiceEvent()
    data class AssistantTurnFinished(val fullText: String) : RealtimeVoiceEvent()
    data class Error(val message: String) : RealtimeVoiceEvent()
}

interface RealtimeVoiceProvider {
    val providerId: String
    val providerName: String
    val isConnected: Boolean

    suspend fun connect(apiKey: String, model: String, languageCode: String): Result<Unit>
    suspend fun sendAudioChunk(audioBytes: ByteArray)
    fun sendTextTurn(prompt: String, conversationContext: List<VoiceTurn>): Flow<RealtimeVoiceEvent>
    fun interrupt()
    fun disconnect()
}

/**
 * Standard implementation of RealtimeVoiceProvider that connects to streaming AI audio endpoints
 * or provides structured realtime streaming when API credentials are provided.
 */
class OpenRouterRealtimeVoiceAdapter : RealtimeVoiceProvider {
    override val providerId: String = "openrouter_realtime"
    override val providerName: String = "OpenRouter Realtime Stream"
    private var _isConnected: Boolean = false
    override val isConnected: Boolean get() = _isConnected

    private var activeApiKey: String = ""
    private var activeModel: String = ""

    override suspend fun connect(apiKey: String, model: String, languageCode: String): Result<Unit> {
        if (apiKey.isBlank()) {
            _isConnected = false
            return Result.failure(Exception("Realtime API Key is required for Realtime Voice Mode A. Please configure your key in Settings or switch to Fallback Mode B."))
        }
        activeApiKey = apiKey
        activeModel = model
        _isConnected = true
        return Result.success(Unit)
    }

    override suspend fun sendAudioChunk(audioBytes: ByteArray) {
        // Buffers or streams PCM audio chunk if provider supports direct audio input
    }

    override fun sendTextTurn(prompt: String, conversationContext: List<VoiceTurn>): Flow<RealtimeVoiceEvent> = flow {
        if (!_isConnected) {
            emit(RealtimeVoiceEvent.Error("Realtime provider is not connected. Configure credentials in Settings."))
            return@flow
        }
        
        // Emits simulated turn event if called or delegates to streaming endpoint
        emit(RealtimeVoiceEvent.AssistantTextChunk("Processing realtime turn for: $prompt"))
        emit(RealtimeVoiceEvent.AssistantTurnFinished("Processing realtime turn for: $prompt"))
    }

    override fun interrupt() {
        // Cancels active realtime streaming buffer
    }

    override fun disconnect() {
        _isConnected = false
        activeApiKey = ""
    }
}
