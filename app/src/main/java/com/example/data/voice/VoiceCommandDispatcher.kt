package com.example.data.voice

import com.example.data.automation.WorkflowParser
import com.example.data.model.OpenRouterMessage
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.remote.OpenRouterService
import com.example.data.remote.PollinationsService
import com.example.data.remote.WebSearchService
import com.example.data.repository.AutomationRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

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
    private val preferencesRepository: PreferencesRepository
) {

    suspend fun dispatchVoiceInput(
        userText: String,
        recentContext: List<VoiceTurn>
    ): VoiceCommandResult {
        val trimmed = userText.trim()
        val lower = trimmed.lowercase()
        val settings = preferencesRepository.settingsFlow.first()

        // 1. Task Creation Voice Intent
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
                    text = "I've added the task '$cleanedTitle' with ${priority.name.lowercase()} priority to your tasks list.",
                    task = task
                )
            }
        }

        // 2. Image Generation Voice Intent
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
                val genResult = pollinationsService.generateAndSaveImage(
                    prompt = prompt,
                    model = settings.selectedPollinationsModel,
                    aspectRatio = settings.defaultAspectRatio
                )
                if (genResult.isSuccess) {
                    val (path, _) = genResult.getOrThrow()
                    return VoiceCommandResult.ImageCreated(
                        text = "I generated that image for '$prompt'. You can see it on screen.",
                        imagePath = path,
                        prompt = prompt
                    )
                } else {
                    return VoiceCommandResult.Error(
                        text = "I couldn't generate the image right now: ${genResult.exceptionOrNull()?.message ?: "Check your connection."}"
                    )
                }
            }
        }

        // 3. Web Search Voice Intent
        if (lower.startsWith("search the web for") || lower.startsWith("search for") || lower.startsWith("web search") || lower.contains("what is the latest on") || lower.contains("current news about")) {
            val query = trimmed
                .removePrefix("search the web for")
                .removePrefix("search for")
                .removePrefix("web search")
                .trim()

            if (query.isNotBlank()) {
                val searchResult = webSearchService.performSearch(query)
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

        // 4. Scheduled Automation Voice Intent
        if (lower.startsWith("automate") || lower.startsWith("schedule a workflow") || lower.startsWith("schedule a routine")) {
            val autoPrompt = trimmed.removePrefix("automate").removePrefix("schedule a workflow to").trim()
            if (autoPrompt.isNotBlank()) {
                val workflow = WorkflowParser.parsePrompt(autoPrompt)
                automationRepository.saveWorkflow(workflow)
                return VoiceCommandResult.AutomationProposed(
                    text = "I've drafted a workflow for '${workflow.title}' scheduled for ${workflow.scheduleDescription}. You can approve it in the Automations tab.",
                    title = workflow.title
                )
            }
        }

        // 5. General AI Reasoning / Conversational Answer via OpenRouter
        if (!settings.isOpenRouterEnabled) {
            return VoiceCommandResult.Error("OpenRouter text AI is currently disabled in Settings. Please enable it in Settings to talk with MAX.")
        }

        val messages = mutableListOf<OpenRouterMessage>()
        val systemPrompt = "You are MAX AI ASSISTANT in Live Voice conversation mode. Keep your answers natural, friendly, concise, and easy to listen to aloud. Avoid markdown formatting, asterisks, or bullet points because your response will be spoken aloud."
        messages.add(OpenRouterMessage("system", systemPrompt))

        // Provide up to last 4 turns of context
        recentContext.takeLast(4).forEach { turn ->
            val role = if (turn.speaker == "user") "user" else "assistant"
            messages.add(OpenRouterMessage(role, turn.text))
        }
        messages.add(OpenRouterMessage("user", trimmed))

        val aiResult = openRouterService.sendChatPrompt(
            apiKey = settings.openRouterApiKey,
            model = settings.selectedOpenRouterModel,
            messages = messages
        )

        return if (aiResult.isSuccess) {
            val answer = aiResult.getOrDefault("I'm here. How can I help you today?")
            // Strip markdown asterisks or code formatting for clean speech
            val cleanSpeech = answer.replace("**", "").replace("#", "").replace("`", "")
            VoiceCommandResult.SpokenAnswer(cleanSpeech)
        } else {
            val errorMsg = aiResult.exceptionOrNull()?.message ?: "I had trouble generating a response. Please check your network or API settings."
            VoiceCommandResult.Error(errorMsg)
        }
    }
}
