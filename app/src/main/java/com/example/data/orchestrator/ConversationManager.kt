package com.example.data.orchestrator

import com.example.data.voice.VoiceTurn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConversationManager {
    private val _turns = MutableStateFlow<List<VoiceTurn>>(emptyList())
    val turns: StateFlow<List<VoiceTurn>> = _turns.asStateFlow()

    fun addTurn(turn: VoiceTurn) {
        val updated = _turns.value.toMutableList()
        updated.add(turn)
        _turns.value = updated
    }

    fun clear() {
        _turns.value = emptyList()
    }

    fun getRecentHistory(limit: Int = 6): List<VoiceTurn> {
        return _turns.value.takeLast(limit)
    }
}
