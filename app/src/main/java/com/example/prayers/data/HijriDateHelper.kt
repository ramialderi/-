package com.example.prayers.data

import java.util.Calendar

data class HijriDate(
    val day: Int,
    val month: Int,
    val monthName: String,
    val year: Int,
    val formatted: String,
    val formattedEn: String = "",
    val specialOccasion: String? = null
)

data class IslamicOccasionInfo(
    val title: String,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriMonthName: String,
    val description: String,
    val daysRemaining: Int,
    val hijriFormattedDate: String
)

object HijriDateHelper {

    private val HIJRI_MONTHS = listOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة"
    )

    private val HIJRI_MONTHS_EN = listOf(
        "Muharram",
        "Safar",
        "Rabi' I",
        "Rabi' II",
        "Jumada I",
        "Jumada II",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah"
    )

    fun getHijriDate(calendar: Calendar = Calendar.getInstance(), adjustmentDays: Int = 0): HijriDate {
        val cal = calendar.clone() as Calendar
        if (adjustmentDays != 0) {
            cal.add(Calendar.DAY_OF_YEAR, adjustmentDays)
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // Julian Day calculation
        val m = if (month > 2) month else month + 12
        val y = if (month > 2) year else year - 1
        val a = Math.floor(y / 100.0)
        val b = 2 - a + Math.floor(a / 4.0)
        val jd = Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + b - 1524.5

        // Epoch difference between Julian Date and Islamic Epoch (July 16, 622 CE = JD 1948439.5)
        val epochDays = jd - 1948439.5
        val hijriYear = Math.floor((30 * epochDays + 10646) / 10631.0).toInt()
        val yearStartJd = Math.floor((10631 * hijriYear - 10646) / 30.0)
        val daysInYear = (epochDays - yearStartJd).toInt()
        val hijriMonth = Math.min(12, Math.max(1, Math.floor((daysInYear * 11 + 330) / 325.0).toInt() + 1))
        
        // Month start
        val monthStartDay = Math.floor((325 * (hijriMonth - 1) + 2) / 11.0).toInt()
        val hijriDay = Math.max(1, Math.min(30, daysInYear - monthStartDay + 1))

        val monthName = HIJRI_MONTHS.getOrElse(hijriMonth - 1) { "محرم" }
        val monthNameEn = HIJRI_MONTHS_EN.getOrElse(hijriMonth - 1) { "Muharram" }
        val formatted = "$hijriDay $monthName $hijriYear هـ"
        val formattedEn = "$monthNameEn $hijriDay"

        val occasion = getOccasion(hijriDay, hijriMonth)

        return HijriDate(
            day = hijriDay,
            month = hijriMonth,
            monthName = monthName,
            year = hijriYear,
            formatted = formatted,
            formattedEn = formattedEn,
            specialOccasion = occasion
        )
    }

    private fun getOccasion(day: Int, month: Int): String? {
        return when (month) {
            1 -> if (day == 1) "رأس السنة الهجرية" else if (day == 10) "يوم عاشوراء" else null
            3 -> if (day == 12) "المولد النبوي الشريف" else null
            7 -> if (day == 27) "ذكرى الإسراء والمعراج" else null
            8 -> if (day == 15) "ليلة النصف من شعبان" else null
            9 -> "شهر رمضان المبارك"
            10 -> if (day in 1..3) "عيد الفطر المبارك" else null
            12 -> when (day) {
                in 1..8 -> "عشر ذي الحجة"
                9 -> "يوم عرفة"
                in 10..13 -> "عيد الأضحى المبارك وأيام التشريق"
                else -> null
            }
            else -> null
        }
    }

    private val ALL_ANNUAL_OCCASIONS = listOf(
        Triple("رأس السنة الهجرية", Pair(1, 1), "بداية العام الهجري الجديد واستذكار هجرة النبي ﷺ"),
        Triple("يوم عاشوراء", Pair(10, 1), "اليوم العاشر من محرم، صيام يكفر السنة الماضية"),
        Triple("المولد النبوي الشريف", Pair(12, 3), "ذكرى مولد خير الأنام محمد ﷺ"),
        Triple("ذكرى الإسراء والمعراج", Pair(27, 7), "معجزة الإسراء من المسجد الحرام للمسجد الأقصى والعروج للسماوات"),
        Triple("ليلة النصف من شعبان", Pair(15, 8), "ليلة مباركة يستحب قيامها وصيام نهارها"),
        Triple("أول أيام شهر رمضان المبارك", Pair(1, 9), "بداية شهر الصيام والقرآن والرحمة والمغفرة"),
        Triple("غزوة بدر الكبرى", Pair(17, 9), "يوم الفرقان بين الحق والباطل"),
        Triple("ليلة القدر المباركة (العشر الأواخر)", Pair(27, 9), "خير من ألف شهر، تتنزل فيها الملائكة والروح"),
        Triple("عيد الفطر المبارك", Pair(1, 10), "يوم الجائزة والفرح بإتمام صيام شهر رمضان"),
        Triple("أول أيام عشر ذي الحجة", Pair(1, 12), "أيام العمل الصالح فيها أحب إلى الله تعالى من غيرها"),
        Triple("يوم عرفة المبارك", Pair(9, 12), "أعظم أيام العام، صيامه يكفر سنة ماضية وسنة باقية"),
        Triple("عيد الأضحى المبارك", Pair(10, 12), "يوم النحر وذبح الأضاحي والفرح الأكبر"),
        Triple("أيام التشريق", Pair(11, 12), "أيام أكل وشرب وذكر لله تعالى")
    )

    fun getUpcomingOccasions(currentHijri: HijriDate, limit: Int = 8): List<IslamicOccasionInfo> {
        val currentDayOfYear = (currentHijri.month - 1) * 29.53 + currentHijri.day

        return ALL_ANNUAL_OCCASIONS.map { (title, datePair, desc) ->
            val day = datePair.first
            val month = datePair.second
            val occasionDayOfYear = (month - 1) * 29.53 + day

            val daysRemaining = if (occasionDayOfYear >= currentDayOfYear) {
                (occasionDayOfYear - currentDayOfYear).toInt()
            } else {
                (354.36 - currentDayOfYear + occasionDayOfYear).toInt()
            }

            val monthName = HIJRI_MONTHS.getOrElse(month - 1) { "" }
            val formattedDate = "$day $monthName"

            IslamicOccasionInfo(
                title = title,
                hijriDay = day,
                hijriMonth = month,
                hijriMonthName = monthName,
                description = desc,
                daysRemaining = daysRemaining,
                hijriFormattedDate = formattedDate
            )
        }.sortedBy { it.daysRemaining }.take(limit)
    }

    fun isWhiteDay(day: Int): Boolean = day in 13..15
}
