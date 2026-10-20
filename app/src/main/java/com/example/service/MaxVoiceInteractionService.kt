package com.example.service

import android.os.Bundle
import android.service.voice.VoiceInteractionService

class MaxVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
    }

    override fun onShutdown() {
        super.onShutdown()
    }

    fun launchAssistantSession(args: Bundle? = null) {
        showSession(args ?: Bundle(), 0)
    }
}
