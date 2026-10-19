package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.VoiceEngineMode
import com.example.data.repository.ChatRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TaskRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val chatRepository: ChatRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferencesRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.updateThemeMode(mode) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { preferencesRepository.updateLanguage(lang) }
    }

    fun setOpenRouterEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setOpenRouterEnabled(enabled) }
    }

    fun setOpenRouterKey(key: String) {
        viewModelScope.launch { preferencesRepository.setOpenRouterApiKey(key) }
    }

    fun setPollinationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setPollinationsEnabled(enabled) }
    }

    fun setVoiceEngineMode(mode: VoiceEngineMode) {
        viewModelScope.launch { preferencesRepository.setVoiceEngineMode(mode) }
    }

    fun setRealtimeVoiceKey(key: String) {
        viewModelScope.launch { preferencesRepository.setRealtimeVoiceApiKey(key) }
    }

    fun setSpeechOutputEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSpeechOutputEnabled(enabled) }
    }

    fun setAutoReadResponses(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoReadResponses(enabled) }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechRate(rate) }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechPitch(pitch) }
    }

    fun setHandsFreeLoopEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHandsFreeLoopEnabled(enabled) }
    }

    fun setInterruptionEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setInterruptionEnabled(enabled) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            chatRepository.clearAllHistory()
            taskRepository.clearAllTasks()
        }
    }
}
