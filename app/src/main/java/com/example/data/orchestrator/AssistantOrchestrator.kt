package com.example.data.orchestrator

import android.content.Context
import com.example.data.action.ActionPlanner
import com.example.data.action.ActionResult
import com.example.data.action.PlannedIntent
import com.example.data.automation.WorkflowParser
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.AutomationRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TaskRepository
import com.example.data.voice.VoiceCommandResult
import kotlinx.coroutines.flow.first
import java.util.UUID

class AssistantOrchestrator(
    private val context: Context,
    val providerRouter: ProviderRouter = ProviderRouter(context),
    val conversationManager: ConversationManager = ConversationManager(),
    val voiceSessionManager: VoiceSessionManager = VoiceSessionManager(),
    val permissionManager: PermissionManager = PermissionManager(context),
    private val taskRepository: TaskRepository,
    private val automationRepository: AutomationRepository,
    private val preferencesRepository: PreferencesRepository
) {
    val actionPlanner = ActionPlanner(context)

    suspend fun processInput(userText: String): VoiceCommandResult {
        val trimmed = userText.trim()
        val lower = trimmed.lowercase()
        val settings = preferencesRepository.settingsFlow.first()

        // 1. Evaluate Phone Control & System Actions via ActionPlanner
        val plannedIntent = actionPlanner.planIntent(trimmed)
        if (plannedIntent !is PlannedIntent.PassToAi) {
            val actionResult = actionPlanner.executePlannedIntent(plannedIntent)
            if (actionResult != null) {
                return when (actionResult) {
                    is ActionResult.Success -> VoiceCommandResult.SpokenAnswer(actionResult.message)
                    is ActionResult.RequiresConfirmation -> VoiceCommandResult.SpokenAnswer(actionResult.message)
                    is ActionResult.Error -> VoiceCommandResult.Error(actionResult.message)
                }
            }
        }

        // 2. High-Level Assistant Intents: Tasks, Image Generation, Web Search, Automations
        if (lower.startsWith("add task") || lower.startsWith("create task") || lower.startsWith("new task") || lower.startsWith("remind me to")) {
            val taskTitle = trimmed
                .removePrefix("add task")
                .removePrefix("Add task")
                .removePrefix("create task")
                .removePrefix("Create task")
                .removePrefix("new task")
                .removePrefix("New task")
                .removePrefix("remind me to")
                .removePrefix("Remind me to")
                .trim()
                .trimStart(':', ' ')

            if (taskTitle.isNotBlank()) {
                val priority = when {
                    lower.contains("high priority") || lower.contains("urgent") -> TaskPriority.HIGH
                    lower.contains("low priority") -> TaskPriority.LOW
                    else -> TaskPriority.MEDIUM
                }
                val cleanedTitle = taskTitle.replace("high priority", "", ignoreCase = true)
                    .replace("low priority", "", ignoreCase = true)
                    .replace("urgent", "", ignoreCase = true).trim()

                val task = TaskItem(
                    id = UUID.randomUUID().toString(),
                    title = cleanedTitle,
                    priority = priority,
                    category = "Voice Created"
                )
                taskRepository.addTask(task)
                return VoiceCommandResult.TaskCreated(
                    text = "Added task '$cleanedTitle' with ${priority.name.lowercase()} priority.",
                    task = task
                )
            }
        }

        // Image Generation Intent
        if ((lower.startsWith("create an image") || lower.startsWith("generate an image") || lower.startsWith("draw") || lower.startsWith("make an image")) && settings.isPollinationsEnabled) {
            val prompt = trimmed
                .removePrefix("create an image of")
                .removePrefix("Create an image of")
                .removePrefix("generate an image of")
                .removePrefix("Generate an image of")
                .removePrefix("create an image")
                .removePrefix("draw")
                .removePrefix("Draw")
                .removePrefix("make an image of")
                .trim()

            if (prompt.isNotBlank()) {
                val genResult = providerRouter.pollinationsService.generateAndSaveImage(
                    prompt = prompt,
                    model = settings.selectedPollinationsModel,
                    aspectRatio = settings.defaultAspectRatio
                )
                if (genResult.isSuccess) {
                    val (path, _) = genResult.getOrThrow()
                    return VoiceCommandResult.ImageCreated(
                        text = "I generated that image for '$prompt'.",
                        imagePath = path,
                        prompt = prompt
                    )
                } else {
                    return VoiceCommandResult.Error(
                        text = "Image generation failed: ${genResult.exceptionOrNull()?.message ?: "Check network connection."}"
                    )
                }
            }
        }

        // Web Search Intent
        if (lower.startsWith("search the web for") || lower.startsWith("search for") || lower.startsWith("web search") || lower.contains("what is the latest on") || lower.contains("current news about")) {
            val query = trimmed
                .removePrefix("search the web for")
                .removePrefix("search for")
                .removePrefix("web search")
                .trim()

            if (query.isNotBlank()) {
                val searchResult = providerRouter.webSearchService.performSearch(query)
                if (searchResult.isSuccess) {
                    val sources = searchResult.getOrDefault(emptyList())
                    if (sources.isNotEmpty()) {
                        val topSnippet = sources.first().snippet
                        val topDomain = sources.first().domain
                        val responseText = "According to $topDomain: $topSnippet"
                        return VoiceCommandResult.SearchAnswer(text = responseText, sourcesCount = sources.size)
                    }
                }
            }
        }

        // Scheduled Automation Intent
        if (lower.startsWith("automate") || lower.startsWith("schedule a workflow") || lower.startsWith("schedule a routine")) {
            val autoPrompt = trimmed.removePrefix("automate").removePrefix("schedule a workflow to").trim()
            if (autoPrompt.isNotBlank()) {
                val workflow = WorkflowParser.parsePrompt(autoPrompt)
                automationRepository.saveWorkflow(workflow)
                return VoiceCommandResult.AutomationProposed(
                    text = "I've drafted a workflow for '${workflow.title}' scheduled for ${workflow.scheduleDescription}. You can view it in Automations.",
                    title = workflow.title
                )
            }
        }

        // 3. Multiturn Live Voice AI via Gemini Live Provider
        val historyTurns = conversationManager.getRecentHistory(6).map { turn ->
            val role = if (turn.speaker == "user") "user" else "assistant"
            role to turn.text
        }

        val geminiResult = providerRouter.geminiApiService.sendVoicePrompt(
            userText = trimmed,
            history = historyTurns,
            customApiKey = settings.realtimeVoiceApiKey
        )

        val spokenText = geminiResult.getOrDefault("Hello! I am MAX, your live voice assistant. How can I help you?")
        return VoiceCommandResult.SpokenAnswer(spokenText)
    }
}
