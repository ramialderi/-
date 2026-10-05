package com.example.prayers.data

import com.example.prayers.model.LocationInfo

object CitiesData {

    val PRESET_CITIES = listOf(
        LocationInfo("مكة المكرمة", "المملكة العربية السعودية", 21.4225, 39.8262, "Asia/Riyadh", "Makkah"),
        LocationInfo("المدينة المنورة", "المملكة العربية السعودية", 24.5247, 39.5692, "Asia/Riyadh", "Madinah"),
        LocationInfo("الحسكة", "سوريا", 36.5024, 40.7481, "Asia/Damascus", "Al-Hasakah"),
        LocationInfo("دمشق", "سوريا", 33.5138, 36.2765, "Asia/Damascus", "Damascus"),
        LocationInfo("الرياض", "المملكة العربية السعودية", 24.7136, 46.6753, "Asia/Riyadh", "Riyadh"),
        LocationInfo("جدة", "المملكة العربية السعودية", 21.5433, 39.1728, "Asia/Riyadh", "Jeddah"),
        LocationInfo("القدس الشريف", "فلسطين", 31.7683, 35.2137, "Asia/Jerusalem", "Jerusalem"),
        LocationInfo("القاهرة", "مصر", 30.0444, 31.2357, "Africa/Cairo", "Cairo"),
        LocationInfo("دبي", "الإمارات العربية المتحدة", 25.2048, 55.2708, "Asia/Dubai", "Dubai"),
        LocationInfo("أبوظبي", "الإمارات العربية المتحدة", 24.4539, 54.3773, "Asia/Dubai"),
        LocationInfo("عمّان", "الأردن", 31.9454, 35.9284, "Asia/Amman"),
        LocationInfo("بغداد", "العراق", 33.3152, 44.3661, "Asia/Baghdad"),
        LocationInfo("بيروت", "لبنان", 33.8938, 35.5018, "Asia/Beirut"),
        LocationInfo("الكويت", "الكويت", 29.3759, 47.9774, "Asia/Kuwait"),
        LocationInfo("الدوحة", "قطر", 25.2854, 51.5310, "Asia/Qatar"),
        LocationInfo("المنامة", "البحرين", 26.2285, 50.5860, "Asia/Bahrain"),
        LocationInfo("مسقط", "سلطنة عمان", 23.5880, 58.3829, "Asia/Muscat"),
        LocationInfo("صنعاء", "اليمن", 15.3694, 44.1910, "Asia/Aden"),
        LocationInfo("طرابلس", "ليبيا", 32.8872, 13.1913, "Africa/Tripoli"),
        LocationInfo("تونس", "تونس", 36.8065, 10.1815, "Africa/Tunis"),
        LocationInfo("الجزائر", "الجزائر", 36.7538, 3.0588, "Africa/Algiers"),
        LocationInfo("الرباط", "المغرب", 34.0209, -6.8416, "Africa/Casablanca"),
        LocationInfo("الخرطوم", "السودان", 15.5007, 32.5599, "Africa/Khartoum"),
        LocationInfo("إسطنبول", "تركيا", 41.0082, 28.9784, "Europe/Istanbul"),
        LocationInfo("جاكرتا", "إندونيسيا", -6.2088, 106.8456, "Asia/Jakarta"),
        LocationInfo("كوالالمبور", "ماليزيا", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        LocationInfo("لندن", "المملكة المتحدة", 51.5074, -0.1278, "Europe/London"),
        LocationInfo("باريس", "فرنسا", 48.8566, 2.3522, "Europe/Paris"),
        LocationInfo("برلين", "ألمانيا", 52.5200, 13.4050, "Europe/Berlin"),
        LocationInfo("نيويورك", "الولايات المتحدة", 40.7128, -74.0060, "America/New_York"),
        LocationInfo("سيدني", "أستراليا", -33.8688, 151.2093, "Australia/Sydney")
    )

    val DEFAULT_CITY = PRESET_CITIES[0] // Makkah Al-Mukarramah
}
