package com.example.ui.screens.productivity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class TaskFilter {
    ALL, PENDING, COMPLETED
}

data class ProductivityUiState(
    val tasks: List<TaskItem> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val isAddTaskDialogOpen: Boolean = false
)

class ProductivityViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _isAddTaskDialogOpen = MutableStateFlow(false)

    val isAddTaskDialogOpen: StateFlow<Boolean> = _isAddTaskDialogOpen.asStateFlow()

    val uiState: StateFlow<ProductivityUiState> = combine(
        taskRepository.allTasks,
        _filter,
        _isAddTaskDialogOpen
    ) { allTasks, filter, dialogOpen ->
        val filtered = when (filter) {
            TaskFilter.ALL -> allTasks
            TaskFilter.PENDING -> allTasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }
        ProductivityUiState(
            tasks = filtered,
            filter = filter,
            isAddTaskDialogOpen = dialogOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductivityUiState()
    )

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun openAddTaskDialog() {
        _isAddTaskDialogOpen.value = true
    }

    fun closeAddTaskDialog() {
        _isAddTaskDialogOpen.value = false
    }

    fun addTask(
        title: String,
        description: String,
        priority: TaskPriority,
        category: String
    ) {
        if (title.isBlank()) return
        val task = TaskItem(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            priority = priority,
            category = if (category.isNotBlank()) category else "General"
        )
        viewModelScope.launch {
            taskRepository.addTask(task)
            closeAddTaskDialog()
        }
    }

    fun toggleTask(task: TaskItem) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task)
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }
}
