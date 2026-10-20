package com.example.data.voice

import android.util.Log

object VoiceLogger {
    private const val TAG = "MAX_VoiceEngine"

    fun logSessionStart(sessionType: String) {
        Log.i(TAG, "Voice session started: $sessionType")
    }

    fun logSessionStop(reason: String) {
        Log.i(TAG, "Voice session stopped: $reason")
    }

    fun logAudioFormat(sampleRate: Int, channels: Int, encodingBits: Int) {
        Log.i(TAG, "Audio Format: $sampleRate Hz, $channels channel(s), $encodingBits bit PCM")
    }

    fun logFrameStats(totalFrames: Long, droppedFrames: Long) {
        Log.d(TAG, "Frame Stats - Total: $totalFrames, Dropped: $droppedFrames")
    }

    fun logPermissionStatus(permission: String, isGranted: Boolean) {
        if (isGranted) {
            Log.i(TAG, "Permission GRANTED: $permission")
        } else {
            Log.w(TAG, "Permission DENIED: $permission")
        }
    }

    fun logProviderState(provider: String, status: String) {
        Log.i(TAG, "Provider [$provider] status: $status")
    }

    fun logError(category: String, message: String) {
        Log.e(TAG, "[$category Error] $message")
    }
}
