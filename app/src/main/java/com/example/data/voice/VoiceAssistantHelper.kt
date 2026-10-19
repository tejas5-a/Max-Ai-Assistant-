package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object VoiceAssistantHelper {

    fun openDefaultAssistantSettings(context: Context): Boolean {
        return try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    val assistantCapabilitiesExplanation = """
        MAX AI Assistant provides in-app conversational voice assistance.
        
        • In-App Hands-Free Live Voice: Full multi-turn conversation with interruption support.
        • System Default App Settings: You can set MAX AI as your preferred Assist & Voice Input app in Android System Settings.
        • Transparent Limitations: As a native app on modern Android (Android 14+), system-level power-button triggers, always-on lockscreen wake words, and screen overlays are controlled by device OEM policies and privileged system services.
    """.trimIndent()
}
