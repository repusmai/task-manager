package com.personal.taskmanager.data.db

import androidx.room.*
import com.personal.taskmanager.data.model.Appointment
import com.personal.taskmanager.data.model.Category
import com.personal.taskmanager.data.model.Task
import com.personal.taskmanager.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dueDate = :date ORDER BY dueTime ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY dueDate ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE reminderMinutesBefore IS NOT NULL AND status = 'PENDING'")
    suspend fun getTasksWithReminders(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET status = 'COMPLETED', completedAt = :timestamp WHERE id = :id")
    suspend fun markComplete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = 'OVERDUE' WHERE dueDate < :today AND status = 'PENDING'")
    suspend fun markOverdueTasks(today: String)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY startDate ASC, startTime ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    // Get appointments that overlap with a given date
    @Query("SELECT * FROM appointments WHERE startDate <= :date AND endDate >= :date ORDER BY startTime ASC")
    fun getAppointmentsForDate(date: String): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getAppointmentById(id: Long): Appointment?

    @Query("SELECT * FROM appointments WHERE reminderMinutesBefore IS NOT NULL")
    suspend fun getAppointmentsWithReminders(): List<Appointment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)
}
