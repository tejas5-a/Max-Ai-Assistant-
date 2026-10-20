package com.example.data.model

import com.example.ui.theme.ThemeMode

enum class AppLanguage(val code: String, val displayName: String) {
    AUTO("auto", "Auto Detect"),
    ENGLISH("en", "English"),
    HINDI("hi", "Hindi (हिंदी)"),
    HINGLISH("hinglish", "Hinglish"),
    SPANISH("es", "Spanish (Español)"),
    FRENCH("fr", "French (Français)"),
    REGIONAL("regional", "Regional Language")
}

enum class VoiceEngineMode(val displayName: String, val description: String) {
    REALTIME_AUDIO("Realtime Voice (Mode A)", "Low-latency bidirectional audio streaming engine"),
    CONVERSATIONAL_FALLBACK("Conversational Fallback (Mode B)", "Multi-turn hands-free voice loop using STT + OpenRouter + TTS")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedLanguage: AppLanguage = AppLanguage.AUTO,
    
    // OpenRouter Provider Configuration
    val isOpenRouterEnabled: Boolean = true,
    val openRouterApiKey: String = "",
    val selectedOpenRouterModel: String = "meta-llama/llama-3.3-70b-instruct:free",
    val selectedOpenRouterModelName: String = "Llama 3.3 70B (Free)",
    
    // Pollinations Provider Configuration (Fully Independent!)
    val isPollinationsEnabled: Boolean = true,
    val selectedPollinationsModel: String = "flux",
    val defaultAspectRatio: String = "1:1",
    
    // MAX Live Voice Settings
    val voiceEngineMode: VoiceEngineMode = VoiceEngineMode.CONVERSATIONAL_FALLBACK,
    val realtimeVoiceApiKey: String = "",
    val isSpeechOutputEnabled: Boolean = true,
    val isAutoReadResponses: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val isHandsFreeLoopEnabled: Boolean = true,
    val isInterruptionEnabled: Boolean = true,
    
    // General App Settings
    val isOnboardingCompleted: Boolean = false,
    val isGuestMode: Boolean = true
)
