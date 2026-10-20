package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.VoiceEngineMode
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "max_app_settings")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("selected_language")
        val OPENROUTER_ENABLED = booleanPreferencesKey("openrouter_enabled")
        val OPENROUTER_KEY = stringPreferencesKey("openrouter_key")
        val OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val OPENROUTER_MODEL_NAME = stringPreferencesKey("openrouter_model_name")
        val POLLINATIONS_ENABLED = booleanPreferencesKey("pollinations_enabled")
        val POLLINATIONS_MODEL = stringPreferencesKey("pollinations_model")
        val POLLINATIONS_ASPECT = stringPreferencesKey("pollinations_aspect")
        
        // Voice & Live Assistant Keys
        val VOICE_ENGINE_MODE = stringPreferencesKey("voice_engine_mode")
        val REALTIME_VOICE_KEY = stringPreferencesKey("realtime_voice_key")
        val SPEECH_OUTPUT = booleanPreferencesKey("speech_output")
        val AUTO_READ_RESPONSES = booleanPreferencesKey("auto_read_responses")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val HANDS_FREE_LOOP = booleanPreferencesKey("hands_free_loop")
        val INTERRUPTION_ENABLED = booleanPreferencesKey("interruption_enabled")
        
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = try {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) { ThemeMode.SYSTEM },
            selectedLanguage = try {
                AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.AUTO.name)
            } catch (e: Exception) { AppLanguage.AUTO },
            isOpenRouterEnabled = prefs[Keys.OPENROUTER_ENABLED] ?: true,
            openRouterApiKey = prefs[Keys.OPENROUTER_KEY] ?: "",
            selectedOpenRouterModel = prefs[Keys.OPENROUTER_MODEL] ?: "meta-llama/llama-3.3-70b-instruct:free",
            selectedOpenRouterModelName = prefs[Keys.OPENROUTER_MODEL_NAME] ?: "Llama 3.3 70B (Free)",
            isPollinationsEnabled = prefs[Keys.POLLINATIONS_ENABLED] ?: true,
            selectedPollinationsModel = prefs[Keys.POLLINATIONS_MODEL] ?: "flux",
            defaultAspectRatio = prefs[Keys.POLLINATIONS_ASPECT] ?: "1:1",
            
            voiceEngineMode = try {
                VoiceEngineMode.valueOf(prefs[Keys.VOICE_ENGINE_MODE] ?: VoiceEngineMode.CONVERSATIONAL_FALLBACK.name)
            } catch (e: Exception) { VoiceEngineMode.CONVERSATIONAL_FALLBACK },
            realtimeVoiceApiKey = prefs[Keys.REALTIME_VOICE_KEY] ?: "",
            isSpeechOutputEnabled = prefs[Keys.SPEECH_OUTPUT] ?: true,
            isAutoReadResponses = prefs[Keys.AUTO_READ_RESPONSES] ?: false,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            speechPitch = prefs[Keys.SPEECH_PITCH] ?: 1.0f,
            isHandsFreeLoopEnabled = prefs[Keys.HANDS_FREE_LOOP] ?: true,
            isInterruptionEnabled = prefs[Keys.INTERRUPTION_ENABLED] ?: true,
            
            isOnboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            isGuestMode = prefs[Keys.GUEST_MODE] ?: true
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateLanguage(lang: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang.name }
    }

    suspend fun setOpenRouterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OPENROUTER_ENABLED] = enabled }
    }

    suspend fun setOpenRouterApiKey(key: String) {
        context.dataStore.edit { it[Keys.OPENROUTER_KEY] = key.trim() }
    }

    suspend fun setOpenRouterModel(modelId: String, modelName: String = "") {
        context.dataStore.edit { prefs ->
            prefs[Keys.OPENROUTER_MODEL] = modelId.trim()
            if (modelName.isNotBlank()) {
                prefs[Keys.OPENROUTER_MODEL_NAME] = modelName.trim()
            }
        }
    }

    suspend fun setPollinationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.POLLINATIONS_ENABLED] = enabled }
    }

    suspend fun setPollinationsModel(model: String) {
        context.dataStore.edit { it[Keys.POLLINATIONS_MODEL] = model }
    }

    suspend fun setPollinationsAspect(aspect: String) {
        context.dataStore.edit { it[Keys.POLLINATIONS_ASPECT] = aspect }
    }

    suspend fun setVoiceEngineMode(mode: VoiceEngineMode) {
        context.dataStore.edit { it[Keys.VOICE_ENGINE_MODE] = mode.name }
    }

    suspend fun setRealtimeVoiceApiKey(key: String) {
        context.dataStore.edit { it[Keys.REALTIME_VOICE_KEY] = key.trim() }
    }

    suspend fun setSpeechOutputEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SPEECH_OUTPUT] = enabled }
    }

    suspend fun setAutoReadResponses(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_READ_RESPONSES] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { it[Keys.SPEECH_PITCH] = pitch }
    }

    suspend fun setHandsFreeLoopEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HANDS_FREE_LOOP] = enabled }
    }

    suspend fun setInterruptionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INTERRUPTION_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
