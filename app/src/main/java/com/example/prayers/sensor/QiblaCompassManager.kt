package com.example.prayers.sensor

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassOrientation(
    val magneticAzimuth: Float = 0f,
    val trueAzimuth: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isFlat: Boolean = true,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
)

class QiblaCompassManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val _orientationFlow = MutableStateFlow(CompassOrientation())
    val orientationFlow: StateFlow<CompassOrientation> = _orientationFlow.asStateFlow()

    private var userLat: Double = 21.4225
    private var userLng: Double = 39.8262

    // Low-pass smoothing accumulators (unit vector)
    private var smoothCos = 1.0
    private var smoothSin = 0.0
    private val alpha = 0.18 // Smoothing factor (0.18 provides silky smooth response)

    private var rotationSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravityValues = FloatArray(3)
    private val magneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false

    init {
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

        if (rotationSensor == null) {
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        }
    }

    fun setLocation(lat: Double, lng: Double) {
        userLat = lat
        userLng = lng
    }

    fun startListening() {
        val sm = sensorManager ?: return
        if (rotationSensor != null) {
            sm.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelerometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var accuracy = event.accuracy

        var calculated = false

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
            event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        ) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            calculated = true
        } else {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magneticValues, 0, 3)
                hasMagnetic = true
            }

            if (hasGravity && hasMagnetic) {
                calculated = SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, magneticValues)
            }
        }

        if (calculated) {
            SensorManager.getOrientation(rotationMatrix, orientation)

            var rawAzimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
            if (rawAzimuthDeg < 0) rawAzimuthDeg += 360f

            val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

            // Circular exponential smoothing
            val rad = Math.toRadians(rawAzimuthDeg.toDouble())
            smoothCos = (1.0 - alpha) * smoothCos + alpha * cos(rad)
            smoothSin = (1.0 - alpha) * smoothSin + alpha * sin(rad)
            var smoothedAzimuth = Math.toDegrees(atan2(smoothSin, smoothCos)).toFloat()
            if (smoothedAzimuth < 0) smoothedAzimuth += 360f

            // Calculate True North using GeomagneticField
            val geoField = try {
                GeomagneticField(
                    userLat.toFloat(),
                    userLng.toFloat(),
                    0f,
                    System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }

            val declination = geoField?.declination ?: 0f
            var trueAzimuth = (smoothedAzimuth + declination + 360f) % 360f

            // Check if phone is held reasonably flat (within 22 degrees)
            val isFlat = kotlin.math.abs(pitchDeg) < 22f && kotlin.math.abs(rollDeg) < 22f

            _orientationFlow.value = CompassOrientation(
                magneticAzimuth = smoothedAzimuth,
                trueAzimuth = trueAzimuth,
                pitch = pitchDeg,
                roll = rollDeg,
                isFlat = isFlat,
                accuracy = accuracy
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _orientationFlow.value = _orientationFlow.value.copy(accuracy = accuracy)
    }
}
