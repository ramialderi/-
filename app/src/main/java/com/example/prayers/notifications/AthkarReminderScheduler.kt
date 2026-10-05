package com.example.prayers.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import java.util.Calendar

object AthkarReminderScheduler {

    private const val PREFS_NAME = "athkar_reminders_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getReminders(context: Context): List<AthkarReminder> {
        val prefs = getPrefs(context)
        return DefaultReminders.DEFAULT_LIST.map { defaultItem ->
            val enabled = prefs.getBoolean("reminder_enabled_${defaultItem.id}", defaultItem.isEnabled)
            val hour = prefs.getInt("reminder_hour_${defaultItem.id}", defaultItem.hour)
            val minute = prefs.getInt("reminder_minute_${defaultItem.id}", defaultItem.minute)
            defaultItem.copy(isEnabled = enabled, hour = hour, minute = minute)
        }
    }

    fun updateReminder(
        context: Context,
        reminderId: String,
        enabled: Boolean,
        hour: Int,
        minute: Int
    ) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putBoolean("reminder_enabled_$reminderId", enabled)
            .putInt("reminder_hour_$reminderId", hour)
            .putInt("reminder_minute_$reminderId", minute)
            .apply()

        val item = getReminders(context).firstOrNull { it.id == reminderId } ?: return
        if (enabled) {
            scheduleDailyAlarm(context, item.id, item.title, item.subtitle, hour, minute)
        } else {
            cancelAlarm(context, item.id)
        }
    }

    fun scheduleDailyAlarm(
        context: Context,
        id: String,
        title: String,
        subtitle: String,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AthkarNotificationReceiver::class.java).apply {
            putExtra(AthkarNotificationReceiver.EXTRA_REMINDER_ID, id)
            putExtra(AthkarNotificationReceiver.EXTRA_TITLE, title)
            putExtra(AthkarNotificationReceiver.EXTRA_SUBTITLE, subtitle)
            putExtra(AthkarNotificationReceiver.EXTRA_HOUR, hour)
            putExtra(AthkarNotificationReceiver.EXTRA_MINUTE, minute)
        }

        val requestCode = id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to inexact if exact alarm permission restricted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AthkarNotificationReceiver::class.java)
        val requestCode = id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAll(context: Context) {
        val list = getReminders(context)
        for (item in list) {
            if (item.isEnabled) {
                scheduleDailyAlarm(context, item.id, item.title, item.subtitle, item.hour, item.minute)
            }
        }
    }

    fun sendTestNotification(context: Context, title: String = "أذكار الصباح", subtitle: String = "أصبحنا وأصبح الملك لله والحمد لله") {
        AthkarNotificationReceiver.showNotification(
            context = context,
            notificationId = 9999,
            title = title,
            content = subtitle
        )
    }
}
