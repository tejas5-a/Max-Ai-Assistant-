package com.example.data.automation

import com.example.data.model.AutomationWorkflow
import com.example.data.model.WorkflowActionType
import com.example.data.model.WorkflowStatus
import java.util.UUID

object WorkflowParser {

    fun parsePrompt(prompt: String): AutomationWorkflow {
        val lower = prompt.lowercase()

        val actionType = when {
            lower.contains("briefing") || lower.contains("morning summary") || lower.contains("news") -> WorkflowActionType.DAILY_BRIEFING
            lower.contains("search") || lower.contains("research") -> WorkflowActionType.WEB_RESEARCH
            lower.contains("summarize") || lower.contains("ai") -> WorkflowActionType.AI_SUMMARY
            lower.contains("task") || lower.contains("remind") || lower.contains("todo") -> WorkflowActionType.TASK_REMINDER
            else -> WorkflowActionType.CUSTOM_NOTIFICATION
        }

        val scheduleDesc = when {
            lower.contains("every weekday") -> "Every Weekday at 8:00 AM"
            lower.contains("every morning") || lower.contains("daily") -> "Every Day at 8:00 AM"
            lower.contains("every evening") || lower.contains("night") -> "Every Day at 8:00 PM"
            lower.contains("hourly") || lower.contains("every hour") -> "Every Hour"
            else -> "Daily at 9:00 AM"
        }

        val title = when (actionType) {
            WorkflowActionType.DAILY_BRIEFING -> "Morning Productivity Briefing"
            WorkflowActionType.WEB_RESEARCH -> "Scheduled Web Research"
            WorkflowActionType.AI_SUMMARY -> "Daily AI Overview"
            WorkflowActionType.TASK_REMINDER -> "Task Check & Reminder"
            WorkflowActionType.CUSTOM_NOTIFICATION -> "Automated Notification"
        }

        return AutomationWorkflow(
            id = UUID.randomUUID().toString(),
            title = title,
            naturalLanguagePrompt = prompt,
            scheduleDescription = scheduleDesc,
            actionType = actionType,
            actionPayload = "{\"prompt\": \"${prompt.replace("\"", "\\\"")}\"}",
            status = WorkflowStatus.AWAITING_REVIEW,
            isEnabled = true,
            createdAt = System.currentTimeMillis()
        )
    }
}
