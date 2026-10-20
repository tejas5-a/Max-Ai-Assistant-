package com.example.data.voice

import android.content.Context
import com.example.data.model.TaskItem
import com.example.data.orchestrator.AssistantOrchestrator
import com.example.data.remote.OpenRouterService
import com.example.data.remote.PollinationsService
import com.example.data.remote.WebSearchService
import com.example.data.repository.AutomationRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TaskRepository

sealed class VoiceCommandResult {
    data class SpokenAnswer(val text: String) : VoiceCommandResult()
    data class TaskCreated(val text: String, val task: TaskItem) : VoiceCommandResult()
    data class ImageCreated(val text: String, val imagePath: String, val prompt: String) : VoiceCommandResult()
    data class SearchAnswer(val text: String, val sourcesCount: Int) : VoiceCommandResult()
    data class AutomationProposed(val text: String, val title: String) : VoiceCommandResult()
    data class Error(val text: String) : VoiceCommandResult()
}

class VoiceCommandDispatcher(
    private val openRouterService: OpenRouterService,
    private val pollinationsService: PollinationsService,
    private val webSearchService: WebSearchService,
    private val taskRepository: TaskRepository,
    private val automationRepository: AutomationRepository,
    private val preferencesRepository: PreferencesRepository,
    context: Context? = null
) {
    private var orchestrator: AssistantOrchestrator? = if (context != null) {
        AssistantOrchestrator(
            context = context,
            taskRepository = taskRepository,
            automationRepository = automationRepository,
            preferencesRepository = preferencesRepository
        )
    } else null

    fun initContext(context: Context) {
        if (orchestrator == null) {
            orchestrator = AssistantOrchestrator(
                context = context,
                taskRepository = taskRepository,
                automationRepository = automationRepository,
                preferencesRepository = preferencesRepository
            )
        }
    }

    suspend fun dispatchVoiceInput(
        userText: String,
        recentContext: List<VoiceTurn>
    ): VoiceCommandResult {
        val orch = orchestrator
        return if (orch != null) {
            orch.processInput(userText)
        } else {
            // Standalone fallback
            VoiceCommandResult.SpokenAnswer("I heard: '$userText'. Processing via MAX Assistant.")
        }
    }
}
