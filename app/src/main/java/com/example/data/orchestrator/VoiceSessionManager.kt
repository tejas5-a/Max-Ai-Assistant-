package com.example.data.orchestrator

import com.example.data.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceSessionManager {
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    fun updateState(newState: VoiceState) {
        _voiceState.value = newState
    }

    fun toggleMute(): Boolean {
        _isMuted.value = !_isMuted.value
        return _isMuted.value
    }

    fun stopSession() {
        _voiceState.value = VoiceState.IDLE
    }
}
