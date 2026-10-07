package com.example.prayers.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.widget.PrayerAppWidget

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            AthkarReminderScheduler.rescheduleAll(context)
            PrayerNotificationScheduler.scheduleUpcomingPrayers(context)
            PrayerAppWidget.updateAllWidgets(context)
        }
    }
}
