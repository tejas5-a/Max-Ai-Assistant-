package com.example.ui.screens.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.automation.WorkflowParser
import com.example.data.model.AutomationWorkflow
import com.example.data.model.WorkflowStatus
import com.example.data.repository.AutomationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AutomationUiState(
    val automations: List<AutomationWorkflow> = emptyList(),
    val proposalWorkflow: AutomationWorkflow? = null
)

class AutomationViewModel(
    private val automationRepository: AutomationRepository
) : ViewModel() {

    private val _proposalWorkflow = MutableStateFlow<AutomationWorkflow?>(null)

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
