package com.example.prayers.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.prayers.audio.PrayerNotificationHelper
import com.example.prayers.calculator.PrayerCalculator
import com.example.prayers.data.UserPreferencesRepository
import com.example.prayers.model.Prayer
import java.util.Calendar

object PrayerNotificationScheduler {

    fun scheduleUpcomingPrayers(context: Context) {
        val prefs = UserPreferencesRepository(context)
        val location = prefs.getLocation()
        val method = prefs.getCalculationMethod()
        val juristic = prefs.getJuristicMethod()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        // Calculate today and tomorrow schedules
        val todayCal = Calendar.getInstance()
        val todaySchedule = PrayerCalculator.calculateDaySchedule(
            calendar = todayCal,
            latitude = location.latitude,
            longitude = location.longitude,
            method = method,
            juristic = juristic
        )

        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrowSchedule = PrayerCalculator.calculateDaySchedule(
            calendar = tomorrowCal,
            latitude = location.latitude,
            longitude = location.longitude,
            method = method,
            juristic = juristic
        )

        val prayersList = listOf(
            Prayer.FAJR to todaySchedule.fajrMillis,
            Prayer.DHUHR to todaySchedule.dhuhrMillis,
            Prayer.ASR to todaySchedule.asrMillis,
            Prayer.MAGHRIB to todaySchedule.maghribMillis,
            Prayer.ISHA to todaySchedule.ishaMillis,
            Prayer.FAJR to tomorrowSchedule.fajrMillis,
            Prayer.DHUHR to tomorrowSchedule.dhuhrMillis,
            Prayer.ASR to tomorrowSchedule.asrMillis,
            Prayer.MAGHRIB to tomorrowSchedule.maghribMillis,
            Prayer.ISHA to tomorrowSchedule.ishaMillis
        )

        prayersList.forEachIndexed { index, (prayer, timeMillis) ->
            // 1. Exact Prayer Alert at the time of each prayer
            if (timeMillis > now) {
                val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_PRAYER_ALERT
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
                    putExtra(PrayerAlarmReceiver.EXTRA_CITY_NAME, location.cityName)
                }

                val requestCode = 1000 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleExact(alarmManager, timeMillis, pendingIntent)
            }

            // 2. Exact Widget Transition after 30 minutes (to switch from entry window to countdown)
            val transitionTimeMillis = timeMillis + (30 * 60 * 1000L)
            if (transitionTimeMillis > now) {
                val transitionIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_WIDGET_TRANSITION
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
                }

                val transitionRequestCode = 2000 + index
                val transitionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    transitionRequestCode,
                    transitionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleExact(alarmManager, transitionTimeMillis, transitionPendingIntent)
            }
        }
    }

    private fun scheduleExact(
        alarmManager: AlarmManager,
        timeMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                pendingIntent
            )
        }
    }

    fun sendTestPrayerAlert(context: Context, prayer: Prayer = Prayer.DHUHR) {
        val prefs = UserPreferencesRepository(context)
        val city = prefs.getLocation().cityName
        PrayerNotificationHelper.showPrayerEnteredNotification(context, prayer, city)
    }
}
