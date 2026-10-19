package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OpenRouterMessage>,
    @Json(name = "temperature") val temperature: Double = 0.7
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @Json(name = "message") val message: OpenRouterMessage?,
    @Json(name = "finish_reason") val finishReason: String?
)

@JsonClass(generateAdapter = true)
data class OpenRouterUsage(
    @Json(name = "prompt_tokens") val promptTokens: Int? = 0,
    @Json(name = "completion_tokens") val completionTokens: Int? = 0,
    @Json(name = "total_tokens") val totalTokens: Int? = 0
)

@JsonClass(generateAdapter = true)
data class OpenRouterResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "choices") val choices: List<OpenRouterChoice>?,
    @Json(name = "usage") val usage: OpenRouterUsage?,
    @Json(name = "error") val error: OpenRouterErrorDetail?
)

@JsonClass(generateAdapter = true)
data class OpenRouterErrorDetail(
    @Json(name = "message") val message: String?,
    @Json(name = "code") val code: Int?
)

data class OpenRouterModelItem(
    val id: String,
    val name: String,
    val description: String,
    val isFree: Boolean
)
