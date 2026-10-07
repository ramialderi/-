package com.example.prayers.data

import android.content.Context
import android.content.SharedPreferences
import com.example.prayers.model.CalculationMethod
import com.example.prayers.model.JuristicMethod
import com.example.prayers.model.LocationInfo
import com.example.prayers.model.Prayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ela_salaty_prefs", Context.MODE_PRIVATE)

    private val _locationFlow = MutableStateFlow(getLocation())
    val locationFlow: StateFlow<LocationInfo> = _locationFlow.asStateFlow()

    private val _methodFlow = MutableStateFlow(getCalculationMethod())
    val methodFlow: StateFlow<CalculationMethod> = _methodFlow.asStateFlow()

    private val _juristicFlow = MutableStateFlow(getJuristicMethod())
    val juristicFlow: StateFlow<JuristicMethod> = _juristicFlow.asStateFlow()

    private val _hijriAdjustmentFlow = MutableStateFlow(getHijriAdjustment())
    val hijriAdjustmentFlow: StateFlow<Int> = _hijriAdjustmentFlow.asStateFlow()

    private val _notificationsEnabledFlow = MutableStateFlow(areNotificationsEnabled())
    val notificationsEnabledFlow: StateFlow<Boolean> = _notificationsEnabledFlow.asStateFlow()

    fun getLocation(): LocationInfo {
        val city = prefs.getString(KEY_CITY, CitiesData.DEFAULT_CITY.cityName) ?: CitiesData.DEFAULT_CITY.cityName
        val country = prefs.getString(KEY_COUNTRY, CitiesData.DEFAULT_CITY.countryName) ?: CitiesData.DEFAULT_CITY.countryName
        val lat = prefs.getFloat(KEY_LAT, CitiesData.DEFAULT_CITY.latitude.toFloat()).toDouble()
        val lng = prefs.getFloat(KEY_LNG, CitiesData.DEFAULT_CITY.longitude.toFloat()).toDouble()
        val tz = prefs.getString(KEY_TZ, CitiesData.DEFAULT_CITY.timeZoneId) ?: CitiesData.DEFAULT_CITY.timeZoneId
        return LocationInfo(city, country, lat, lng, tz)
    }

    fun saveLocation(location: LocationInfo) {
        prefs.edit()
            .putString(KEY_CITY, location.cityName)
            .putString(KEY_COUNTRY, location.countryName)
            .putFloat(KEY_LAT, location.latitude.toFloat())
            .putFloat(KEY_LNG, location.longitude.toFloat())
            .putString(KEY_TZ, location.timeZoneId)
            .apply()
        _locationFlow.value = location
    }

    fun getCalculationMethod(): CalculationMethod {
        val name = prefs.getString(KEY_METHOD, CalculationMethod.UMM_AL_QURA.name)
        return try {
            CalculationMethod.valueOf(name ?: CalculationMethod.UMM_AL_QURA.name)
        } catch (e: Exception) {
            CalculationMethod.UMM_AL_QURA
        }
    }

    fun saveCalculationMethod(method: CalculationMethod) {
        prefs.edit().putString(KEY_METHOD, method.name).apply()
        _methodFlow.value = method
    }

    fun getJuristicMethod(): JuristicMethod {
        val name = prefs.getString(KEY_JURISTIC, JuristicMethod.STANDARD.name)
        return try {
            JuristicMethod.valueOf(name ?: JuristicMethod.STANDARD.name)
        } catch (e: Exception) {
            JuristicMethod.STANDARD
        }
    }

    fun saveJuristicMethod(method: JuristicMethod) {
        prefs.edit().putString(KEY_JURISTIC, method.name).apply()
        _juristicFlow.value = method
    }

    fun getHijriAdjustment(): Int = prefs.getInt(KEY_HIJRI_ADJ, 0)

    fun saveHijriAdjustment(adjustment: Int) {
        prefs.edit().putInt(KEY_HIJRI_ADJ, adjustment).apply()
        _hijriAdjustmentFlow.value = adjustment
    }

    fun areNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _notificationsEnabledFlow.value = enabled
    }

    fun isPrayerNotificationEnabled(prayer: Prayer): Boolean =
        prefs.getBoolean("pref_notif_${prayer.name}", true)

    fun setPrayerNotificationEnabled(prayer: Prayer, enabled: Boolean) {
        prefs.edit().putBoolean("pref_notif_${prayer.name}", enabled).apply()
    }

    fun isAzanSoundEnabled(): Boolean =
        prefs.getBoolean("pref_azan_sound_enabled", true)

    fun setAzanSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pref_azan_sound_enabled", enabled).apply()
    }

    companion object {
        private const val KEY_CITY = "pref_city"
        private const val KEY_COUNTRY = "pref_country"
        private const val KEY_LAT = "pref_lat"
        private const val KEY_LNG = "pref_lng"
        private const val KEY_TZ = "pref_tz"
        private const val KEY_METHOD = "pref_method"
        private const val KEY_JURISTIC = "pref_juristic"
        private const val KEY_HIJRI_ADJ = "pref_hijri_adj"
        private const val KEY_NOTIFICATIONS = "pref_notifications"
    }
}
