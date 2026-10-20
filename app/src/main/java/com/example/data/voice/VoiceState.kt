package com.example.data.voice

enum class VoiceState(val label: String, val isMicActive: Boolean) {
    IDLE("Ready", false),
    REQUESTING_PERMISSION("Requesting Permission...", false),
    CONNECTING("Connecting...", false),
    LISTENING("Listening...", true),
    PROCESSING_AUDIO("Processing Audio...", false),
    PROCESSING("Processing...", false),
    THINKING("Thinking...", false),
    GENERATING("Generating response...", false),
    SPEAKING("Speaking...", false),
    INTERRUPTED("Interrupted", false),
    RECONNECTING("Reconnecting...", false),
    ERROR("Error", false),
    CLOSED("Session Ended", false)
}

data class VoiceTurn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val speaker: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val actionDetail: String? = null
)
