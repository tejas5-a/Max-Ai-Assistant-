package com.example.data.orchestrator

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class PermissionStatusItem(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean
)

class PermissionManager(private val context: Context) {

    fun getPermissionDashboard(): List<PermissionStatusItem> {
        return listOf(
            PermissionStatusItem(
                permission = Manifest.permission.RECORD_AUDIO,
                title = "Microphone Access",
                description = "Required for natural multi-turn voice conversations and real-time Gemini Live AI speech input.",
                isGranted = isPermissionGranted(Manifest.permission.RECORD_AUDIO)
            ),
            PermissionStatusItem(
                permission = Manifest.permission.CAMERA,
                title = "Camera & Flashlight",
                description = "Required for controlling camera LED flashlight commands.",
                isGranted = isPermissionGranted(Manifest.permission.CAMERA)
            ),
            PermissionStatusItem(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                title = "Notifications",
                description = "Required for posting automation routine status and scheduled reminder alerts.",
                isGranted = isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
            ),
            PermissionStatusItem(
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                title = "Location Access",
                description = "Required for location-based contextual queries.",
                isGranted = isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        )
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
