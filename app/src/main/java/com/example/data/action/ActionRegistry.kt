package com.example.data.action

enum class CapabilityStatus {
    AVAILABLE,
    REQUIRES_PERMISSION,
    REQUIRES_CONFIRMATION,
    REQUIRES_UNLOCKED_DEVICE,
    UNSUPPORTED
}

data class CapabilityInfo(
    val id: String,
    val name: String,
    val category: String, // System, Apps, Communication, Notifications, Voice, Automation
    val implementationClass: String,
    val requiredPermission: String?,
    val status: CapabilityStatus,
    val description: String
)

object ActionRegistry {

    fun getCapabilityMatrix(): List<CapabilityInfo> {
        return listOf(
            CapabilityInfo(
                id = "flashlight",
                name = "Flashlight Toggle",
                category = "System",
                implementationClass = "android.hardware.camera2.CameraManager",
                requiredPermission = "android.permission.CAMERA",
                status = CapabilityStatus.REQUIRES_PERMISSION,
                description = "Turn device camera LED flashlight on or off."
            ),
            CapabilityInfo(
                id = "media_volume",
                name = "Sound & Volume Control",
                category = "System",
                implementationClass = "android.media.AudioManager",
                requiredPermission = "android.permission.MODIFY_AUDIO_SETTINGS",
                status = CapabilityStatus.AVAILABLE,
                description = "Adjust media volume, mute, unmute, or set maximum volume."
            ),
            CapabilityInfo(
                id = "display_settings",
                name = "Brightness & Display Settings",
                category = "System",
                implementationClass = "android.provider.Settings.ACTION_DISPLAY_SETTINGS",
                requiredPermission = null,
                status = CapabilityStatus.AVAILABLE,
                description = "Open device display and brightness settings screen."
            ),
            CapabilityInfo(
                id = "device_status",
                name = "Device Status & Battery Telemetry",
                category = "System",
                implementationClass = "android.os.BatteryManager",
                requiredPermission = null,
                status = CapabilityStatus.AVAILABLE,
                description = "Query exact battery percentage, charging state, date, and time."
            ),
            CapabilityInfo(
                id = "app_launcher",
                name = "Launch Installed Applications",
                category = "Apps",
                implementationClass = "android.content.pm.PackageManager",
                requiredPermission = null,
                status = CapabilityStatus.AVAILABLE,
                description = "Launch apps like YouTube, Chrome, Maps, Camera, Gallery, Calculator, etc."
            ),
            CapabilityInfo(
                id = "system_settings",
                name = "System Settings Navigation",
                category = "Apps",
                implementationClass = "android.provider.Settings",
                requiredPermission = null,
                status = CapabilityStatus.AVAILABLE,
                description = "Open Wi-Fi, Bluetooth, Sound, Display, and Voice Assistant settings."
            ),
            CapabilityInfo(
                id = "alarm_timer",
                name = "Alarms & Timers",
                category = "Automation",
                implementationClass = "android.provider.AlarmClock",
                requiredPermission = "android.permission.SCHEDULE_EXACT_ALARM",
                status = CapabilityStatus.AVAILABLE,
                description = "Set exact system alarms and countdown timers."
            ),
            CapabilityInfo(
                id = "tasks_reminders",
                name = "Reminders & Scheduled Tasks",
                category = "Automation",
                implementationClass = "com.example.data.repository.TaskRepository",
                requiredPermission = null,
                status = CapabilityStatus.AVAILABLE,
                description = "Create persistent reminders with priority levels and scheduled triggers."
            ),
            CapabilityInfo(
                id = "notifications_access",
                name = "Notification Post & Access",
                category = "Notifications",
                implementationClass = "android.app.NotificationManager",
                requiredPermission = "android.permission.POST_NOTIFICATIONS",
                status = CapabilityStatus.REQUIRES_PERMISSION,
                description = "Post assistant notifications and routine execution alerts."
            ),
            CapabilityInfo(
                id = "phone_communication",
                name = "Phone Calls & Messages Preparation",
                category = "Communication",
                implementationClass = "android.content.Intent.ACTION_DIAL",
                requiredPermission = null,
                status = CapabilityStatus.REQUIRES_CONFIRMATION,
                description = "Prepare phone call dialer or SMS composer with explicit user confirmation."
            ),
            CapabilityInfo(
                id = "voice_assistant_role",
                name = "Android Default Assistant Service",
                category = "Voice",
                implementationClass = "com.example.service.MaxVoiceInteractionService",
                requiredPermission = "android.permission.BIND_VOICE_INTERACTION",
                status = CapabilityStatus.AVAILABLE,
                description = "Integrated Android VoiceInteractionService for system assist triggers."
            ),
            CapabilityInfo(
                id = "universal_third_party_control",
                name = "Universal Third-Party App Control",
                category = "Cross-App Workflows",
                implementationClass = "N/A",
                requiredPermission = null,
                status = CapabilityStatus.UNSUPPORTED,
                description = "Autonomous remote control over arbitrary 3rd party internal UIs is prohibited by Android security policies."
            )
        )
    }
}
