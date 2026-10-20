package com.example.data.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutomationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Automation schedules recover automatically via WorkManager persistent schedules
        }
    }
}
