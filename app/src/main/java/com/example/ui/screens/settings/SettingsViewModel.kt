package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.OpenRouterModelItem
import com.example.data.model.VoiceEngineMode
import com.example.data.repository.ChatRepository
import com.example.data.repository.OpenRouterRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TaskRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val chatRepository: ChatRepository,
    private val taskRepository: TaskRepository,
    private val openRouterRepository: OpenRouterRepository = OpenRouterRepository(preferencesRepository = preferencesRepository)
) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferencesRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    private val _availableModels = MutableStateFlow<List<OpenRouterModelItem>>(openRouterRepository.defaultModels)
    val availableModels: StateFlow<List<OpenRouterModelItem>> = _availableModels.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    private val _fetchStatus = MutableStateFlow<String?>(null)
    val fetchStatus: StateFlow<String?> = _fetchStatus.asStateFlow()

    init {
        fetchOpenRouterModels()
    }

    fun fetchOpenRouterModels() {
        _isFetchingModels.value = true
        _fetchStatus.value = null
        viewModelScope.launch {
            val result = openRouterRepository.fetchModels()
            _isFetchingModels.value = false
            if (result.isSuccess) {
                val models = result.getOrDefault(openRouterRepository.defaultModels)
                _availableModels.value = models
                _fetchStatus.value = "Fetched ${models.size} models from OpenRouter API."
            } else {
                _fetchStatus.value = "Fetch failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}. Displaying default models."
            }
        }
    }

    fun selectOpenRouterModel(modelId: String, displayName: String) {
        viewModelScope.launch {
            openRouterRepository.selectModel(modelId, displayName)
        }
    }

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
