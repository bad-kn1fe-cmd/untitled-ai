package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global controller for 3D Parallax effect on avatar frames across the entire application.
 * Tilts the avatar frame overlay relative to the inner circular avatar based on phone rotation.
 */
object FrameParallaxController : SensorEventListener {

    var isEnabled by mutableStateOf(false)
        private set

    var sensitivity by mutableFloatStateOf(1.0f)
        private set

    var maxOffsetDp by mutableFloatStateOf(12f)
        private set

    var currentTiltX by mutableFloatStateOf(0f)
        private set

    var currentTiltY by mutableFloatStateOf(0f)
        private set

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isRegistered = false

    private var rawX = 0f
    private var rawY = 0f

    fun init(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
    }

    fun updateConfig(context: Context, enabled: Boolean, sens: Float, maxOffset: Float) {
        init(context)
        isEnabled = enabled
        sensitivity = sens.coerceIn(0.2f, 3.0f)
        maxOffsetDp = maxOffset.coerceIn(2f, 32f)

        if (enabled) {
            if (!isRegistered) {
                register()
            }
        } else {
            if (isRegistered) {
                unregister()
            }
            currentTiltX = 0f
            currentTiltY = 0f
            rawX = 0f
            rawY = 0f
        }
    }

    private fun register() {
        val sm = sensorManager ?: return
        val sensor = accelerometer ?: return
        try {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            isRegistered = true
        } catch (_: Exception) {}
    }

    private fun unregister() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isEnabled || event == null) return
        val x = event.values[0]
        val y = event.values[1] - 4.5f // Standard ergonomic tilt angle while holding phone

        val bound = maxOffsetDp
        val targetX = (-x * 2.8f * sensitivity).coerceIn(-bound, bound)
        val targetY = (y * 2.8f * sensitivity).coerceIn(-bound, bound)

        // Smooth IIR filter
        rawX = rawX * 0.82f + targetX * 0.18f
        rawY = rawY * 0.82f + targetY * 0.18f

        currentTiltX = rawX
        currentTiltY = rawY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
