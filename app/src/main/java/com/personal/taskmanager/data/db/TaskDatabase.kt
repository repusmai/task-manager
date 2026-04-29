package com.personal.taskmanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personal.taskmanager.data.model.*

@Database(
    entities = [Task::class, Appointment::class, Category::class, Routine::class, RoutineStep::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun routineDao(): RoutineDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS appointments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
                startDate TEXT NOT NULL, startTime TEXT, endDate TEXT NOT NULL, endTime TEXT,
                location TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '#6750A4',
                reminderMinutesBefore INTEGER, createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN routineId INTEGER")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS routines (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
                colorHex TEXT NOT NULL DEFAULT '#00838F', scheduledTime TEXT,
                scheduledDays TEXT NOT NULL DEFAULT '', isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS routine_steps (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                routineId INTEGER NOT NULL, title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '', durationMinutes INTEGER,
                priority TEXT NOT NULL DEFAULT 'MEDIUM', orderIndex INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceDays TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceTime TEXT")
    }
}
