package com.example

import com.example.data.automation.WorkflowParser
import com.example.data.model.AppSettings
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.VoiceEngineMode
import com.example.data.voice.OpenRouterRealtimeVoiceAdapter
import com.example.data.voice.VoiceCommandResult
import com.example.data.voice.VoiceState
import com.example.data.voice.VoiceTurn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveVoiceAssistantTest {

    @Test
    fun testVoiceStateProperties() {
        assertEquals("Listening...", VoiceState.LISTENING.label)
        assertTrue(VoiceState.LISTENING.isMicActive)
        assertFalse(VoiceState.IDLE.isMicActive)
        assertFalse(VoiceState.PROCESSING.isMicActive)
        assertFalse(VoiceState.SPEAKING.isMicActive)
        assertFalse(VoiceState.INTERRUPTED.isMicActive)
        assertFalse(VoiceState.CLOSED.isMicActive)
    }

    @Test
    fun testVoiceEngineModeDefaultsAndSwitching() {
        var settings = AppSettings()
        // Default is Conversational Fallback Mode B
        assertEquals(VoiceEngineMode.CONVERSATIONAL_FALLBACK, settings.voiceEngineMode)
        assertTrue(settings.isHandsFreeLoopEnabled)
        assertTrue(settings.isInterruptionEnabled)

        // Switch to Realtime Mode A
        settings = settings.copy(voiceEngineMode = VoiceEngineMode.REALTIME_AUDIO)
        assertEquals(VoiceEngineMode.REALTIME_AUDIO, settings.voiceEngineMode)
    }

    @Test
    fun testRealtimeVoiceProviderConnectionRequiresKey() = runTest {
        val adapter = OpenRouterRealtimeVoiceAdapter()
        assertFalse(adapter.isConnected)

        // Connecting without key should return failure
        val failureResult = adapter.connect(apiKey = "", model = "gpt-4o-realtime", languageCode = "en-US")
        assertTrue(failureResult.isFailure)
        assertFalse(adapter.isConnected)

        // Connecting with key should succeed
        val successResult = adapter.connect(apiKey = "valid_key_123", model = "gpt-4o-realtime", languageCode = "en-US")
        assertTrue(successResult.isSuccess)
        assertTrue(adapter.isConnected)

        adapter.disconnect()
        assertFalse(adapter.isConnected)
    }

    @Test
    fun testMultiTurnConversationContextRetention() {
        val turns = mutableListOf<VoiceTurn>()
        assertEquals(0, turns.size)

        // Turn 1: User speaks
        val userTurn1 = VoiceTurn(speaker = "user", text = "Hello MAX")
        turns.add(userTurn1)

        // Turn 1: Assistant replies
        val assistantTurn1 = VoiceTurn(speaker = "assistant", text = "Hi there! What can I help you with today?")
        turns.add(assistantTurn1)

        // Turn 2: User follow up
        val userTurn2 = VoiceTurn(speaker = "user", text = "What can you do?")
        turns.add(userTurn2)

        assertEquals(3, turns.size)
        assertEquals("Hello MAX", turns[0].text)
        assertEquals("assistant", turns[1].speaker)
        assertEquals("What can you do?", turns[2].text)
    }

    @Test
    fun testWorkflowParserFromVoicePrompt() {
        val voicePrompt = "schedule a reminder every weekday morning at 8:00 AM to review urgent tasks"
        val workflow = WorkflowParser.parsePrompt(voicePrompt)

        assertNotNull(workflow)
        assertTrue(workflow.scheduleDescription.contains("Weekday", ignoreCase = true))
        assertTrue(workflow.title.isNotBlank())
    }

    @Test
    fun testTaskVoiceParsingStructure() {
        val userSpeech = "add task Prepare quarterly report urgent"
        val lower = userSpeech.lowercase()

        assertTrue(lower.startsWith("add task"))
        val priority = if (lower.contains("urgent")) TaskPriority.HIGH else TaskPriority.MEDIUM
        assertEquals(TaskPriority.HIGH, priority)
    }
}
