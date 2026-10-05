package com.example

import com.example.prayers.calculator.PrayerCalculator
import com.example.prayers.data.CitiesData
import com.example.prayers.data.HijriDateHelper
import com.example.prayers.model.CalculationMethod
import com.example.prayers.model.JuristicMethod
import com.example.prayers.model.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testPrayerCalculationForMakkah() {
        val cal = Calendar.getInstance()
        val makkah = CitiesData.DEFAULT_CITY
        val schedule = PrayerCalculator.calculateDaySchedule(
            calendar = cal,
            latitude = makkah.latitude,
            longitude = makkah.longitude,
            method = CalculationMethod.UMM_AL_QURA,
            juristic = JuristicMethod.STANDARD
        )

        assertTrue("Fajr must be earlier than Sunrise", schedule.fajrMillis < schedule.sunriseMillis)
        assertTrue("Sunrise must be earlier than Dhuhr", schedule.sunriseMillis < schedule.dhuhrMillis)
        assertTrue("Dhuhr must be earlier than Asr", schedule.dhuhrMillis < schedule.asrMillis)
        assertTrue("Asr must be earlier than Maghrib", schedule.asrMillis < schedule.maghribMillis)
        assertTrue("Maghrib must be earlier than Isha", schedule.maghribMillis < schedule.ishaMillis)
    }

    @Test
    fun testPrayerEntryWindow_First15Minutes() {
        val cal = Calendar.getInstance()
        val makkah = CitiesData.DEFAULT_CITY
        val schedule = PrayerCalculator.calculateDaySchedule(
            calendar = cal,
            latitude = makkah.latitude,
            longitude = makkah.longitude
        )

        // Simulate exactly 5 minutes after Dhuhr starts
        val testTime = schedule.dhuhrMillis + (5 * 60 * 1000L)
        val status = PrayerCalculator.getRealtimeStatus(testTime, schedule)

        assertEquals("Current prayer should be Dhuhr", Prayer.DHUHR, status.currentPrayer)
        assertTrue("Should be within 15-minute entry window", status.isWithinEntryWindow)
        assertTrue("Entry progress should be between 0 and 1", status.entryProgress in 0f..1f)
    }

    @Test
    fun testPrayerCountdown_After15Minutes() {
        val cal = Calendar.getInstance()
        val makkah = CitiesData.DEFAULT_CITY
        val schedule = PrayerCalculator.calculateDaySchedule(
            calendar = cal,
            latitude = makkah.latitude,
            longitude = makkah.longitude
        )

        // Simulate 30 minutes after Dhuhr starts (past the 15-minute entry window)
        val testTime = schedule.dhuhrMillis + (30 * 60 * 1000L)
        val status = PrayerCalculator.getRealtimeStatus(testTime, schedule)

        assertEquals("Current prayer is still Dhuhr", Prayer.DHUHR, status.currentPrayer)
        assertEquals("Next prayer should be Asr", Prayer.ASR, status.nextPrayer)
        assertFalse("Must NOT be within 15-minute entry window anymore", status.isWithinEntryWindow)
        assertTrue("Remaining time to next prayer must be positive", status.remainingToNextMillis > 0)
        assertTrue("Next prayer progress should be between 0 and 1", status.nextPrayerProgress in 0f..1f)
    }

    @Test
    fun testQiblaBearingCalculation() {
        // Cairo, Egypt -> Qibla should be approximately 136° Southeast
        val cairoBearing = PrayerCalculator.calculateQiblaBearing(30.0444, 31.2357)
        assertTrue("Cairo Qibla should be around 130-140 degrees", cairoBearing in 130f..140f)

        // Al-Hasakah, Syria -> Qibla should be around ~180-186 degrees (South)
        val hasakahBearing = PrayerCalculator.calculateQiblaBearing(36.5024, 40.7481)
        assertTrue("Al-Hasakah Qibla should be approximately South (180-186 deg)", hasakahBearing in 180f..186f)

        // Distance calculation
        val distance = PrayerCalculator.calculateDistanceToKaabaKm(30.0444, 31.2357)
        assertTrue("Distance Cairo to Makkah should be approx 1200-1400 km", distance in 1200..1400)
    }

    @Test
    fun testHijriDateHelper() {
        val cal = Calendar.getInstance()
        val hijri = HijriDateHelper.getHijriDate(cal, adjustmentDays = 0)
        assertNotNull(hijri.formatted)
        assertTrue("Hijri year should be around 1447 or 1448", hijri.year in 1445..1450)
        assertTrue("Hijri day in 1..30", hijri.day in 1..30)
    }

    @Test
    fun testAthkarDataAndReminders() {
        assertTrue("Athkar list should not be empty", com.example.prayers.data.AthkarData.ATHKAR_LIST.isNotEmpty())
        assertTrue("Categories should have at least 4 categories", com.example.prayers.data.AthkarData.CATEGORIES.size >= 4)

        val morningReminders = com.example.prayers.notifications.DefaultReminders.DEFAULT_LIST
        assertTrue("Default reminders list should contain morning & evening", morningReminders.any { it.id == "morning" })
        assertTrue("Default reminders list should contain evening", morningReminders.any { it.id == "evening" })
    }

    @Test
    fun testUpcomingIslamicOccasions() {
        val cal = Calendar.getInstance()
        val hijri = HijriDateHelper.getHijriDate(cal, adjustmentDays = 0)
        val occasions = HijriDateHelper.getUpcomingOccasions(hijri, limit = 8)

        assertTrue("Occasions should not be empty", occasions.isNotEmpty())
        assertTrue("Occasions should contain Ramadan or Eid", occasions.any { it.title.contains("رمضان") || it.title.contains("عيد") })
        occasions.forEach {
            assertTrue("Days remaining should be non-negative", it.daysRemaining >= 0)
            assertNotNull(it.hijriFormattedDate)
        }

        assertTrue("Day 14 should be a White Day", HijriDateHelper.isWhiteDay(14))
        assertFalse("Day 1 should not be a White Day", HijriDateHelper.isWhiteDay(1))
    }
}
