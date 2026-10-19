package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.AutomationWorkflow
import com.example.data.model.TaskItem
import com.example.data.repository.AutomationRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val pendingTasks: List<TaskItem> = emptyList(),
    val activeAutomations: List<AutomationWorkflow> = emptyList()
)

class HomeViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val taskRepository: TaskRepository,
    private val automationRepository: AutomationRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.settingsFlow,
        taskRepository.allTasks,
        automationRepository.allAutomations
    ) { settings, tasks, automations ->
        HomeUiState(
            settings = settings,
            pendingTasks = tasks.filter { !it.isCompleted }.take(3),
            activeAutomations = automations.filter { it.isEnabled }.take(3)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task)
        }
    }
}
