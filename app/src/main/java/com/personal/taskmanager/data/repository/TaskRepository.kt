package com.personal.taskmanager.data.repository

import com.personal.taskmanager.data.db.CategoryDao
import com.personal.taskmanager.data.db.TaskDao
import com.personal.taskmanager.data.model.Category
import com.personal.taskmanager.data.model.Task
import com.personal.taskmanager.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao
) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: LocalDate): Flow<List<Task>> =
        taskDao.getTasksForDate(date.toString())

    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> =
        taskDao.getTasksByStatus(status)

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun addTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun markComplete(id: Long) = taskDao.markComplete(id)

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun getTasksWithReminders(): List<Task> = taskDao.getTasksWithReminders()

    suspend fun addCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun updateOverdueTasks() = taskDao.markOverdueTasks(LocalDate.now().toString())
}
