package com.example.data.action

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class RequiresConfirmation(val message: String, val pendingIntent: Intent?) : ActionResult()
    data class Error(val message: String) : ActionResult()
}

enum class VolumeAction { UP, DOWN, MUTE, MAX }

class ActionExecutor(private val context: Context) {

    fun setFlashlight(enabled: Boolean): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult.Error("Flashlight hardware not available on this device.")

            cameraManager.setTorchMode(cameraId, enabled)
            val stateStr = if (enabled) "turned ON" else "turned OFF"
            ActionResult.Success("Flashlight $stateStr.")
        } catch (e: Exception) {
            ActionResult.Error("Unable to control flashlight: ${e.message}")
        }
    }

    fun adjustVolume(action: VolumeAction): ActionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            when (action) {
                VolumeAction.UP -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    ActionResult.Success("Volume turned up.")
                }
                VolumeAction.DOWN -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    ActionResult.Success("Volume turned down.")
                }
                VolumeAction.MUTE -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    ActionResult.Success("Media volume muted.")
                }
                VolumeAction.MAX -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
                    ActionResult.Success("Media volume set to maximum.")
                }
            }
        } catch (e: Exception) {
            ActionResult.Error("Failed to adjust volume: ${e.message}")
        }
    }

    fun openApp(targetApp: String): ActionResult {
        val lower = targetApp.lowercase().trim()
        val pm = context.packageManager

        // Known popular app package mappings
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "camera" to "com.google.android.GoogleCamera",
            "whatsapp" to "com.whatsapp",
            "gmail" to "com.google.android.gm"
        )

        val directPkg = knownPackages[lower]
        if (directPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ActionResult.Success("Opening ${targetApp.replaceFirstChar { it.uppercase() }}.")
            }
        }

        // Generic intent falls for system apps
        val systemActionIntent = when (lower) {
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            "wifi", "wi-fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "display", "brightness" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "sound", "volume settings" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "assistant settings", "voice input" -> Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            "dialer", "phone", "calls" -> Intent(Intent.ACTION_DIAL)
            "browser" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            else -> null
        }

        if (systemActionIntent != null) {
            systemActionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(systemActionIntent)
            return ActionResult.Success("Opening ${targetApp.replaceFirstChar { it.uppercase() }}.")
        }

        // Search installed applications by app label
        try {
            val installedApps = pm.getInstalledApplications(0)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(lower) || lower.contains(label)) {
                    val intent = pm.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return ActionResult.Success("Opening ${pm.getApplicationLabel(app)}.")
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through
        }

        // Fallback: search web / play store or browser search
        val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, targetApp)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(webSearchIntent)
            ActionResult.Success("Searching for '$targetApp'.")
        } catch (e: Exception) {
            ActionResult.Error("Could not find or launch application '$targetApp'.")
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "MAX Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            ActionResult.Success("Alarm set for $timeFormatted.")
        } catch (e: Exception) {
            ActionResult.Error("Unable to set alarm: ${e.message}")
        }
    }

    fun setTimer(seconds: Int, message: String): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "MAX Timer" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val minutes = seconds / 60
            val label = if (minutes > 0) "$minutes minute(s)" else "$seconds second(s)"
            ActionResult.Success("Timer set for $label.")
        } catch (e: Exception) {
            ActionResult.Error("Unable to set timer: ${e.message}")
        }
    }

    fun getDeviceStatus(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 100

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val timeFormat = SimpleDateFormat("h:mm a, EEEE, MMM d, yyyy", Locale.getDefault())
        val currentTime = timeFormat.format(Date())

        val chargingStr = if (isCharging) "charging" else "discharging"
        return "Current time: $currentTime. Battery: $batteryPct% ($chargingStr)."
    }

    fun prepareCall(phoneNumber: String): ActionResult {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.trim()}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActionResult.RequiresConfirmation(
            message = "Ready to call $phoneNumber. Tap confirm to proceed.",
            pendingIntent = intent
        )
    }
}
