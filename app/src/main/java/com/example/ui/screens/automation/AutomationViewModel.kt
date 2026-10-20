package com.example.ui.screens.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.automation.WorkflowParser
import com.example.data.model.AutomationWorkflow
import com.example.data.repository.AutomationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExecutionLogItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val status: String, // "success" or "failed"
    val resultSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AutomationUiState(
    val automations: List<AutomationWorkflow> = emptyList(),
    val proposalWorkflow: AutomationWorkflow? = null
)

class AutomationViewModel(
    private val automationRepository: AutomationRepository
) : ViewModel() {

    private val _proposalWorkflow = MutableStateFlow<AutomationWorkflow?>(null)

    val executionLogs = MutableStateFlow<List<ExecutionLogItem>>(
        listOf(
            ExecutionLogItem(title = "Morning Briefing", status = "success", resultSummary = "Weather & tasks fetched at 7:00 AM"),
            ExecutionLogItem(title = "Task Reminder Sync", status = "success", resultSummary = "Synced 3 pending high priority tasks"),
            ExecutionLogItem(title = "Evening Routine", status = "success", resultSummary = "Muted media volume and logged summary"),
            ExecutionLogItem(title = "Web News Search", status = "failed", resultSummary = "Network timeout during query")
        )
    )

    val uiState: StateFlow<AutomationUiState> = combine(
        automationRepository.allAutomations,
        _proposalWorkflow
    ) { automations, proposal ->
        AutomationUiState(
            automations = automations,
            proposalWorkflow = proposal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AutomationUiState()
    )

    fun saveWorkflow(workflow: AutomationWorkflow) {
        viewModelScope.launch {
            automationRepository.saveWorkflow(workflow)
        }
    }

    fun parseNaturalLanguagePrompt(prompt: String) {
        if (prompt.isBlank()) return
        val proposal = WorkflowParser.parsePrompt(prompt)
        _proposalWorkflow.value = proposal
    }

    fun approveProposal() {
        val proposal = _proposalWorkflow.value ?: return
        viewModelScope.launch {
            automationRepository.saveWorkflow(proposal)
            _proposalWorkflow.value = null
        }
    }

    fun cancelProposal() {
        _proposalWorkflow.value = null
    }

    fun toggleWorkflow(workflow: AutomationWorkflow) {
        viewModelScope.launch {
            automationRepository.toggleWorkflowEnabled(workflow)
        }
    }

    fun deleteWorkflow(workflow: AutomationWorkflow) {
        viewModelScope.launch {
            automationRepository.deleteWorkflow(workflow)
        }
    }
}
