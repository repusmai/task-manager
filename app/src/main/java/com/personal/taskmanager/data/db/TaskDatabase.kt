package com.personal.taskmanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personal.taskmanager.data.model.Appointment
import com.personal.taskmanager.data.model.Category
import com.personal.taskmanager.data.model.Task

@Database(
    entities = [Task::class, Appointment::class, Category::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun categoryDao(): CategoryDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS appointments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                startDate TEXT NOT NULL,
                startTime TEXT,
                endDate TEXT NOT NULL,
                endTime TEXT,
                location TEXT NOT NULL DEFAULT '',
                colorHex TEXT NOT NULL DEFAULT '#6750A4',
                reminderMinutesBefore INTEGER,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
