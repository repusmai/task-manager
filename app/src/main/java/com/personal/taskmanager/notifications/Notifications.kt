package com.personal.taskmanager.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.personal.taskmanager.MainActivity
import com.personal.taskmanager.R
import com.personal.taskmanager.data.model.Appointment
import com.personal.taskmanager.data.model.Task
import java.time.LocalDateTime
import java.time.ZoneId

const val CHANNEL_ID = "task_reminders"
const val EXTRA_TASK_ID = "task_id"
const val EXTRA_TASK_TITLE = "task_title"

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID, "Task Reminders", NotificationManager.IMPORTANCE_HIGH
    ).apply { description = "Reminders for upcoming tasks and appointments" }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

fun scheduleReminder(context: Context, task: Task) {
    val dueDate = task.dueDate ?: return
    val dueTime = task.dueTime ?: return
    val minutesBefore = task.reminderMinutesBefore ?: return
    val reminderTime = LocalDateTime.of(dueDate, dueTime)
        .minusMinutes(minutesBefore.toLong())
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    if (reminderTime <= System.currentTimeMillis()) return
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(EXTRA_TASK_ID, task.id)
        putExtra(EXTRA_TASK_TITLE, task.title)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, task.id.toInt(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    context.getSystemService(AlarmManager::class.java)
        .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
}

fun scheduleAppointmentReminder(context: Context, appointment: Appointment) {
    val startDate = appointment.startDate
    val startTime = appointment.startTime ?: return
    val minutesBefore = appointment.reminderMinutesBefore ?: return
    val reminderTime = LocalDateTime.of(startDate, startTime)
        .minusMinutes(minutesBefore.toLong())
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    if (reminderTime <= System.currentTimeMillis()) return
    // Use offset ID to avoid clashing with task IDs
    val notifId = (appointment.id + 100000L).toInt()
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(EXTRA_TASK_ID, notifId.toLong())
        putExtra(EXTRA_TASK_TITLE, "Appointment: ${appointment.title}")
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, notifId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    context.getSystemService(AlarmManager::class.java)
        .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
}

fun cancelReminder(context: Context, taskId: Long) {
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, taskId.toInt(), intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) ?: return
    context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Reminder"
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPending = PendingIntent.getActivity(
            context, taskId.toInt(), tapIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(taskTitle)
            .setContentText("Don't forget!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(taskId.toInt(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {}
}
