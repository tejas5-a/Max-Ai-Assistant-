package com.example.data.model

data class SearchSource(
    val title: String,
    val domain: String,
    val snippet: String,
    val url: String
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val model: String? = null,
    val imageUrl: String? = null,
    val sources: List<SearchSource> = emptyList(),
    val isError: Boolean = false,
    val isStreaming: Boolean = false
)

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = ""
)
