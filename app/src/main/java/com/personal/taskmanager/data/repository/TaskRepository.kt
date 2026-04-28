package com.personal.taskmanager.data.repository

import com.personal.taskmanager.data.db.AppointmentDao
import com.personal.taskmanager.data.db.CategoryDao
import com.personal.taskmanager.data.db.TaskDao
import com.personal.taskmanager.data.model.Appointment
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
    private val appointmentDao: AppointmentDao,
    private val categoryDao: CategoryDao
) {
    // Tasks
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    fun getTasksForDate(date: LocalDate): Flow<List<Task>> = taskDao.getTasksForDate(date.toString())
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> = taskDao.getTasksByStatus(status)
    suspend fun addTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    suspend fun markComplete(id: Long) = taskDao.markComplete(id)
    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)
    suspend fun getTasksWithReminders(): List<Task> = taskDao.getTasksWithReminders()
    suspend fun updateOverdueTasks() = taskDao.markOverdueTasks(LocalDate.now().toString())

    // Appointments
    fun getAllAppointments(): Flow<List<Appointment>> = appointmentDao.getAllAppointments()
    fun getAppointmentsForDate(date: LocalDate): Flow<List<Appointment>> = appointmentDao.getAppointmentsForDate(date.toString())
    suspend fun addAppointment(appointment: Appointment): Long = appointmentDao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: Appointment) = appointmentDao.updateAppointment(appointment)
    suspend fun deleteAppointment(appointment: Appointment) = appointmentDao.deleteAppointment(appointment)
    suspend fun getAppointmentsWithReminders(): List<Appointment> = appointmentDao.getAppointmentsWithReminders()

    // Categories
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun addCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
}
