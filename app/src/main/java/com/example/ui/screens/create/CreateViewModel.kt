package com.example.ui.screens.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.remote.PollinationsModelItem
import com.example.data.remote.PollinationsService
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class GeneratedImageItem(
    val localFilePath: String,
    val prompt: String,
    val model: String,
    val aspectRatio: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CreateUiState(
    val settings: AppSettings = AppSettings(),
    val isGenerating: Boolean = false,
    val currentImageItem: GeneratedImageItem? = null,
    val historyItems: List<GeneratedImageItem> = emptyList(),
    val availableModels: List<PollinationsModelItem> = emptyList(),
    val errorMessage: String? = null
)

class CreateViewModel(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val pollinationsService = PollinationsService(context)

    private val _isGenerating = MutableStateFlow(false)
    private val _currentImageItem = MutableStateFlow<GeneratedImageItem?>(null)
    private val _historyItems = MutableStateFlow<List<GeneratedImageItem>>(emptyList())
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CreateUiState> = combine(
        preferencesRepository.settingsFlow,
        _isGenerating,
        _currentImageItem,
        _historyItems,
        _errorMessage
    ) { settings, generating, current, history, error ->
        CreateUiState(
            settings = settings,
            isGenerating = generating,
            currentImageItem = current,
            historyItems = history,
            availableModels = pollinationsService.availableModels,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreateUiState()
    )

    init {
        loadHistoryFromFiles()
    }

    private fun loadHistoryFromFiles() {
        viewModelScope.launch {
            val imagesDir = File(context.filesDir, "pollinations_images")
            if (imagesDir.exists()) {
                val files = imagesDir.listFiles()?.filter { it.extension == "jpg" }?.sortedByDescending { it.lastModified() } ?: emptyList()
                val items = files.map { file ->
                    GeneratedImageItem(
                        localFilePath = file.absolutePath,
                        prompt = "Pollinations Generated Image",
                        model = "flux",
                        aspectRatio = "1:1",
                        timestamp = file.lastModified()
                    )
                }
                _historyItems.value = items
            }
        }
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            preferencesRepository.setPollinationsModel(modelId)
        }
    }

    fun selectAspect(aspect: String) {
        viewModelScope.launch {
            preferencesRepository.setPollinationsAspect(aspect)
        }
    }

    fun generateImage(prompt: String) {
        if (prompt.isBlank()) return
        val settings = uiState.value.settings

        if (!settings.isPollinationsEnabled) {
            _errorMessage.value = "Pollinations image provider is disabled in Settings. Enable it in Settings to generate images."
            return
        }

        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = pollinationsService.generateAndSaveImage(
                prompt = prompt,
                model = settings.selectedPollinationsModel,
                aspectRatio = settings.defaultAspectRatio
            )

            _isGenerating.value = false

            if (result.isSuccess) {
                val (localPath, _) = result.getOrThrow()
                val item = GeneratedImageItem(
                    localFilePath = localPath,
                    prompt = prompt,
                    model = settings.selectedPollinationsModel,
                    aspectRatio = settings.defaultAspectRatio
                )
                _currentImageItem.value = item
                _historyItems.value = listOf(item) + _historyItems.value
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to generate image."
            }
        }
    }
}
