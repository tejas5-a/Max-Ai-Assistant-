package com.example.data.remote

import android.util.Log
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
        @Header("Authorization") authHeader: String?,
        @Header("Content-Type") contentType: String = "application/json",
        @Header("HTTP-Referer") referer: String = "https://maxai.assistant",
        @Header("X-Title") appTitle: String = "MAX AI ASSISTANT",
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>

    @GET("models")
    suspend fun getModels(): Response<OpenRouterModelsListResponse>
}

class OpenRouterService {
    private val TAG = "OpenRouterService"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
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
                val sorted = mapped.sortedWith(compareByDescending<OpenRouterModelItem> { it.isFree }.thenBy { it.name })
                availableModels = sorted
                Result.success(sorted)
            } else {
                val errorBodyStr = response.errorBody()?.string()
                Log.e(TAG, "GET models failed: HTTP ${response.code()} | Body: $errorBodyStr")
                Result.failure(Exception("HTTP ${response.code()} while fetching models"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching models: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    suspend fun sendChatPrompt(
        apiKey: String,
        model: String,
        messages: List<OpenRouterMessage>
    ): Result<String> {
        val cleanModel = model.trim()
        val cleanApiKey = apiKey.trim()

        if (cleanModel.isBlank()) {
            return Result.failure(Exception("No AI model selected. Please select a model in Settings or the model dropdown."))
        }

        val authHeader = if (cleanApiKey.isNotBlank()) "Bearer $cleanApiKey" else null

        val request = OpenRouterRequest(model = cleanModel, messages = messages)

        Log.d(TAG, "POST https://openrouter.ai/api/v1/chat/completions | Model: $cleanModel | Messages Count: ${messages.size} | Auth Configured: ${cleanApiKey.isNotBlank()}")

        return try {
            val response = api.createChatCompletion(authHeader = authHeader, request = request)

            if (response.isSuccessful) {
                val body = response.body()
                val responseText = body?.choices?.firstOrNull()?.message?.content
                if (!responseText.isNullOrEmpty()) {
                    Log.d(TAG, "OpenRouter HTTP ${response.code()} Success | Response Length: ${responseText.length}")
                    Result.success(responseText)
                } else if (body?.error != null) {
                    val serverErrMsg = body.error.message ?: "Unknown server error"
                    Log.e(TAG, "OpenRouter 200 with Error JSON: $serverErrMsg")
                    Result.failure(Exception("OpenRouter Error: $serverErrMsg"))
                } else {
                    Result.failure(Exception("Received empty response body from OpenRouter AI."))
                }
            } else {
                val errorCode = response.code()
                val rawErrorBody = response.errorBody()?.string() ?: ""
                Log.e(TAG, "OpenRouter HTTP $errorCode Failure | Model: $cleanModel | Raw Error Body: $rawErrorBody")

                var parsedServerErrorMsg: String? = null
                try {
                    val errorAdapter = moshi.adapter(OpenRouterResponse::class.java)
                    val parsedResp = errorAdapter.fromJson(rawErrorBody)
                    parsedServerErrorMsg = parsedResp?.error?.message
                } catch (_: Exception) {
                    if (rawErrorBody.contains("message")) {
                        val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(rawErrorBody)
                        parsedServerErrorMsg = messageMatch?.groupValues?.get(1)
                    }
                }

                val finalErrorDetail = parsedServerErrorMsg ?: if (rawErrorBody.isNotBlank() && rawErrorBody.length < 200) rawErrorBody else null

                val userFacingError = when (errorCode) {
                    400 -> {
                        val detail = finalErrorDetail ?: "Invalid request structure or unsupported parameter."
                        "Invalid Request (400): $detail"
                    }
                    401 -> {
                        val detail = finalErrorDetail ?: "Authentication failed. Please verify your OpenRouter API key in Settings."
                        "Unauthorized (401): $detail"
                    }
                    402 -> {
                        val detail = finalErrorDetail ?: "Insufficient credits or payment required. Please top up or choose a free model."
                        "Payment Required (402): $detail"
                    }
                    404 -> {
                        val detail = finalErrorDetail ?: "Model '$cleanModel' is not available on OpenRouter or endpoint was not found."
                        "Model Not Found (404): $detail. Please choose an available model in Settings or the model dropdown."
                    }
                    429 -> {
                        val detail = finalErrorDetail ?: "Rate limit reached on OpenRouter. Please wait a moment and try again."
                        "Rate Limit Exceeded (429): $detail"
                    }
                    in 500..599 -> {
                        val detail = finalErrorDetail ?: "OpenRouter provider service error."
                        "Server Error ($errorCode): $detail"
                    }
                    else -> {
                        val detail = finalErrorDetail ?: "Unexpected error response from server."
                        "OpenRouter Error ($errorCode): $detail"
                    }
                }

                Result.failure(Exception(userFacingError))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during OpenRouter call: ${e.localizedMessage}")
            Result.failure(Exception(e.localizedMessage ?: "Failed to connect to OpenRouter service. Please check your network connection."))
        }
    }
}
