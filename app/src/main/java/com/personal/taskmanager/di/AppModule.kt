package com.personal.taskmanager.di

import android.content.Context
import androidx.room.Room
import com.personal.taskmanager.data.db.CategoryDao
import com.personal.taskmanager.data.db.TaskDao
import com.personal.taskmanager.data.db.TaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TaskDatabase =
        Room.databaseBuilder(context, TaskDatabase::class.java, "taskmanager.db")
            .build()

    @Provides fun provideTaskDao(db: TaskDatabase): TaskDao = db.taskDao()

    @Provides fun provideCategoryDao(db: TaskDatabase): CategoryDao = db.categoryDao()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}
