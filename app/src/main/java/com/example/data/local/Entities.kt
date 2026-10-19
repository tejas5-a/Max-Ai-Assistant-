package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AutomationWorkflow
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.SearchSource
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.model.WorkflowActionType
import com.example.data.model.WorkflowStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastMessage: String
) {
    fun toDomain() = Conversation(id, title, createdAt, updatedAt, lastMessage)
    companion object {
        fun fromDomain(conv: Conversation) = ConversationEntity(
            conv.id, conv.title, conv.createdAt, conv.updatedAt, conv.lastMessage
        )
    }
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val model: String?,
    val imageUrl: String?,
    val sourcesJson: String?,
    val isError: Boolean
) {
    fun toDomain(moshi: Moshi): ChatMessage {
        val sources = sourcesJson?.let {
            val type = Types.newParameterizedType(List::class.java, SearchSource::class.java)
            moshi.adapter<List<SearchSource>>(type).fromJson(it)
        } ?: emptyList()

        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = role,
            content = content,
            timestamp = timestamp,
            model = model,
            imageUrl = imageUrl,
            sources = sources,
            isError = isError,
            isStreaming = false
        )
    }

    companion object {
        fun fromDomain(msg: ChatMessage, moshi: Moshi): ChatMessageEntity {
            val type = Types.newParameterizedType(List::class.java, SearchSource::class.java)
            val json = if (msg.sources.isNotEmpty()) {
                moshi.adapter<List<SearchSource>>(type).toJson(msg.sources)
            } else null

            return ChatMessageEntity(
                id = msg.id,
                conversationId = msg.conversationId,
                role = msg.role,
                content = msg.content,
                timestamp = msg.timestamp,
                model = msg.model,
                imageUrl = msg.imageUrl,
                sourcesJson = json,
                isError = msg.isError
            )
        }
    }
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dueDateMillis: Long?,
    val priorityName: String,
    val isCompleted: Boolean,
    val isReminderEnabled: Boolean,
    val category: String,
    val createdAt: Long
) {
    fun toDomain() = TaskItem(
        id = id,
        title = title,
        description = description,
        dueDateMillis = dueDateMillis,
        priority = try { TaskPriority.valueOf(priorityName) } catch (e: Exception) { TaskPriority.MEDIUM },
        isCompleted = isCompleted,
        isReminderEnabled = isReminderEnabled,
        category = category,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(task: TaskItem) = TaskEntity(
            id = task.id,
            title = task.title,
            description = task.description,
            dueDateMillis = task.dueDateMillis,
            priorityName = task.priority.name,
            isCompleted = task.isCompleted,
            isReminderEnabled = task.isReminderEnabled,
            category = task.category,
            createdAt = task.createdAt
        )
    }
}

@Entity(tableName = "automations")
data class AutomationWorkflowEntity(
    @PrimaryKey val id: String,
    val title: String,
    val naturalLanguagePrompt: String,
    val scheduleDescription: String,
    val actionTypeName: String,
    val actionPayload: String,
    val statusName: String,
    val isEnabled: Boolean,
    val lastRunMillis: Long?,
    val nextRunMillis: Long?,
    val createdAt: Long
) {
    fun toDomain() = AutomationWorkflow(
        id = id,
        title = title,
        naturalLanguagePrompt = naturalLanguagePrompt,
        scheduleDescription = scheduleDescription,
        actionType = try { WorkflowActionType.valueOf(actionTypeName) } catch (e: Exception) { WorkflowActionType.CUSTOM_NOTIFICATION },
        actionPayload = actionPayload,
        status = try { WorkflowStatus.valueOf(statusName) } catch (e: Exception) { WorkflowStatus.SCHEDULED },
        isEnabled = isEnabled,
        lastRunMillis = lastRunMillis,
        nextRunMillis = nextRunMillis,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(workflow: AutomationWorkflow) = AutomationWorkflowEntity(
            id = workflow.id,
            title = workflow.title,
            naturalLanguagePrompt = workflow.naturalLanguagePrompt,
            scheduleDescription = workflow.scheduleDescription,
            actionTypeName = workflow.actionType.name,
            actionPayload = workflow.actionPayload,
            statusName = workflow.status.name,
            isEnabled = workflow.isEnabled,
            lastRunMillis = workflow.lastRunMillis,
            nextRunMillis = workflow.nextRunMillis,
            createdAt = workflow.createdAt
        )
    }
}
