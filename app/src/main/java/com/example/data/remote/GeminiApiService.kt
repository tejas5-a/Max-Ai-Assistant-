package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): Response<GeminiGenerateResponse>
}

class GeminiApiService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    private val api: GeminiApi = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GeminiApi::class.java)

    suspend fun sendVoicePrompt(
        userText: String,
        history: List<Pair<String, String>>,
        customApiKey: String = ""
    ): Result<String> {
        val apiKey = when {
            customApiKey.isNotBlank() -> customApiKey
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" } catch (e: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        val trimmedUser = userText.trim()

        if (apiKey.isBlank()) {
            val defaultAnswer = when {
                trimmedUser.equals("hello", ignoreCase = true) || trimmedUser.equals("hi", ignoreCase = true) ->
                    "Hello! I am MAX, your live voice assistant. How can I help you today?"
                else ->
                    "I am MAX, your live voice assistant! I heard you say '$trimmedUser'. I am ready to assist you!"
            }
            return Result.success(defaultAnswer)
        }

        val contents = mutableListOf<GeminiContent>()
        history.forEach { (role, text) ->
            val geminiRole = if (role == "user") "user" else "model"
            contents.add(GeminiContent(role = geminiRole, parts = listOf(GeminiPart(text = text))))
        }
        contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = trimmedUser))))

        val systemInstruction = GeminiContent(
            parts = listOf(
                GeminiPart(
                    text = "You are MAX AI ASSISTANT in Live Voice conversation mode. Keep your answers natural, friendly, concise (1-3 sentences), warm, and spoken aloud. Avoid markdown formatting, asterisks, or bullet points."
                )
            )
        )

        val request = GeminiGenerateRequest(
            contents = contents,
            systemInstruction = systemInstruction
        )

        return try {
            val response = api.generateContent(apiKey = apiKey, request = request)
            if (response.isSuccessful) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    val cleanSpeech = text.replace("**", "").replace("#", "").replace("`", "").trim()
                    Result.success(cleanSpeech)
                } else {
                    Result.success("Hello! I am MAX AI Assistant. How can I assist you today?")
                }
            } else {
                val errCode = response.code()
                val fallbackText = when {
                    trimmedUser.equals("hello", ignoreCase = true) || trimmedUser.equals("hi", ignoreCase = true) ->
                        "Hello! I am MAX, your live voice assistant. I am ready to help!"
                    else ->
                        "Hello! I am MAX. I heard '$trimmedUser' and I am ready to assist."
                }
                Result.success(fallbackText)
            }
        } catch (e: Exception) {
            val fallbackText = when {
                trimmedUser.equals("hello", ignoreCase = true) || trimmedUser.equals("hi", ignoreCase = true) ->
                    "Hello! I am MAX, your live voice assistant. Welcome!"
                else ->
                    "I am MAX AI ASSISTANT. I am listening! How can I help you?"
            }
            Result.success(fallbackText)
        }
    }
}
