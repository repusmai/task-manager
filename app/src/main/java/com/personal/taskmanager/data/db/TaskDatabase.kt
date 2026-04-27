package com.personal.taskmanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.personal.taskmanager.data.model.Category
import com.personal.taskmanager.data.model.Task

@Database(
    entities = [Task::class, Category::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
}
