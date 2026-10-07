package com.example.prayers.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.prayers.audio.PrayerNotificationHelper
import com.example.prayers.model.Prayer
import com.example.widget.PrayerAppWidget

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: ACTION_PRAYER_ALERT

        if (action == ACTION_PRAYER_ALERT) {
            val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: Prayer.DHUHR.name
            val cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: ""
            val prayer = try {
                Prayer.valueOf(prayerName)
            } catch (e: Exception) {
                Prayer.DHUHR
            }

            // 1. Show notification and play Azan
            PrayerNotificationHelper.showPrayerEnteredNotification(context, prayer, cityName)

            // 2. Immediately update widget so it enters the 30-minute entry window
            PrayerAppWidget.updateAllWidgets(context)

            // 3. Reschedule upcoming prayer alerts and widget transitions
            PrayerNotificationScheduler.scheduleUpcomingPrayers(context)
        } else if (action == ACTION_WIDGET_TRANSITION) {
            // Transition widget from the 30-min entry window to the countdown mode
            PrayerAppWidget.updateAllWidgets(context)

            // Reschedule upcoming prayer alerts and widget transitions
            PrayerNotificationScheduler.scheduleUpcomingPrayers(context)
        }
    }

    companion object {
        const val ACTION_PRAYER_ALERT = "com.example.prayers.ACTION_PRAYER_ALERT"
        const val ACTION_WIDGET_TRANSITION = "com.example.prayers.ACTION_WIDGET_TRANSITION"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_CITY_NAME = "extra_city_name"
    }
}
