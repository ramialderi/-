package com.example.prayers.calculator

import com.example.prayers.model.CalculationMethod
import com.example.prayers.model.JuristicMethod
import com.example.prayers.model.Prayer
import com.example.prayers.model.PrayerDaySchedule
import com.example.prayers.model.PrayerRealtimeStatus
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

object PrayerCalculator {

    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI

    private fun dsin(d: Double): Double = sin(d * DEG_TO_RAD)
    private fun dcos(d: Double): Double = cos(d * DEG_TO_RAD)
    private fun dtan(d: Double): Double = tan(d * DEG_TO_RAD)
    private fun dasin(x: Double): Double = asin(x.coerceIn(-1.0, 1.0)) * RAD_TO_DEG
    private fun dacos(x: Double): Double = acos(x.coerceIn(-1.0, 1.0)) * RAD_TO_DEG
    private fun datan2(y: Double, x: Double): Double = atan2(y, x) * RAD_TO_DEG

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(datan2(dcos(e) * dsin(l), dcos(l))) / 15.0
        val dcl = dasin(dsin(e) * dsin(l))
        val eqt = q / 15.0 - ra
        return Pair(dcl, eqt)
    }

    private fun sunAltitudeHourAngle(lat: Double, dcl: Double, altitude: Double): Double {
        val term = (dsin(altitude) - dsin(lat) * dsin(dcl)) / (dcos(lat) * dcos(dcl))
        if (term > 1.0 || term < -1.0) {
            return 0.0
        }
        return dacos(term) / 15.0
    }

    private fun asrHourAngle(lat: Double, dcl: Double, shadowFactor: Int): Double {
        val angle = Math.toDegrees(kotlin.math.atan(1.0 / (shadowFactor + dtan(abs(lat - dcl)))))
        val term = (dsin(angle) - dsin(lat) * dsin(dcl)) / (dcos(lat) * dcos(dcl))
        return if (term in -1.0..1.0) dacos(term) / 15.0 else 0.0
    }

    fun calculateDaySchedule(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod = CalculationMethod.UMM_AL_QURA,
        juristic: JuristicMethod = JuristicMethod.STANDARD
    ): PrayerDaySchedule {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day)
        val (declination, eqt) = sunPosition(jd)

        val tzOffsetHours = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0
        val noon = fixHour(12.0 + tzOffsetHours - (longitude / 15.0) - eqt)

        // Sunrise & Sunset angle: -0.8333 degrees
        val sunRiseSetHa = sunAltitudeHourAngle(latitude, declination, -0.8333)
        val sunriseHour = noon - sunRiseSetHa
        val sunsetHour = noon + sunRiseSetHa

        // Fajr angle
        val fajrHa = sunAltitudeHourAngle(latitude, declination, -method.fajrAngle)
        val fajrHour = noon - fajrHa

        // Asr angle
        val asrHa = asrHourAngle(latitude, declination, juristic.shadowFactor)
        val asrHour = noon + asrHa

        // Isha hour
        val ishaHour: Double = if (method.ishaFixedMinutes > 0) {
            sunsetHour + (method.ishaFixedMinutes / 60.0)
        } else {
            val ishaHa = sunAltitudeHourAngle(latitude, declination, -method.ishaAngle)
            noon + ishaHa
        }

        fun toCalendarMillis(hourFraction: Double): Long {
            val cal = calendar.clone() as Calendar
            val totalSeconds = (hourFraction * 3600).toInt()
            val h = totalSeconds / 3600
            val rem = totalSeconds % 3600
            val m = rem / 60
            val s = rem % 60
            cal.set(Calendar.HOUR_OF_DAY, (h % 24 + 24) % 24)
            cal.set(Calendar.MINUTE, m)
            cal.set(Calendar.SECOND, s)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val fajrMillis = toCalendarMillis(fajrHour)
        val sunriseMillis = toCalendarMillis(sunriseHour)
        val dhuhrMillis = toCalendarMillis(noon)
        val asrMillis = toCalendarMillis(asrHour)
        val maghribMillis = toCalendarMillis(sunsetHour)
        val ishaMillis = toCalendarMillis(ishaHour)

        // Midnight is halfway between Maghrib and next Fajr (approximated with today's Fajr + 24h)
        val nextFajr = fajrMillis + 24 * 3600 * 1000L
        val nightDuration = nextFajr - maghribMillis
        val midnightMillis = maghribMillis + (nightDuration / 2)
        val qiyamMillis = maghribMillis + ((nightDuration * 2) / 3) // Last third of night starts
        val imsakMillis = fajrMillis - (10 * 60 * 1000L) // 10 minutes before Fajr

        return PrayerDaySchedule(
            fajrMillis = fajrMillis,
            sunriseMillis = sunriseMillis,
            dhuhrMillis = dhuhrMillis,
            asrMillis = asrMillis,
            maghribMillis = maghribMillis,
            ishaMillis = ishaMillis,
            midnightMillis = midnightMillis,
            qiyamMillis = qiyamMillis,
            imsakMillis = imsakMillis
        )
    }

    /**
     * Computes the current prayer status, implementing the user's specific request:
     * - Within the first 15 minutes of prayer entry: isWithinEntryWindow = true, shows prayer entry line.
     * - After 15 minutes: isWithinEntryWindow = false, shows countdown and remaining time until next prayer.
     */
    fun getRealtimeStatus(
        currentTimeMillis: Long,
        schedule: PrayerDaySchedule,
        simulationMode: Boolean = false,
        simulateEntryWindow: Boolean = false
    ): PrayerRealtimeStatus {
        val entryWindowMillis = 15 * 60 * 1000L

        // Order of prayer checkpoints
        val prayerPairs = listOf(
            Prayer.FAJR to schedule.fajrMillis,
            Prayer.SUNRISE to schedule.sunriseMillis,
            Prayer.DHUHR to schedule.dhuhrMillis,
            Prayer.ASR to schedule.asrMillis,
            Prayer.MAGHRIB to schedule.maghribMillis,
            Prayer.ISHA to schedule.ishaMillis
        )

        // Determine current & next prayer
        var currentPrayer = Prayer.ISHA
        var currentPrayerTime = schedule.ishaMillis - (24 * 3600 * 1000L)
        var nextPrayer = Prayer.FAJR
        var nextPrayerTime = schedule.fajrMillis

        val now = currentTimeMillis

        if (now < schedule.fajrMillis) {
            currentPrayer = Prayer.ISHA
            currentPrayerTime = schedule.ishaMillis - (24 * 3600 * 1000L)
            nextPrayer = Prayer.FAJR
            nextPrayerTime = schedule.fajrMillis
        } else if (now < schedule.sunriseMillis) {
            currentPrayer = Prayer.FAJR
            currentPrayerTime = schedule.fajrMillis
            nextPrayer = Prayer.SUNRISE
            nextPrayerTime = schedule.sunriseMillis
        } else if (now < schedule.dhuhrMillis) {
            currentPrayer = Prayer.SUNRISE
            currentPrayerTime = schedule.sunriseMillis
            nextPrayer = Prayer.DHUHR
            nextPrayerTime = schedule.dhuhrMillis
        } else if (now < schedule.asrMillis) {
            currentPrayer = Prayer.DHUHR
            currentPrayerTime = schedule.dhuhrMillis
            nextPrayer = Prayer.ASR
            nextPrayerTime = schedule.asrMillis
        } else if (now < schedule.maghribMillis) {
            currentPrayer = Prayer.ASR
            currentPrayerTime = schedule.asrMillis
            nextPrayer = Prayer.MAGHRIB
            nextPrayerTime = schedule.maghribMillis
        } else if (now < schedule.ishaMillis) {
            currentPrayer = Prayer.MAGHRIB
            currentPrayerTime = schedule.maghribMillis
            nextPrayer = Prayer.ISHA
            nextPrayerTime = schedule.ishaMillis
        } else {
            currentPrayer = Prayer.ISHA
            currentPrayerTime = schedule.ishaMillis
            nextPrayer = Prayer.FAJR
            nextPrayerTime = schedule.fajrMillis + (24 * 3600 * 1000L)
        }

        var elapsedSinceEntry = now - currentPrayerTime
        var remainingToNext = nextPrayerTime - now

        // If simulation mode is requested (allows user to test both 15-min entry mode and countdown mode!)
        if (simulationMode) {
            if (simulateEntryWindow) {
                // Simulate: prayer entered 5 minutes ago (within 15 minutes window)
                elapsedSinceEntry = 5 * 60 * 1000L
                currentPrayerTime = now - elapsedSinceEntry
            } else {
                // Simulate: prayer entered 35 minutes ago (after 15 minutes window -> showing countdown)
                elapsedSinceEntry = 35 * 60 * 1000L
                currentPrayerTime = now - elapsedSinceEntry
            }
        }

        val isWithinWindow = elapsedSinceEntry in 0 until entryWindowMillis

        // 1. Entry window progress (from 0 to 15 min)
        val entryProgress = (elapsedSinceEntry.toFloat() / entryWindowMillis.toFloat()).coerceIn(0f, 1f)

        // 2. Next prayer countdown progress (from 15 min mark to next prayer)
        val countdownStartMillis = currentPrayerTime + entryWindowMillis
        val totalCountdownSpan = (nextPrayerTime - countdownStartMillis).coerceAtLeast(1L)
        val countdownElapsed = (now - countdownStartMillis).coerceAtLeast(0L)
        val nextPrayerProgress = (countdownElapsed.toFloat() / totalCountdownSpan.toFloat()).coerceIn(0f, 1f)

        return PrayerRealtimeStatus(
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer,
            currentPrayerTimeMillis = currentPrayerTime,
            nextPrayerTimeMillis = nextPrayerTime,
            isWithinEntryWindow = isWithinWindow,
            elapsedSinceEntryMillis = elapsedSinceEntry.coerceAtLeast(0L),
            remainingToNextMillis = remainingToNext.coerceAtLeast(0L),
            entryWindowTotalMillis = entryWindowMillis,
            entryProgress = entryProgress,
            nextPrayerProgress = nextPrayerProgress,
            isSimulationMode = simulationMode
        )
    }

    fun formatDuration(millis: Long): String {
        val totalSecs = (millis / 1000).coerceAtLeast(0)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d:%02d", hours, mins, secs)
    }

    fun formatTime(timeMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = timeMillis
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return String.format("%02d:%02d", h, m)
    }

    fun formatTime12h(timeMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = timeMillis
        var h = cal.get(Calendar.HOUR)
        if (h == 0) h = 12
        val m = cal.get(Calendar.MINUTE)
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        return String.format("%d:%02d %s", h, m, amPm)
    }

    fun formatDurationCountdown(millis: Long): String {
        val totalSecs = (millis / 1000).coerceAtLeast(0)
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return String.format("%d:%02d:%02d", hours, mins, secs)
    }

    /**
     * Calculates Qibla direction in degrees from true North for given latitude and longitude.
     * Holy Kaaba in Makkah: 21.4225° N, 39.8262° E
     */
    fun calculateQiblaBearing(lat: Double, lng: Double): Float {
        val kaabaLat = 21.4225241 * DEG_TO_RAD
        val kaabaLng = 39.8261818 * DEG_TO_RAD
        val userLat = lat * DEG_TO_RAD
        val deltaLng = kaabaLng - (lng * DEG_TO_RAD)

        val y = sin(deltaLng)
        val x = cos(userLat) * tan(kaabaLat) - sin(userLat) * cos(deltaLng)
        var qibla = atan2(y, x) * RAD_TO_DEG
        if (qibla < 0) qibla += 360.0
        return qibla.toFloat()
    }

    /**
     * Calculates approximate distance to Holy Kaaba in kilometers.
     */
    fun calculateDistanceToKaabaKm(lat: Double, lng: Double): Int {
        val r = 6371.0 // Earth radius in km
        val kaabaLat = 21.4225241 * DEG_TO_RAD
        val kaabaLng = 39.8261818 * DEG_TO_RAD
        val uLat = lat * DEG_TO_RAD
        val uLng = lng * DEG_TO_RAD

        val dLat = kaabaLat - uLat
        val dLng = kaabaLng - uLng

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(uLat) * cos(kaabaLat) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (r * c).toInt()
    }
}
