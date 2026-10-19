package com.example.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.automation.AutomationWorker
import com.example.data.local.AutomationDao
import com.example.data.local.AutomationWorkflowEntity
import com.example.data.model.AutomationWorkflow
import com.example.data.model.WorkflowStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class AutomationRepository(
    private val context: Context,
    private val automationDao: AutomationDao
) {
    private val workManager = WorkManager.getInstance(context)

    val allAutomations: Flow<List<AutomationWorkflow>> = automationDao.getAllAutomations().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveWorkflow(workflow: AutomationWorkflow) {
        val domainWorkflow = if (workflow.status == WorkflowStatus.AWAITING_REVIEW || workflow.status == WorkflowStatus.DRAFT) {
            workflow.copy(status = WorkflowStatus.SCHEDULED)
        } else workflow

        automationDao.insertAutomation(AutomationWorkflowEntity.fromDomain(domainWorkflow))
        if (domainWorkflow.isEnabled) {
            scheduleWorkManagerJob(domainWorkflow)
        }
    }

    suspend fun updateWorkflowStatus(workflow: AutomationWorkflow, newStatus: WorkflowStatus) {
        val updated = workflow.copy(status = newStatus)
        automationDao.updateAutomation(AutomationWorkflowEntity.fromDomain(updated))
    }

    suspend fun toggleWorkflowEnabled(workflow: AutomationWorkflow) {
        val updated = workflow.copy(
            isEnabled = !workflow.isEnabled,
            status = if (!workflow.isEnabled) WorkflowStatus.SCHEDULED else WorkflowStatus.DISABLED
        )
        automationDao.updateAutomation(AutomationWorkflowEntity.fromDomain(updated))

        if (updated.isEnabled) {
            scheduleWorkManagerJob(updated)
        } else {
            cancelWorkManagerJob(updated.id)
        }
    }

    suspend fun deleteWorkflow(workflow: AutomationWorkflow) {
        cancelWorkManagerJob(workflow.id)
        automationDao.deleteAutomation(AutomationWorkflowEntity.fromDomain(workflow))
    }

    private fun scheduleWorkManagerJob(workflow: AutomationWorkflow) {
        val inputData = Data.Builder()
            .putString("WORKFLOW_ID", workflow.id)
            .build()

        val intervalHours = when {
            workflow.scheduleDescription.contains("Hourly", ignoreCase = true) -> 1L
            else -> 24L
        }

        val workRequest = PeriodicWorkRequestBuilder<AutomationWorker>(intervalHours, TimeUnit.HOURS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "WORKFLOW_${workflow.id}",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelWorkManagerJob(workflowId: String) {
        workManager.cancelUniqueWork("WORKFLOW_$workflowId")
    }
}
