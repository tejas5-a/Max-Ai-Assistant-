package com.example.ui.screens.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.VoiceEngineMode
import com.example.data.remote.OpenRouterService
import com.example.data.remote.PollinationsService
import com.example.data.remote.WebSearchService
import com.example.data.repository.AutomationRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TaskRepository
import com.example.data.voice.ConversationalVoiceController
import com.example.data.voice.VoiceCommandDispatcher
import com.example.data.voice.VoiceState
import com.example.data.voice.VoiceTurn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LiveVoiceUiState(
    val voiceState: VoiceState = VoiceState.IDLE,
    val audioAmplitude: Float = 0f,
    val currentTranscript: String = "",
    val assistantSpokenText: String = "",
    val conversationHistory: List<VoiceTurn> = emptyList(),
    val isOutputMuted: Boolean = false,
    val errorMessage: String? = null,
    val settings: AppSettings = AppSettings()
)

class LiveVoiceViewModel(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val taskRepository: TaskRepository,
    private val automationRepository: AutomationRepository
) : ViewModel() {

    private val openRouterService = OpenRouterService()
    private val pollinationsService = PollinationsService(context)
    private val webSearchService = WebSearchService()

    private val voiceCommandDispatcher = VoiceCommandDispatcher(
        openRouterService = openRouterService,
        pollinationsService = pollinationsService,
        webSearchService = webSearchService,
        taskRepository = taskRepository,
        automationRepository = automationRepository,
        preferencesRepository = preferencesRepository
    )

    val controller = ConversationalVoiceController(
        context = context,
        voiceCommandDispatcher = voiceCommandDispatcher,
        preferencesRepository = preferencesRepository
    )

    init {
        voiceCommandDispatcher.initContext(context)
    }

    val uiState: StateFlow<LiveVoiceUiState> = combine(
        controller.voiceState,
        controller.audioAmplitude,
        controller.currentTranscript,
        controller.assistantSpokenText,
        controller.conversationHistory,
        controller.isOutputMuted,
        controller.errorMessage,
        preferencesRepository.settingsFlow
    ) { args ->
        val historyList = (args[4] as? List<*>)?.filterIsInstance<VoiceTurn>() ?: emptyList()
        LiveVoiceUiState(
            voiceState = args[0] as VoiceState,
            audioAmplitude = args[1] as Float,
            currentTranscript = args[2] as String,
            assistantSpokenText = args[3] as String,
            conversationHistory = historyList,
            isOutputMuted = args[5] as Boolean,
            errorMessage = args[6] as String?,
            settings = args[7] as AppSettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveVoiceUiState()
    )

    fun startSession() {
        controller.startSession()
    }

    fun pauseOrResume() {
        if (uiState.value.voiceState == VoiceState.LISTENING) {
            controller.stopListening()
        } else {
            controller.startListening()
        }
    }

    fun interrupt() {
        controller.interrupt()
    }

    fun toggleMute() {
        controller.toggleOutputMute()
    }

    fun switchMode(mode: VoiceEngineMode) {
        viewModelScope.launch {
            preferencesRepository.setVoiceEngineMode(mode)
        }
    }

    fun endSession() {
        controller.endSession()
    }

    override fun onCleared() {
        super.onCleared()
        controller.destroy()
    }
}
