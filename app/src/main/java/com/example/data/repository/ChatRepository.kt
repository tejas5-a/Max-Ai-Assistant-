package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.OpenRouterMessage
import com.example.data.remote.OpenRouterService
import com.example.data.remote.WebSearchService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val preferencesRepository: PreferencesRepository
) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val openRouterService = OpenRouterService()
    private val webSearchService = WebSearchService()

    fun getAllConversations(): Flow<List<Conversation>> {
        return chatDao.getAllConversations().map { list -> list.map { it.toDomain() } }
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain(moshi) }
        }
    }

    suspend fun createNewConversation(title: String = "New Conversation"): String {
        val id = UUID.randomUUID().toString()
        val conv = Conversation(id = id, title = title)
        chatDao.insertConversation(ConversationEntity.fromDomain(conv))
        return id
    }

    suspend fun deleteConversation(conversationId: String) {
        chatDao.deleteMessagesForConversation(conversationId)
        chatDao.deleteConversation(conversationId)
    }

    suspend fun clearAllHistory() {
        chatDao.clearAllMessages()
        chatDao.clearAllConversations()
    }

    suspend fun sendMessage(
        conversationId: String,
        userText: String,
        isWebSearchRequested: Boolean = false
    ): Result<ChatMessage> {
        val settings = preferencesRepository.settingsFlow.first()

        // 1. Save user message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = "user",
            content = userText
        )
        chatDao.insertMessage(ChatMessageEntity.fromDomain(userMsg, moshi))

        // Update conversation title & last message
        val existingMessages = chatDao.getMessagesForConversation(conversationId).first()
        val title = if (existingMessages.size <= 2) userText.take(30) else "Conversation"
        chatDao.insertConversation(
            ConversationEntity(
                id = conversationId,
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastMessage = userText
            )
        )

        // Check if OpenRouter provider is enabled
        if (!settings.isOpenRouterEnabled) {
            val disabledMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "assistant",
                content = "OpenRouter AI provider is currently disabled in Settings. Please enable OpenRouter in Settings to send text messages.",
                isError = true
            )
            chatDao.insertMessage(ChatMessageEntity.fromDomain(disabledMsg, moshi))
            return Result.success(disabledMsg)
        }

        // 2. Perform web search if requested
        val sources = if (isWebSearchRequested || userText.contains("search", ignoreCase = true) || userText.contains("latest", ignoreCase = true)) {
            webSearchService.performSearch(userText).getOrDefault(emptyList())
        } else emptyList()

        // 3. Prepare messages for OpenRouter AI
        val history = existingMessages.map { it.toDomain(moshi) }
        val apiMessages = mutableListOf<OpenRouterMessage>()

        // System prompt context
        var systemContext = "You are MAX AI ASSISTANT, a helpful, intelligent, and real native AI assistant."
        if (sources.isNotEmpty()) {
            val sourceSummary = sources.joinToString("\n") { "- [${it.title}](${it.url}): ${it.snippet}" }
            systemContext += "\n\nReal-time Web Search Results retrieved for this query:\n$sourceSummary\n\nPlease incorporate these facts into your answer and cite sources."
        }
        apiMessages.add(OpenRouterMessage("system", systemContext))

        // Recent history (up to last 10 messages)
        history.takeLast(10).forEach { msg ->
            if (msg.role == "user" || msg.role == "assistant") {
                apiMessages.add(OpenRouterMessage(msg.role, msg.content))
            }
        }
        apiMessages.add(OpenRouterMessage("user", userText))

        // 4. Send request to OpenRouter using saved model ID in the 'model' field
        val activeModelId = settings.selectedOpenRouterModel.ifBlank { "meta-llama/llama-3.3-70b-instruct:free" }
        val aiResult = openRouterService.sendChatPrompt(
            apiKey = settings.openRouterApiKey,
            model = activeModelId,
            messages = apiMessages
        )

        val modelLabel = settings.selectedOpenRouterModelName.ifBlank { activeModelId }

        val assistantMsg = if (aiResult.isSuccess) {
            ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "assistant",
                content = aiResult.getOrDefault(""),
                model = modelLabel,
                sources = sources
            )
        } else {
            ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = "assistant",
                content = aiResult.exceptionOrNull()?.message ?: "An unexpected error occurred.",
                model = modelLabel,
                isError = true
            )
        }

        chatDao.insertMessage(ChatMessageEntity.fromDomain(assistantMsg, moshi))
        return Result.success(assistantMsg)
    }
}
