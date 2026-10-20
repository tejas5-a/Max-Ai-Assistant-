package com.example.data.remote

import com.example.data.model.OpenRouterMessage
import com.example.data.model.OpenRouterModelItem
import com.example.data.model.OpenRouterModelsListResponse
import com.example.data.model.OpenRouterRequest
import com.example.data.model.OpenRouterResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Header("HTTP-Referer") referer: String = "https://maxai.assistant",
        @Header("X-Title") appTitle: String = "MAX AI ASSISTANT",
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>

    @GET("models")
    suspend fun getModels(): Response<OpenRouterModelsListResponse>
}

class OpenRouterService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    private val api: OpenRouterApi = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OpenRouterApi::class.java)

    val defaultModels = listOf(
        OpenRouterModelItem("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B (Free)", "High performance open model for reasoning & coding", true),
        OpenRouterModelItem("google/gemini-2.0-flash-exp:free", "Gemini 2.0 Flash (Free)", "Google's fast multimodal model with low latency", true),
        OpenRouterModelItem("deepseek/deepseek-r1:free", "DeepSeek R1 (Free)", "Advanced reasoning and problem-solving model", true),
        OpenRouterModelItem("mistralai/mistral-7b-instruct:free", "Mistral 7B (Free)", "Lightweight and fast conversational model", true),
        OpenRouterModelItem("openai/gpt-4o-mini", "GPT-4o Mini", "Balanced speed and intelligence from OpenAI", false),
        OpenRouterModelItem("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", "Anthropic state-of-the-art assistant model", false)
    )

    var availableModels: List<OpenRouterModelItem> = defaultModels
        private set

    suspend fun fetchModels(): Result<List<OpenRouterModelItem>> {
        return try {
            val response = api.getModels()
            if (response.isSuccessful && response.body()?.data != null) {
                val apiItems = response.body()!!.data!!
                val mapped = apiItems.map { item ->
                    val isFreeModel = item.id.endsWith(":free") ||
                            (item.pricing?.prompt == "0" && item.pricing?.completion == "0")
                    OpenRouterModelItem(
                        id = item.id,
                        name = item.name ?: item.id,
                        description = item.description ?: if (isFreeModel) "Free OpenRouter Model" else "Standard OpenRouter Model",
                        isFree = isFreeModel
                    )
                }
                // Prioritize free models first, then sort alphabetically
                val sorted = mapped.sortedWith(compareByDescending<OpenRouterModelItem> { it.isFree }.thenBy { it.name })
                availableModels = sorted
                Result.success(sorted)
            } else {
                Result.failure(Exception("HTTP ${response.code()} while fetching models"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendChatPrompt(
        apiKey: String,
        model: String,
        messages: List<OpenRouterMessage>
    ): Result<String> {
        val authHeader = if (apiKey.isNotBlank()) "Bearer $apiKey" else "Bearer free"
        val request = OpenRouterRequest(model = model, messages = messages)

        return try {
            val response = api.createChatCompletion(authHeader = authHeader, request = request)
            if (response.isSuccessful) {
                val body = response.body()
                val text = body?.choices?.firstOrNull()?.message?.content
                if (!text.isNullOrEmpty()) {
                    Result.success(text)
                } else if (body?.error != null) {
                    Result.failure(Exception("OpenRouter Error: ${body.error.message ?: "Unknown error"}"))
                } else {
                    Result.failure(Exception("Received empty response from OpenRouter AI."))
                }
            } else {
                val errorCode = response.code()
                val errorMsg = when (errorCode) {
                    401 -> "Invalid OpenRouter API Key. Please check your key in Settings."
                    402 -> "Payment required / OpenRouter credits exhausted."
                    429 -> "OpenRouter rate limit reached. Please wait a moment and try again."
                    500, 502, 503 -> "OpenRouter service is temporarily unavailable. Try switching models."
                    else -> "OpenRouter API returned HTTP $errorCode."
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to OpenRouter service."))
        }
    }
}
