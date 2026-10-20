package com.example.data.repository

import com.example.data.model.AppSettings
import com.example.data.model.OpenRouterMessage
import com.example.data.model.OpenRouterModelItem
import com.example.data.remote.OpenRouterService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OpenRouterRepository(
    private val openRouterService: OpenRouterService = OpenRouterService(),
    private val preferencesRepository: PreferencesRepository
) {
    val defaultModels: List<OpenRouterModelItem> = openRouterService.defaultModels

    val selectedModelIdFlow: Flow<String> = preferencesRepository.settingsFlow.map { it.selectedOpenRouterModel }
    val selectedModelNameFlow: Flow<String> = preferencesRepository.settingsFlow.map { it.selectedOpenRouterModelName }

    suspend fun fetchModels(): Result<List<OpenRouterModelItem>> {
        return openRouterService.fetchModels()
    }

    suspend fun selectModel(modelId: String, displayName: String = "") {
        val cleanId = modelId.trim()
        val resolvedName = displayName.ifBlank {
            openRouterService.availableModels.find { it.id == cleanId }?.name
                ?: defaultModels.find { it.id == cleanId }?.name
                ?: cleanId.substringAfterLast("/")
        }.trim()
        preferencesRepository.setOpenRouterModel(modelId = cleanId, modelName = resolvedName)
    }

    suspend fun sendChatPrompt(
        apiKey: String = "",
        messages: List<OpenRouterMessage>,
        settings: AppSettings
    ): Result<String> {
        // Explicitly ensure the saved model ID (selectedOpenRouterModel) is sent in the 'model' field
        val modelId = settings.selectedOpenRouterModel.ifBlank { "meta-llama/llama-3.3-70b-instruct:free" }
        val keyToUse = apiKey.ifBlank { settings.openRouterApiKey }
        return openRouterService.sendChatPrompt(
            apiKey = keyToUse,
            model = modelId,
            messages = messages
        )
    }
}
