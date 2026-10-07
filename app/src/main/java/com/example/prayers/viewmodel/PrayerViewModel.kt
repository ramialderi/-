package com.example.prayers.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prayers.audio.AzanAudioPlayer
import com.example.prayers.calculator.PrayerCalculator
import com.example.prayers.data.CitiesData
import com.example.prayers.data.HijriDate
import com.example.prayers.data.HijriDateHelper
import com.example.prayers.data.UserPreferencesRepository
import com.example.prayers.data.local.PrayerDatabase
import com.example.prayers.data.local.PrayerLogEntity
import com.example.prayers.data.local.TasbihItemEntity
import com.example.prayers.model.CalculationMethod
import com.example.prayers.model.JuristicMethod
import com.example.prayers.model.LocationInfo
import com.example.prayers.model.Prayer
import com.example.prayers.model.PrayerDaySchedule
import com.example.prayers.model.PrayerRealtimeStatus
import com.example.widget.PrayerAppWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = UserPreferencesRepository(application)
    private val db = PrayerDatabase.getDatabase(application)
    private val prayerDao = db.prayerDao()

    val location: StateFlow<LocationInfo> = prefsRepo.locationFlow
    val calculationMethod: StateFlow<CalculationMethod> = prefsRepo.methodFlow
    val juristicMethod: StateFlow<JuristicMethod> = prefsRepo.juristicFlow
    val hijriAdjustment: StateFlow<Int> = prefsRepo.hijriAdjustmentFlow
    val notificationsEnabled: StateFlow<Boolean> = prefsRepo.notificationsEnabledFlow

    private val _schedule = MutableStateFlow(calculateCurrentSchedule())
    val schedule: StateFlow<PrayerDaySchedule> = _schedule.asStateFlow()

    private val _realtimeStatus = MutableStateFlow(
        PrayerCalculator.getRealtimeStatus(System.currentTimeMillis(), _schedule.value)
    )
    val realtimeStatus: StateFlow<PrayerRealtimeStatus> = _realtimeStatus.asStateFlow()

    private val _hijriDate = MutableStateFlow(
        HijriDateHelper.getHijriDate(Calendar.getInstance(), prefsRepo.getHijriAdjustment())
    )
    val hijriDate: StateFlow<HijriDate> = _hijriDate.asStateFlow()

    // Compass & Qibla
    val qiblaCompassManager = com.example.prayers.sensor.QiblaCompassManager(application)
    val compassOrientation: StateFlow<com.example.prayers.sensor.CompassOrientation> =
        qiblaCompassManager.orientationFlow

    val compassAzimuth: StateFlow<Float> = MutableStateFlow(0f).apply {
        viewModelScope.launch {
            compassOrientation.collectLatest {
                value = it.trueAzimuth
            }
        }
    }

    private val _qiblaBearing = MutableStateFlow(
        PrayerCalculator.calculateQiblaBearing(location.value.latitude, location.value.longitude)
    )
    val qiblaBearing: StateFlow<Float> = _qiblaBearing.asStateFlow()

    private val _distanceToKaabaKm = MutableStateFlow(
        PrayerCalculator.calculateDistanceToKaabaKm(location.value.latitude, location.value.longitude)
    )
    val distanceToKaabaKm: StateFlow<Int> = _distanceToKaabaKm.asStateFlow()

    // Today's prayer log
    private val todayDateStr: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _todayPrayerLog = MutableStateFlow(PrayerLogEntity(dateString = todayDateStr))
    val todayPrayerLog: StateFlow<PrayerLogEntity> = _todayPrayerLog.asStateFlow()

    // Tasbih
    private val _tasbihList = MutableStateFlow<List<TasbihItemEntity>>(emptyList())
    val tasbihList: StateFlow<List<TasbihItemEntity>> = _tasbihList.asStateFlow()

    // Simulation switcher for testing the two states requested by user
    private val _isSimulationActive = MutableStateFlow(false)
    val isSimulationActive: StateFlow<Boolean> = _isSimulationActive.asStateFlow()

    private val _simulateEntryWindow = MutableStateFlow(true) // true = first 15 mins; false = countdown mode
    val simulateEntryWindow: StateFlow<Boolean> = _simulateEntryWindow.asStateFlow()

    // Azan playing state
    private val _isAzanPlaying = MutableStateFlow(false)
    val isAzanPlaying: StateFlow<Boolean> = _isAzanPlaying.asStateFlow()

    // Location loading feedback
    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    init {
        // Recalculate when settings change
        viewModelScope.launch {
            location.collectLatest {
                recalculate()
                updateQiblaValues()
                qiblaCompassManager.setLocation(it.latitude, it.longitude)
                PrayerAppWidget.updateAllWidgets(getApplication())
                com.example.prayers.notifications.PrayerNotificationScheduler.scheduleUpcomingPrayers(getApplication())
            }
        }
        viewModelScope.launch {
            calculationMethod.collectLatest {
                recalculate()
                PrayerAppWidget.updateAllWidgets(getApplication())
                com.example.prayers.notifications.PrayerNotificationScheduler.scheduleUpcomingPrayers(getApplication())
            }
        }
        viewModelScope.launch {
            juristicMethod.collectLatest {
                recalculate()
                PrayerAppWidget.updateAllWidgets(getApplication())
                com.example.prayers.notifications.PrayerNotificationScheduler.scheduleUpcomingPrayers(getApplication())
            }
        }
        viewModelScope.launch {
            hijriAdjustment.collectLatest { adj ->
                _hijriDate.value = HijriDateHelper.getHijriDate(Calendar.getInstance(), adj)
                PrayerAppWidget.updateAllWidgets(getApplication())
            }
        }

        // Realtime 1-second ticker for precise live updates
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                updateRealtimeStatus()
                delay(1000)
            }
        }

        // Room prayer log flow
        viewModelScope.launch {
            prayerDao.getPrayerLogForDate(todayDateStr).collectLatest { log ->
                if (log != null) {
                    _todayPrayerLog.value = log
                } else {
                    val newLog = PrayerLogEntity(dateString = todayDateStr)
                    prayerDao.insertOrUpdatePrayerLog(newLog)
                    _todayPrayerLog.value = newLog
                }
            }
        }

        // Room tasbih list flow
        viewModelScope.launch {
            prayerDao.getAllTasbihItems().collectLatest { items ->
                _tasbihList.value = items
            }
        }

        qiblaCompassManager.setLocation(location.value.latitude, location.value.longitude)
        qiblaCompassManager.startListening()
    }

    private fun calculateCurrentSchedule(): PrayerDaySchedule {
        val loc = location.value
        val method = calculationMethod.value
        val juristic = juristicMethod.value
        return PrayerCalculator.calculateDaySchedule(
            calendar = Calendar.getInstance(),
            latitude = loc.latitude,
            longitude = loc.longitude,
            method = method,
            juristic = juristic
        )
    }

    private fun recalculate() {
        val newSchedule = calculateCurrentSchedule()
        _schedule.value = newSchedule
        updateRealtimeStatus()
    }

    private fun updateQiblaValues() {
        val loc = location.value
        _qiblaBearing.value = PrayerCalculator.calculateQiblaBearing(loc.latitude, loc.longitude)
        _distanceToKaabaKm.value = PrayerCalculator.calculateDistanceToKaabaKm(loc.latitude, loc.longitude)
    }

    private fun updateRealtimeStatus() {
        val now = System.currentTimeMillis()
        _realtimeStatus.value = PrayerCalculator.getRealtimeStatus(
            currentTimeMillis = now,
            schedule = _schedule.value,
            simulationMode = _isSimulationActive.value,
            simulateEntryWindow = _simulateEntryWindow.value
        )
    }

    fun setSimulationMode(active: Boolean, simulateEntry: Boolean = true) {
        _isSimulationActive.value = active
        _simulateEntryWindow.value = simulateEntry
        updateRealtimeStatus()
    }

    fun selectCity(city: LocationInfo) {
        prefsRepo.saveLocation(city)
    }

    fun selectCalculationMethod(method: CalculationMethod) {
        prefsRepo.saveCalculationMethod(method)
    }

    fun selectJuristicMethod(method: JuristicMethod) {
        prefsRepo.saveJuristicMethod(method)
    }

    fun adjustHijri(offset: Int) {
        val current = prefsRepo.getHijriAdjustment()
        val next = (current + offset).coerceIn(-3, 3)
        prefsRepo.saveHijriAdjustment(next)
    }

    fun setHijriAdjustment(adjustment: Int) {
        prefsRepo.saveHijriAdjustment(adjustment.coerceIn(-3, 3))
    }

    fun toggleNotifications(enabled: Boolean) {
        prefsRepo.setNotificationsEnabled(enabled)
    }

    fun playAzanPreview() {
        if (_isAzanPlaying.value) {
            AzanAudioPlayer.stop()
            _isAzanPlaying.value = false
        } else {
            _isAzanPlaying.value = true
            AzanAudioPlayer.playPrayerAlert(getApplication()) {
                _isAzanPlaying.value = false
            }
        }
    }

    fun togglePrayerCompleted(prayer: Prayer) {
        val current = _todayPrayerLog.value
        val updated = when (prayer) {
            Prayer.FAJR -> current.copy(fajrDone = !current.fajrDone)
            Prayer.DHUHR -> current.copy(dhuhrDone = !current.dhuhrDone)
            Prayer.ASR -> current.copy(asrDone = !current.asrDone)
            Prayer.MAGHRIB -> current.copy(maghribDone = !current.maghribDone)
            Prayer.ISHA -> current.copy(ishaDone = !current.ishaDone)
            else -> current
        }
        viewModelScope.launch {
            prayerDao.insertOrUpdatePrayerLog(updated)
            _todayPrayerLog.value = updated
        }
    }

    fun incrementTasbih(item: TasbihItemEntity) {
        val nextCount = item.count + 1
        val nextTotal = item.totalCount + 1
        val updated = item.copy(
            count = if (nextCount >= item.target) 0 else nextCount,
            totalCount = nextTotal
        )
        viewModelScope.launch {
            prayerDao.updateTasbihItem(updated)
        }
    }

    fun resetTasbih(item: TasbihItemEntity) {
        val updated = item.copy(count = 0)
        viewModelScope.launch {
            prayerDao.updateTasbihItem(updated)
        }
    }

    fun addCustomTasbih(title: String, target: Int) {
        viewModelScope.launch {
            prayerDao.insertTasbihItem(
                TasbihItemEntity(title = title, count = 0, target = target, totalCount = 0)
            )
        }
    }

    fun detectGpsLocation(context: Context) {
        _isDetectingLocation.value = true
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            _isDetectingLocation.value = false
            return
        }

        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var bestLoc: Location? = null
            for (p in providers) {
                if (lm.isProviderEnabled(p)) {
                    val l = lm.getLastKnownLocation(p)
                    if (l != null && (bestLoc == null || l.accuracy < bestLoc.accuracy)) {
                        bestLoc = l
                    }
                }
            }

            if (bestLoc != null) {
                val newLoc = LocationInfo(
                    cityName = "موقعي الحالي",
                    countryName = "إحداثيات GPS",
                    latitude = bestLoc.latitude,
                    longitude = bestLoc.longitude
                )
                prefsRepo.saveLocation(newLoc)
                _isDetectingLocation.value = false
            } else {
                // Request a single update
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        val newLoc = LocationInfo(
                            cityName = "موقعي الحالي",
                            countryName = "إحداثيات GPS",
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        prefsRepo.saveLocation(newLoc)
                        _isDetectingLocation.value = false
                        lm.removeUpdates(this)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        _isDetectingLocation.value = false
                    }
                }
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            _isDetectingLocation.value = false
        } catch (e: Exception) {
            e.printStackTrace()
            _isDetectingLocation.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        qiblaCompassManager.stopListening()
        AzanAudioPlayer.stop()
    }
}
