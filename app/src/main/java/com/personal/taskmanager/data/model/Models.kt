package com.personal.taskmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

enum class Priority { LOW, MEDIUM, HIGH }

// NONE=no recurrence, DAILY, WEEKLY, MONTHLY, CUSTOM=specific days of week, SPECIFIC_DATES=hand-picked dates
enum class RecurrenceType { NONE, DAILY, WEEKLY, MONTHLY, CUSTOM_DAYS, SPECIFIC_DATES }

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
    // For CUSTOM_DAYS: comma-separated 1-7 (Mon-Sun), e.g. "1,3,5"
    // For SPECIFIC_DATES: comma-separated ISO dates, e.g. "2026-05-01,2026-05-15"
    val recurrenceDays: String = "",
    val recurrenceTime: LocalTime? = null,
    val categoryId: Long? = null,
    val reminderMinutesBefore: Int? = null,
    val routineId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isArchived: Boolean = false
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
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6750A4"
)

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#00838F",
    val scheduledTime: LocalTime? = null,       // shared time for all days
    val scheduledDays: String = "",             // comma-separated day ints e.g. "1,3,5"
    val perDayTimes: String = "",              // per-day overrides: "1:09:00,3:14:30" (dayNum:HH:mm:ss)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_steps")
data class RoutineStep(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val title: String,
    val description: String = "",
    val durationMinutes: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val orderIndex: Int = 0
)
