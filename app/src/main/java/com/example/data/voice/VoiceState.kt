package com.example.data.voice

enum class VoiceState(val label: String, val isMicActive: Boolean) {
    IDLE("Ready", false),
    CONNECTING("Connecting...", false),
    LISTENING("Listening...", true),
    PROCESSING("Thinking...", false),
    GENERATING("Generating response...", false),
    SPEAKING("Speaking...", false),
    INTERRUPTED("Interrupted", false),
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
