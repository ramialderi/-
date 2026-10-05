package com.example.prayers.model

import java.util.TimeZone

enum class Prayer(
    val arabicName: String,
    val englishName: String,
    val isObligatory: Boolean
) {
    FAJR("الفجر", "Fajr", true),
    SUNRISE("الشروق", "Sunrise", false),
    DHUHR("الظهر", "Dhuhr", true),
    ASR("العصر", "Asr", true),
    MAGHRIB("المغرب", "Maghrib", true),
    ISHA("العشاء", "Isha", true);

    companion object {
        val obligatoryPrayers = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
        val allSchedulePrayers = listOf(FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA)
    }
}

enum class CalculationMethod(
    val titleArabic: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaFixedMinutes: Int = 0
) {
    UMM_AL_QURA("أم القرى - مكة المكرمة", 18.5, 0.0, 90),
    MUSLIM_WORLD_LEAGUE("رابطة العالم الإسلامي", 18.0, 17.0),
    EGYPTIAN("الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    ISNA("أمريكا الشمالية (ISNA)", 15.0, 15.0),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0),
    DUBAI("دبي والإمارات", 18.2, 18.2)
}

enum class JuristicMethod(val titleArabic: String, val shadowFactor: Int) {
    STANDARD("الجمهور (شافعي، مالكي، حنبلي)", 1),
    HANAFI("المذهب الحنفي", 2)
}

data class LocationInfo(
    val cityName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String = TimeZone.getDefault().id,
    val cityNameEn: String = ""
) {
    fun getDisplayCity(isEnglish: Boolean = false): String {
        return if (isEnglish && cityNameEn.isNotBlank()) cityNameEn else cityName
    }
}

data class PrayerTimeItem(
    val prayer: Prayer,
    val timeMillis: Long,
    val timeFormatted: String,
    val isPassed: Boolean = false,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false
)

data class PrayerDaySchedule(
    val fajrMillis: Long,
    val sunriseMillis: Long,
    val dhuhrMillis: Long,
    val asrMillis: Long,
    val maghribMillis: Long,
    val ishaMillis: Long,
    val midnightMillis: Long,
    val qiyamMillis: Long,
    val imsakMillis: Long
)

/**
 * Encapsulates the core behavior requested by the user:
 * 1. Line/indicator showing prayer entry (within first 15 mins).
 * 2. After 15 minutes, switches to countdown and remaining time until next prayer.
 */
data class PrayerRealtimeStatus(
    val currentPrayer: Prayer,
    val nextPrayer: Prayer,
    val currentPrayerTimeMillis: Long,
    val nextPrayerTimeMillis: Long,
    val isWithinEntryWindow: Boolean, // true if elapsed < 15 minutes
    val elapsedSinceEntryMillis: Long,
    val remainingToNextMillis: Long,
    val entryWindowTotalMillis: Long = 15 * 60 * 1000L,
    val entryProgress: Float, // 0.0 to 1.0 (elapsed / 15m)
    val nextPrayerProgress: Float, // 0.0 to 1.0 (from 15m mark to next prayer)
    val isSimulationMode: Boolean = false
)
