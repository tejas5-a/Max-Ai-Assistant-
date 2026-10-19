package com.example.data.automation

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.model.WorkflowStatus

class AutomationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val workflowId = inputData.getString("WORKFLOW_ID") ?: return Result.failure()
        val db = AppDatabase.getDatabase(appContext)
        val automationDao = db.automationDao()

        val entity = automationDao.getAutomationById(workflowId) ?: return Result.failure()
        if (!entity.isEnabled) return Result.success()

        val notificationHelper = NotificationHelper(appContext)

        return try {
            // Update state to RUNNING
            val runningEntity = entity.copy(
                statusName = WorkflowStatus.RUNNING.name,
                lastRunMillis = System.currentTimeMillis()
            )
            automationDao.updateAutomation(runningEntity)

            // Trigger action notification
            val title = "⚡ MAX AI Automation: ${entity.title}"
            val message = "Scheduled action executed successfully: ${entity.naturalLanguagePrompt}"
            notificationHelper.showNotification(title, message)

            // Update state to COMPLETED
            val completedEntity = runningEntity.copy(
                statusName = WorkflowStatus.COMPLETED.name
            )
            automationDao.updateAutomation(completedEntity)

            Result.success()
        } catch (e: Exception) {
            val failedEntity = entity.copy(
                statusName = WorkflowStatus.FAILED.name
            )
            automationDao.updateAutomation(failedEntity)
            Result.failure()
        }
    }
}
