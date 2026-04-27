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
    val reminderMinutesBefore: Int? = null, // null = no reminder
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#6750A4"
)
