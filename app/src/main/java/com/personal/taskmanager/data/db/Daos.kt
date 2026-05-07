package com.personal.taskmanager.data.db

import androidx.room.*
import com.personal.taskmanager.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY dueDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isArchived = 1 ORDER BY completedAt DESC")
    fun getArchivedTasks(): Flow<List<Task>>

    @Query("UPDATE tasks SET isArchived = 1 WHERE id = :id")
    suspend fun archiveTask(id: Long)

    @Query("UPDATE tasks SET isArchived = 0, status = 'PENDING', completedAt = NULL WHERE id = :id")
    suspend fun restoreTask(id: Long)

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isArchived = 0 ORDER BY dueTime ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = :status AND isArchived = 0 ORDER BY dueDate ASC")
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

    @Query("UPDATE tasks SET status = 'COMPLETED', completedAt = :timestamp, isArchived = 1 WHERE id = :id")
    suspend fun markComplete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = 'OVERDUE' WHERE dueDate < :today AND status = 'PENDING'")
    suspend fun markOverdueTasks(today: String)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments WHERE isArchived = 0 ORDER BY startDate ASC, startTime ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE isArchived = 1 ORDER BY startDate DESC")
    fun getArchivedAppointments(): Flow<List<Appointment>>

    @Query("UPDATE appointments SET isArchived = 1 WHERE endDate < :today AND isArchived = 0")
    suspend fun archivePastAppointments(today: String)

    @Query("UPDATE appointments SET isArchived = 0 WHERE id = :id")
    suspend fun restoreAppointment(id: Long)

    @Query("SELECT * FROM appointments WHERE startDate <= :date AND endDate >= :date AND isArchived = 0 ORDER BY startTime ASC")
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

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY name ASC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): Routine?

    @Query("SELECT * FROM routines WHERE isActive = 1 AND scheduledDays != ''")
    suspend fun getScheduledRoutines(): List<Routine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    // Steps
    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getStepsForRoutine(routineId: Long): Flow<List<RoutineStep>>

    @Query("SELECT * FROM routine_steps WHERE routineId = :routineId ORDER BY orderIndex ASC")
    suspend fun getStepsForRoutineSuspend(routineId: Long): List<RoutineStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: RoutineStep): Long

    @Update
    suspend fun updateStep(step: RoutineStep)

    @Delete
    suspend fun deleteStep(step: RoutineStep)

    @Query("DELETE FROM routine_steps WHERE routineId = :routineId")
    suspend fun deleteStepsForRoutine(routineId: Long)
}
