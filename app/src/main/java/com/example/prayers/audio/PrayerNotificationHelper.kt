package com.example.prayers.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.prayers.data.UserPreferencesRepository
import com.example.prayers.model.Prayer

object PrayerNotificationHelper {

    const val CHANNEL_ID = "prayer_alerts_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات مواقيت الصلاة والأذان"
            val descriptionText = "تنبيهات عند دخول وقت كل صلاة مع صوت الأذان"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                lightColor = 0xFF108A56.toInt()
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPrayerEnteredNotification(context: Context, prayer: Prayer, cityName: String) {
        createNotificationChannel(context)

        val prefs = UserPreferencesRepository(context)
        if (!prefs.areNotificationsEnabled()) return
        if (!prefs.isPrayerNotificationEnabled(prayer)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("selected_tab", "prayers")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            prayer.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "حان الآن موعد صلاة ${prayer.arabicName}"
        val content = "الله أكبر، حان وقت صلاة ${prayer.arabicName} في $cityName • حي على الصلاة"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_prayer_lantern)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(0xFF108A56.toInt())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationId = 2000 + prayer.ordinal
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())

        // Play Azan Audio if enabled
        if (prefs.isAzanSoundEnabled()) {
            AzanAudioPlayer.playPrayerAlert(context)
        }
    }
}
