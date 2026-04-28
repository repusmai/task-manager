package com.personal.taskmanager.data.repository

import com.personal.taskmanager.data.db.*
import com.personal.taskmanager.data.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val appointmentDao: AppointmentDao,
    private val categoryDao: CategoryDao,
    private val routineDao: RoutineDao
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

    // Routines
    fun getAllRoutines(): Flow<List<Routine>> = routineDao.getAllRoutines()
    suspend fun getRoutineById(id: Long): Routine? = routineDao.getRoutineById(id)
    suspend fun getScheduledRoutines(): List<Routine> = routineDao.getScheduledRoutines()
    suspend fun addRoutine(routine: Routine): Long = routineDao.insertRoutine(routine)
    suspend fun updateRoutine(routine: Routine) = routineDao.updateRoutine(routine)
    suspend fun deleteRoutine(routine: Routine) {
        routineDao.deleteStepsForRoutine(routine.id)
        routineDao.deleteRoutine(routine)
    }

    // Routine Steps
    fun getStepsForRoutine(routineId: Long): Flow<List<RoutineStep>> = routineDao.getStepsForRoutine(routineId)
    suspend fun getStepsForRoutineSuspend(routineId: Long): List<RoutineStep> = routineDao.getStepsForRoutineSuspend(routineId)
    suspend fun addStep(step: RoutineStep): Long = routineDao.insertStep(step)
    suspend fun updateStep(step: RoutineStep) = routineDao.updateStep(step)
    suspend fun deleteStep(step: RoutineStep) = routineDao.deleteStep(step)
    suspend fun replaceSteps(routineId: Long, steps: List<RoutineStep>) {
        routineDao.deleteStepsForRoutine(routineId)
        steps.forEachIndexed { index, step ->
            routineDao.insertStep(step.copy(routineId = routineId, orderIndex = index))
        }
    }
}
