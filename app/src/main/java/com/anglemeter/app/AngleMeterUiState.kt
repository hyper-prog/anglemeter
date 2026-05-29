package com.anglemeter.app

import androidx.camera.core.CameraSelector

enum class OverlayColorScheme {
    BlackBrown,
    WhitePink
}

data class AngleMeterUiState(
    val deviceAngleDeg: Float = 0f,
    val screenTiltRatio: Float = 0f,
    val primaryDeg: Float = 0f,
    val secondaryDeg: Float = 90f,
    val isCoincident: Boolean = true,
    val angleLabelRotationDeg: Float = 0f,
    val cameraEnabled: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val overlayColorScheme: OverlayColorScheme = OverlayColorScheme.BlackBrown
)
