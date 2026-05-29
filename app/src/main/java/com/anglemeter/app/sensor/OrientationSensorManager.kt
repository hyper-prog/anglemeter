package com.anglemeter.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class OrientationSensorManager(
    context: Context,
    private val movementThresholdDeg: Float = 0.3f,
    private val smoothingFactor: Float = 0.2f
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gameRotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val rotationMatrix = FloatArray(9)
    private var isRunning = false
    private var hasFilterValue = false
    private var filteredAngleDeg = 0f
    private var lastEmittedAngleDeg = Float.NaN
    private var lastLabelRotationDeg = Float.NaN
    private var lastTiltRatio = Float.NaN

    private val minProjectionMagnitude = 0.05f
    private val crossPeriodDeg = 90f
    private val halfCrossPeriodDeg = crossPeriodDeg / 2f
    private val landscapeSwitchThresholdDeg = 45f
    private val tiltRatioEpsilon = 0.01f

    var onAngleChanged: ((Float) -> Unit)? = null
    var onLabelRotationChanged: ((Float) -> Unit)? = null
    var onScreenTiltRatioChanged: ((Float) -> Unit)? = null

    fun start() {
        if (isRunning) {
            return
        }

        val selectedSensor = gameRotationSensor ?: rotationSensor ?: return
        sensorManager.registerListener(this, selectedSensor, SensorManager.SENSOR_DELAY_GAME)
        isRunning = true
    }

    fun stop() {
        if (!isRunning) {
            return
        }

        sensorManager.unregisterListener(this)
        isRunning = false
        hasFilterValue = false
        lastEmittedAngleDeg = Float.NaN
        lastLabelRotationDeg = Float.NaN
        lastTiltRatio = Float.NaN
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }

        if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR && event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) {
            return
        }

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        // Project world-up onto the device screen plane, then compute the in-plane line angle.
        // This keeps the displayed horizon angle stable even when pitch/yaw are present.
        val upX = rotationMatrix[6]
        val upY = rotationMatrix[7]
        val projectionMagnitude = sqrt(upX * upX + upY * upY)

        val tiltRatio = tiltRatioFromProjection(projectionMagnitude)
        if (lastTiltRatio.isNaN() || abs(tiltRatio - lastTiltRatio) >= tiltRatioEpsilon) {
            lastTiltRatio = tiltRatio
            onScreenTiltRatioChanged?.invoke(tiltRatio)
        }

        if (projectionMagnitude < minProjectionMagnitude) {
            return
        }

        val rawAngleDeg = Math.toDegrees(atan2(upX.toDouble(), upY.toDouble())).toFloat()
        val labelRotationDeg = labelRotationFromUpVector(rawAngleDeg)
        if (lastLabelRotationDeg.isNaN() || abs(labelRotationDeg - lastLabelRotationDeg) > 0.01f) {
            lastLabelRotationDeg = labelRotationDeg
            onLabelRotationChanged?.invoke(labelRotationDeg)
        }

        val normalized = normalizeToCrossDomain(rawAngleDeg)
        val smoothed = applyLowPassPeriodic(normalized)

        val delta = if (lastEmittedAngleDeg.isNaN()) {
            Float.POSITIVE_INFINITY
        } else {
            abs(shortestPeriodicDelta(smoothed, lastEmittedAngleDeg, crossPeriodDeg))
        }

        if (lastEmittedAngleDeg.isNaN() || delta >= movementThresholdDeg) {
            lastEmittedAngleDeg = smoothed
            onAngleChanged?.invoke(smoothed)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun applyLowPassPeriodic(current: Float): Float {
        if (!hasFilterValue) {
            filteredAngleDeg = current
            hasFilterValue = true
            return filteredAngleDeg
        }

        val delta = shortestPeriodicDelta(current, filteredAngleDeg, crossPeriodDeg)
        filteredAngleDeg += smoothingFactor * delta
        filteredAngleDeg = normalizeToCrossDomain(filteredAngleDeg)
        return filteredAngleDeg
    }

    private fun normalizeToCrossDomain(angleDeg: Float): Float {
        return ((angleDeg + halfCrossPeriodDeg) % crossPeriodDeg + crossPeriodDeg) % crossPeriodDeg - halfCrossPeriodDeg
    }

    private fun shortestPeriodicDelta(value: Float, reference: Float, period: Float): Float {
        var delta = (value - reference) % period
        val halfPeriod = period / 2f

        if (delta > halfPeriod) {
            delta -= period
        } else if (delta < -halfPeriod) {
            delta += period
        }

        return delta
    }

    private fun labelRotationFromUpVector(rawAngleDeg: Float): Float {
        return when {
            rawAngleDeg >= landscapeSwitchThresholdDeg && rawAngleDeg <= (180f - landscapeSwitchThresholdDeg) -> 90f
            rawAngleDeg <= -landscapeSwitchThresholdDeg && rawAngleDeg >= (-180f + landscapeSwitchThresholdDeg) -> -90f
            rawAngleDeg > (180f - landscapeSwitchThresholdDeg) || rawAngleDeg < (-180f + landscapeSwitchThresholdDeg) -> 180f
            else -> 0f
        }
    }

    private fun tiltRatioFromProjection(projectionMagnitude: Float): Float {
        val clampedProjection = min(1f, max(0f, projectionMagnitude))
        val tiltRadians = acos(clampedProjection.toDouble())
        return (tiltRadians / (PI / 2.0)).toFloat().coerceIn(0f, 1f)
    }
}
