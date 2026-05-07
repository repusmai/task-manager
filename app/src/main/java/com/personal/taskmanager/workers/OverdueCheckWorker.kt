package com.personal.taskmanager.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.personal.taskmanager.data.repository.TaskRepository
import com.personal.taskmanager.notifications.scheduleReminder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class OverdueCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        repository.updateOverdueTasks()
        repository.archivePastAppointments()
        val tasks = repository.getTasksWithReminders()
        tasks.forEach { scheduleReminder(applicationContext, it) }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<OverdueCheckWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "overdue_check", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
