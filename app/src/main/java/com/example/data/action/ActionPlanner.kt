package com.example.data.action

import android.content.Context

sealed class PlannedIntent {
    data class Flashlight(val enabled: Boolean) : PlannedIntent()
    data class Volume(val action: VolumeAction) : PlannedIntent()
    data class OpenApp(val appName: String) : PlannedIntent()
    data class OpenSettings(val settingType: String) : PlannedIntent()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : PlannedIntent()
    data class SetTimer(val seconds: Int, val label: String) : PlannedIntent()
    object GetDeviceStatus : PlannedIntent()
    data class PrepareCall(val number: String) : PlannedIntent()
    object StopCurrentTask : PlannedIntent()
    object PassToAi : PlannedIntent()
}

class ActionPlanner(private val context: Context) {
    private val actionExecutor = ActionExecutor(context)

    fun planIntent(userText: String): PlannedIntent {
        val lower = userText.trim().lowercase()

        // 1. Stop command
        if (lower == "stop" || lower == "cancel" || lower == "stop task" || lower.startsWith("stop current task")) {
            return PlannedIntent.StopCurrentTask
        }

        // 2. Flashlight controls
        if (lower.contains("flashlight on") || lower.contains("turn on flashlight") || lower.contains("torch on") || lower.contains("turn on torch")) {
            return PlannedIntent.Flashlight(enabled = true)
        }
        if (lower.contains("flashlight off") || lower.contains("turn off flashlight") || lower.contains("torch off") || lower.contains("turn off torch")) {
            return PlannedIntent.Flashlight(enabled = false)
        }

        // 3. Volume controls
        if (lower.contains("volume up") || lower.contains("increase volume") || lower.contains("louder")) {
            return PlannedIntent.Volume(VolumeAction.UP)
        }
        if (lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("quieter")) {
            return PlannedIntent.Volume(VolumeAction.DOWN)
        }
        if (lower.contains("mute volume") || lower.contains("mute audio") || lower.contains("silence phone")) {
            return PlannedIntent.Volume(VolumeAction.MUTE)
        }
        if (lower.contains("max volume") || lower.contains("maximum volume") || lower.contains("full volume")) {
            return PlannedIntent.Volume(VolumeAction.MAX)
        }

        // 4. Device status / Time / Battery queries
        if (lower.contains("what time is it") || lower.contains("current time") || lower.contains("battery level") || lower.contains("battery percentage") || lower == "time" || lower == "battery") {
            return PlannedIntent.GetDeviceStatus
        }

        // 5. Open Apps / Settings
        if (lower.startsWith("open ") || lower.startsWith("launch ")) {
            val appOrSetting = lower.removePrefix("open ").removePrefix("launch ").trim()
            if (appOrSetting == "settings" || appOrSetting.endsWith("settings")) {
                return PlannedIntent.OpenSettings(appOrSetting)
            }
            return PlannedIntent.OpenApp(appOrSetting)
        }

        // 6. Alarms and Timers
        if (lower.contains("set alarm") || lower.contains("set an alarm") || lower.contains("wake me up at")) {
            // Default 7:00 AM or extract simple hour
            var hour = 7
            var minute = 0
            if (lower.contains("8") || lower.contains("eight")) hour = 8
            if (lower.contains("9") || lower.contains("nine")) hour = 9
            if (lower.contains("6") || lower.contains("six")) hour = 6
            return PlannedIntent.SetAlarm(hour = hour, minute = minute, label = "MAX Alarm")
        }

        if (lower.contains("set timer") || lower.contains("set a timer") || lower.contains("timer for")) {
            var seconds = 300 // 5 minutes default
            if (lower.contains("1 minute") || lower.contains("one minute")) seconds = 60
            if (lower.contains("10 minute") || lower.contains("ten minute")) seconds = 600
            if (lower.contains("15 minute") || lower.contains("fifteen minute")) seconds = 900
            return PlannedIntent.SetTimer(seconds = seconds, label = "MAX Timer")
        }

        // 7. Phone call prep
        if (lower.startsWith("call ") || lower.startsWith("dial ")) {
            val number = lower.removePrefix("call ").removePrefix("dial ").trim()
            if (number.isNotBlank()) {
                return PlannedIntent.PrepareCall(number)
            }
        }

        return PlannedIntent.PassToAi
    }

    fun executePlannedIntent(intent: PlannedIntent): ActionResult? {
        return when (intent) {
            is PlannedIntent.Flashlight -> actionExecutor.setFlashlight(intent.enabled)
            is PlannedIntent.Volume -> actionExecutor.adjustVolume(intent.action)
            is PlannedIntent.OpenApp -> actionExecutor.openApp(intent.appName)
            is PlannedIntent.OpenSettings -> actionExecutor.openApp(intent.settingType)
            is PlannedIntent.SetAlarm -> actionExecutor.setAlarm(intent.hour, intent.minute, intent.label)
            is PlannedIntent.SetTimer -> actionExecutor.setTimer(intent.seconds, intent.label)
            is PlannedIntent.GetDeviceStatus -> ActionResult.Success(actionExecutor.getDeviceStatus())
            is PlannedIntent.PrepareCall -> actionExecutor.prepareCall(intent.number)
            is PlannedIntent.StopCurrentTask -> ActionResult.Success("Stopped current task.")
            is PlannedIntent.PassToAi -> null
        }
    }
}
