package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.prayers.audio.PrayerNotificationHelper
import com.example.prayers.notifications.AthkarNotificationReceiver
import com.example.prayers.notifications.AthkarReminderScheduler
import com.example.prayers.viewmodel.PrayerViewModel
import com.example.ui.AppTab
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prepare prayer notification channel & scheduled athkar reminders
        PrayerNotificationHelper.createNotificationChannel(this)
        AthkarNotificationReceiver.createNotificationChannel(this)
        AthkarReminderScheduler.rescheduleAll(this)

        val targetTab = if (intent.getStringExtra("selected_tab") == "athkar") {
            AppTab.ATHKAR
        } else {
            AppTab.PRAYERS
        }

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel, initialTab = targetTab)
                }
            }
        }
    }
}
