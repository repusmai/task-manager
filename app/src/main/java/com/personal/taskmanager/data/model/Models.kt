package com.personal.taskmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class Priority { LOW, MEDIUM, HIGH }
enum class RecurrenceType { NONE, DAILY, WEEKLY, MONTHLY }
enum class TaskStatus { PENDING, COMPLETED, OVERDUE }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val categoryId: Long? = null,
    val reminderMinutesBefore: Int? = null,
    val routineId: Long? = null, // set when spawned from a routine
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startDate: LocalDate,
    val startTime: LocalTime? = null,
    val endDate: LocalDate,
    val endTime: LocalTime? = null,
    val location: String = "",
    val colorHex: String = "#6750A4",
    val reminderMinutesBefore: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6750A4"
)

// A routine template — defines a set of tasks to spawn
@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#00838F",
    // Scheduling: null = manual only
    val scheduledTime: LocalTime? = null,       // time of day to auto-start
    val scheduledDays: String = "",             // comma-separated ints: "1,3,5" = Mon,Wed,Fri (1=Mon..7=Sun)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// A single step/task template within a routine
@Entity(tableName = "routine_steps")
data class RoutineStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val title: String,
    val description: String = "",
    val durationMinutes: Int? = null, // estimated time
    val priority: Priority = Priority.MEDIUM,
    val orderIndex: Int = 0
)
