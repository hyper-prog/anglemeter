package com.anglemeter.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.anglemeter.app.sensor.OrientationSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class AngleMeterViewModel(application: Application) : AndroidViewModel(application) {

    private val coincidenceThresholdDeg = 0.2f

    private val orientationSensorManager = OrientationSensorManager(
        context = application.applicationContext,
        movementThresholdDeg = 0.3f,
        smoothingFactor = 0.2f
    )

    private val _uiState = MutableStateFlow(AngleMeterUiState())
    val uiState: StateFlow<AngleMeterUiState> = _uiState.asStateFlow()

    init {
        orientationSensorManager.onAngleChanged = { angleDeg ->
            onSensorAngle(angleDeg)
        }

        orientationSensorManager.onLabelRotationChanged = { rotationDeg ->
            onLabelRotationChanged(rotationDeg)
        }

        orientationSensorManager.onScreenTiltRatioChanged = { tiltRatio ->
            onScreenTiltRatioChanged(tiltRatio)
        }
    }

    fun startSensors() {
        orientationSensorManager.start()
    }

    fun stopSensors() {
        orientationSensorManager.stop()
    }

    fun toggleCamera() {
        _uiState.update { current ->
            current.copy(cameraEnabled = !current.cameraEnabled)
        }
    }

    fun switchCamera() {
        _uiState.update { current ->
            if (!current.cameraEnabled) {
                return@update current
            }

            val nextLens = if (current.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_BACK) {
                androidx.camera.core.CameraSelector.LENS_FACING_FRONT
            } else {
                androidx.camera.core.CameraSelector.LENS_FACING_BACK
            }
            current.copy(lensFacing = nextLens)
        }
    }

    fun setCameraEnabled(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(cameraEnabled = enabled)
        }
    }

    fun toggleOverlayColorScheme() {
        _uiState.update { current ->
            val nextScheme = if (current.overlayColorScheme == OverlayColorScheme.BlackBrown) {
                OverlayColorScheme.WhitePink
            } else {
                OverlayColorScheme.BlackBrown
            }

            current.copy(overlayColorScheme = nextScheme)
        }
    }

    private fun onSensorAngle(angleDeg: Float) {
        val primary = abs(angleDeg).coerceIn(0f, 90f)
        val secondary = 90f - primary
        val coincident = primary <= coincidenceThresholdDeg

        _uiState.update { current ->
            current.copy(
                deviceAngleDeg = angleDeg,
                primaryDeg = primary,
                secondaryDeg = secondary,
                isCoincident = coincident
            )
        }
    }

    private fun onLabelRotationChanged(rotationDeg: Float) {
        _uiState.update { current ->
            if (kotlin.math.abs(current.angleLabelRotationDeg - rotationDeg) < 0.01f) {
                return@update current
            }

            current.copy(angleLabelRotationDeg = rotationDeg)
        }
    }

    private fun onScreenTiltRatioChanged(tiltRatio: Float) {
        _uiState.update { current ->
            if (kotlin.math.abs(current.screenTiltRatio - tiltRatio) < 0.01f) {
                return@update current
            }

            current.copy(screenTiltRatio = tiltRatio)
        }
    }

    override fun onCleared() {
        orientationSensorManager.stop()
        super.onCleared()
    }
}
