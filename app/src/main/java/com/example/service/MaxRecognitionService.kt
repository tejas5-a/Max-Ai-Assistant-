package com.example.service

import android.content.Intent
import android.speech.RecognitionService

class MaxRecognitionService : RecognitionService() {

    override fun onStartListening(intent: Intent?, listener: Callback?) {
        // Recognition logic delegated to VoiceManager/SpeechRecognizer in application
    }

    override fun onCancel(listener: Callback?) {
    }

    override fun onStopListening(listener: Callback?) {
    }
}
