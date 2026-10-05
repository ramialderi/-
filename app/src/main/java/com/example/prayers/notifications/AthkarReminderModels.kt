package com.example.prayers.notifications

data class AthkarReminder(
    val id: String,
    val title: String,
    val subtitle: String,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean
) {
    val formattedTime: String
        get() {
            var h = hour % 12
            if (h == 0) h = 12
            val amPm = if (hour < 12) "ص" else "م"
            return String.format("%02d:%02d %s", h, minute, amPm)
        }
}

object DefaultReminders {
    val DEFAULT_LIST = listOf(
        AthkarReminder(
            id = "morning",
            title = "أذكار الصباح",
            subtitle = "ألا بذكر الله تطمئن القلوب • من بعد الفجر إلى شروق الشمس",
            hour = 6,
            minute = 30,
            isEnabled = true
        ),
        AthkarReminder(
            id = "evening",
            title = "أذكار المساء",
            subtitle = "حصن نفسك وأهلك • من بعد العصر إلى غروب الشمس",
            hour = 17,
            minute = 0,
            isEnabled = true
        ),
        AthkarReminder(
            id = "sleep",
            title = "أذكار النوم",
            subtitle = "باسمك ربي وضعت جنبي وبك أرفعه • قبل النوم",
            hour = 22,
            minute = 30,
            isEnabled = true
        ),
        AthkarReminder(
            id = "wake_up",
            title = "أذكار الاستيقاظ",
            subtitle = "الحمد لله الذي أحيانا بعد ما أماتنا وإليه النشور",
            hour = 5,
            minute = 30,
            isEnabled = false
        )
    )
}
