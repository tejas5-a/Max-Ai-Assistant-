package com.example.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.OpenRouterModelItem
import com.example.data.remote.OpenRouterService
import com.example.data.repository.ChatRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val settings: AppSettings = AppSettings(),
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val availableModels: List<OpenRouterModelItem> = emptyList(),
    val isWebSearchEnabled: Boolean = false,
    val isVoiceListening: Boolean = false,
    val voiceTranscript: String = "",
    val errorMessage: String? = null
)

class ChatViewModel(
    private val context: Context,
    private val chatRepository: ChatRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val openRouterService = OpenRouterService()
    val voiceManager = VoiceManager(context)

    private val _currentConversationId = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isWebSearchEnabled = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _fetchedModels = MutableStateFlow<List<OpenRouterModelItem>>(openRouterService.defaultModels)

    val uiState: StateFlow<ChatUiState> = combine(
        preferencesRepository.settingsFlow,
        chatRepository.getAllConversations(),
        _currentConversationId,
        _isLoading,
        _isWebSearchEnabled,
        voiceManager.isListening,
        voiceManager.speechText,
        _errorMessage,
        _fetchedModels
    ) { args ->
        val settings = args[0] as AppSettings
        @Suppress("UNCHECKED_CAST")
        val convs = args[1] as List<Conversation>
        val currentId = args[2] as String?
        val loading = args[3] as Boolean
        val webSearch = args[4] as Boolean
        val listening = args[5] as Boolean
        val transcript = args[6] as String
        val error = args[7] as String?
        @Suppress("UNCHECKED_CAST")
        val models = args[8] as List<OpenRouterModelItem>

        ChatUiState(
            settings = settings,
            conversations = convs,
            currentConversationId = currentId,
            messages = emptyList(), // Filled by separate message observer
            isLoading = loading,
            availableModels = models,
            isWebSearchEnabled = webSearch,
            isVoiceListening = listening,
            voiceTranscript = transcript,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getAllConversations().collect { list ->
                if (_currentConversationId.value == null && list.isNotEmpty()) {
                    selectConversation(list.first().id)
                } else if (_currentConversationId.value == null) {
                    createNewConversation()
                }
            }
        }

        viewModelScope.launch {
            val result = openRouterService.fetchModels()
            if (result.isSuccess) {
                _fetchedModels.value = result.getOrDefault(openRouterService.defaultModels)
            }
        }
    }

    fun selectConversation(id: String) {
        _currentConversationId.value = id
        viewModelScope.launch {
            chatRepository.getMessagesForConversation(id).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun createNewConversation() {
        viewModelScope.launch {
            val newId = chatRepository.createNewConversation()
            selectConversation(newId)
        }
    }

    fun toggleWebSearch() {
        _isWebSearchEnabled.value = !_isWebSearchEnabled.value
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            preferencesRepository.setOpenRouterModel(modelId)
        }
    }

    fun sendMessage(userText: String) {
        val convId = _currentConversationId.value ?: return
        if (userText.isBlank()) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                conversationId = convId,
                userText = userText,
                isWebSearchRequested = _isWebSearchEnabled.value
            )

            _isLoading.value = false

            if (result.isSuccess) {
                val assistantMsg = result.getOrNull()
                if (assistantMsg != null && uiState.value.settings.isAutoReadResponses) {
                    voiceManager.speakText(assistantMsg.content, uiState.value.settings.speechRate)
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun startVoiceInput() {
        voiceManager.startListening()
    }

    fun stopVoiceInput() {
        voiceManager.stopListening()
        val text = voiceManager.speechText.value
        if (text.isNotBlank()) {
            sendMessage(text)
        }
    }

    fun speakMessage(text: String) {
        voiceManager.speakText(text, uiState.value.settings.speechRate)
    }

    fun clearConversation() {
        val id = _currentConversationId.value ?: return
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
            createNewConversation()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
