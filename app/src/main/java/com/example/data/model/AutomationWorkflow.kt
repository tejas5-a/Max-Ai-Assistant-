package com.example.data.model

enum class WorkflowStatus {
    DRAFT,
    AWAITING_REVIEW,
    SCHEDULED,
    DUE,
    APPROVED,
    RUNNING,
    COMPLETED,
    FAILED,
    DELAYED,
    CANCELLED,
    DISABLED
}

enum class WorkflowActionType {
    TASK_REMINDER,
    DAILY_BRIEFING,
    WEB_RESEARCH,
    AI_SUMMARY,
    CUSTOM_NOTIFICATION
}

data class AutomationWorkflow(
    val id: String,
    val title: String,
    val naturalLanguagePrompt: String,
    val scheduleDescription: String, // e.g., "Every weekday at 8:00 AM"
    val actionType: WorkflowActionType,
    val actionPayload: String, // JSON payload or prompt detail
    val status: WorkflowStatus = WorkflowStatus.SCHEDULED,
    val isEnabled: Boolean = true,
    val lastRunMillis: Long? = null,
    val nextRunMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
