package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.prayers.calculator.PrayerCalculator
import com.example.prayers.data.HijriDateHelper
import com.example.prayers.data.UserPreferencesRepository
import com.example.prayers.model.Prayer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PrayerAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = UserPreferencesRepository(context)
            val location = prefs.getLocation()
            val method = prefs.getCalculationMethod()
            val juristic = prefs.getJuristicMethod()
            val hijriAdj = prefs.getHijriAdjustment()

            val calendar = Calendar.getInstance()
            val schedule = PrayerCalculator.calculateDaySchedule(
                calendar = calendar,
                latitude = location.latitude,
                longitude = location.longitude,
                method = method,
                juristic = juristic
            )

            val now = System.currentTimeMillis()
            val status = PrayerCalculator.getRealtimeStatus(now, schedule)
            val hijriDate = HijriDateHelper.getHijriDate(calendar, hijriAdj)

            val views = RemoteViews(context.packageName, R.layout.widget_prayer)

            // Click pending intent to open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // 1. Left Prayer (Current/Preceding)
            val leftIconRes = getPrayerIcon(status.currentPrayer)
            views.setImageViewResource(R.id.widget_iv_left_icon, leftIconRes)
            views.setTextViewText(R.id.widget_tv_left_name, status.currentPrayer.englishName)
            views.setTextViewText(
                R.id.widget_tv_left_time,
                PrayerCalculator.formatTime12h(status.currentPrayerTimeMillis)
            )

            // 2. Center Column: City, Day, Gregorian & Hijri Date
            val cityName = location.getDisplayCity(isEnglish = true)
            views.setTextViewText(R.id.widget_tv_city, cityName)

            val dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(calendar.time)
            views.setTextViewText(R.id.widget_tv_day, dayOfWeek)

            val gregorianDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(calendar.time)
            views.setTextViewText(R.id.widget_tv_gregorian, gregorianDate)

            val hijriFormatted = if (hijriDate.formattedEn.isNotBlank()) hijriDate.formattedEn else hijriDate.formatted
            views.setTextViewText(R.id.widget_tv_hijri, hijriFormatted)

            // 3. Right Prayer (Next)
            val rightIconRes = getPrayerIcon(status.nextPrayer)
            views.setImageViewResource(R.id.widget_iv_right_icon, rightIconRes)
            views.setTextViewText(R.id.widget_tv_right_name, status.nextPrayer.englishName)
            views.setTextViewText(
                R.id.widget_tv_right_time,
                PrayerCalculator.formatTime12h(status.nextPrayerTimeMillis)
            )

            // 4. Progress Bar & Countdown (Dynamic matching screenshot & user request)
            if (status.isWithinEntryWindow) {
                // First 15 minutes: Prayer time just entered
                views.setTextViewText(
                    R.id.widget_tv_status_title,
                    "Now ${status.currentPrayer.englishName} Time"
                )
                views.setTextViewText(
                    R.id.widget_tv_countdown,
                    PrayerCalculator.formatDurationCountdown(status.elapsedSinceEntryMillis)
                )
                val progressPercent = (status.entryProgress * 100).toInt().coerceIn(5, 100)
                views.setProgressBar(R.id.widget_progress_timeline, 100, progressPercent, false)
            } else {
                // After 15 minutes: Countdown to next prayer
                views.setTextViewText(
                    R.id.widget_tv_status_title,
                    "Left until ${status.nextPrayer.englishName}"
                )
                views.setTextViewText(
                    R.id.widget_tv_countdown,
                    PrayerCalculator.formatDurationCountdown(status.remainingToNextMillis)
                )
                val progressPercent = (status.nextPrayerProgress * 100).toInt().coerceIn(2, 100)
                views.setProgressBar(R.id.widget_progress_timeline, 100, progressPercent, false)
            }

            // Subtitle below card
            views.setTextViewText(R.id.widget_tv_app_label, "Ela-Salaty")

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPrayerIcon(prayer: Prayer): Int {
            return when (prayer) {
                Prayer.FAJR -> R.drawable.ic_prayer_fajr
                Prayer.SUNRISE, Prayer.DHUHR, Prayer.ASR -> R.drawable.ic_prayer_sun
                Prayer.MAGHRIB, Prayer.ISHA -> R.drawable.ic_prayer_crescent
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PrayerAppWidget::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}
