package com.example.data.model

enum class TaskPriority {
    HIGH, MEDIUM, LOW
}

data class TaskItem(
    val id: String,
    val title: String,
    val description: String = "",
    val dueDateMillis: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val isReminderEnabled: Boolean = false,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)
