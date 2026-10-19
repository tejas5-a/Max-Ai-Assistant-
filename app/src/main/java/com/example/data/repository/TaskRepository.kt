package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun addTask(task: TaskItem) {
        taskDao.insertTask(TaskEntity.fromDomain(task))
    }

    suspend fun updateTask(task: TaskItem) {
        taskDao.updateTask(TaskEntity.fromDomain(task))
    }

    suspend fun deleteTask(task: TaskItem) {
        taskDao.deleteTask(TaskEntity.fromDomain(task))
    }

    suspend fun toggleTaskCompletion(task: TaskItem) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        taskDao.updateTask(TaskEntity.fromDomain(updated))
    }

    suspend fun clearAllTasks() {
        taskDao.clearAllTasks()
    }
}
