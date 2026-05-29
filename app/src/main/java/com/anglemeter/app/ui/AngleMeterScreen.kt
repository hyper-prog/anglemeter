package com.anglemeter.app.ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaActionSound
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anglemeter.app.AngleMeterViewModel
import com.anglemeter.app.OverlayColorScheme
import com.anglemeter.app.R
import com.anglemeter.app.camera.CameraPreviewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun AngleMeterScreen(
    viewModel: AngleMeterViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hostView = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val shutterSound = remember {
        MediaActionSound().apply {
            load(MediaActionSound.SHUTTER_CLICK)
        }
    }
    val screenFlashAlpha = remember {
        Animatable(0f)
    }

    val cameraController = remember {
        CameraPreviewController(context)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            viewModel.setCameraEnabled(false)
            Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setCameraEnabled(true)
        }
    }

    DisposableEffect(Unit) {
        viewModel.startSensors()
        onDispose {
            viewModel.stopSensors()
            cameraController.stopPreview()
            shutterSound.release()
        }
    }

    LaunchedEffect(uiState.cameraEnabled, hasCameraPermission) {
        if (uiState.cameraEnabled && !hasCameraPermission) {
            viewModel.setCameraEnabled(false)
        }
    }

    val topRightCornerAngleDeg = if (uiState.deviceAngleDeg < 0f) {
        uiState.primaryDeg
    } else {
        uiState.secondaryDeg
    }
    val bottomLeftCornerAngleDeg = if (uiState.deviceAngleDeg < 0f) {
        uiState.secondaryDeg
    } else {
        uiState.primaryDeg
    }

    val topRightCornerAngleLabel = context.getString(R.string.angle_single_format, topRightCornerAngleDeg)
    val bottomLeftCornerAngleLabel = context.getString(R.string.angle_single_format, bottomLeftCornerAngleDeg)

    val angleLabelRotationDeg = uiState.angleLabelRotationDeg
    val tiltWarningBarMaxWidthFraction = 0.92f
    val tiltWarningBarVisibleThreshold = 0.01f
    val tiltWarningBarWidthFraction = (tiltWarningBarMaxWidthFraction * uiState.screenTiltRatio)
        .coerceIn(0f, tiltWarningBarMaxWidthFraction)
    val overlaySchemeToggleDesc = stringResource(id = R.string.overlay_scheme_toggle_desc)
    val screenshotButtonDesc = stringResource(id = R.string.screenshot_button_desc)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (uiState.overlayColorScheme == OverlayColorScheme.BlackBrown) {
                    Color(0xFFF0EEE8)
                } else {
                    Color(0xFFB8B8B8)
                }
            )
    ) {
        if (uiState.cameraEnabled && hasCameraPermission) {
            val previewView = remember {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            }

            AndroidView(
                factory = {
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            LaunchedEffect(uiState.lensFacing, uiState.cameraEnabled, hasCameraPermission) {
                cameraController.startPreview(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    lensFacing = uiState.lensFacing,
                    mirrorFrontPreview = true
                )
            }
        } else {
            LaunchedEffect(uiState.cameraEnabled) {
                cameraController.stopPreview()
            }
        }

        AngleOverlayCanvas(
            angleDeg = uiState.deviceAngleDeg,
            isCoincident = uiState.isCoincident,
            colorScheme = uiState.overlayColorScheme,
            modifier = Modifier.fillMaxSize()
        )

        if (!uiState.isCoincident) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .fillMaxWidth(0.5f)
                    .fillMaxSize(0.5f)
                    .padding(16.dp)
            ) {
                AngleValueLabel(
                    text = topRightCornerAngleLabel,
                    rotationDeg = angleLabelRotationDeg,
                    modifier = Modifier.align(Alignment.TopEnd)
                )

                AngleValueLabel(
                    text = bottomLeftCornerAngleLabel,
                    rotationDeg = angleLabelRotationDeg,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        if (tiltWarningBarWidthFraction > tiltWarningBarVisibleThreshold) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(tiltWarningBarWidthFraction)
                    .height(12.dp)
                    .background(
                        color = Color.Red.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }

        IconButton(
            onClick = {
                shutterSound.play(MediaActionSound.SHUTTER_CLICK)

                coroutineScope.launch {
                    val screenshotBitmap = captureScreenshotBitmap(hostView, context)
                        ?: hostView.rootView.drawToBitmap()

                    screenFlashAlpha.snapTo(0.5f)
                    screenFlashAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 180)
                    )

                    saveScreenshotToGallery(context, screenshotBitmap)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(end = 12.dp, bottom = 28.dp)
                .size(52.dp)
                .background(
                    color = Color(0xCCFFFFFF),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = screenshotButtonDesc,
                tint = Color.Black,
                modifier = Modifier.graphicsLayer {
                    rotationZ = angleLabelRotationDeg
                }
            )
        }

        if (screenFlashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = screenFlashAlpha.value))
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(12.dp)
                .background(
                    color = Color(0x99FFFFFF),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(4.dp)
        ) {
            IconButton(
                onClick = {
                    viewModel.toggleOverlayColorScheme()
                }
            ) {
                SchemeToggleIcon(
                    colorScheme = uiState.overlayColorScheme,
                    modifier = Modifier
                        .size(22.dp)
                        .semantics {
                            contentDescription = overlaySchemeToggleDesc
                        }
                )
            }

            IconButton(
                onClick = {
                    if (uiState.cameraEnabled) {
                        viewModel.setCameraEnabled(false)
                        return@IconButton
                    }

                    if (hasCameraPermission) {
                        viewModel.setCameraEnabled(true)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            ) {
                Icon(
                    imageVector = if (uiState.cameraEnabled) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                    contentDescription = stringResource(id = R.string.camera_toggle_desc),
                    tint = Color.Black,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = angleLabelRotationDeg
                    }
                )
            }

            if (uiState.cameraEnabled) {
                IconButton(
                    onClick = {
                        viewModel.switchCamera()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = stringResource(id = R.string.camera_switch_desc),
                        tint = Color.Black,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = angleLabelRotationDeg
                        }
                    )
                }
            }
        }
    }
}

private suspend fun captureScreenshotBitmap(view: View, context: Context): Bitmap? {
    if (view.width <= 0 || view.height <= 0) {
        return null
    }

    val activity = context.findActivity() ?: return null
    val location = IntArray(2)
    view.getLocationInWindow(location)
    val captureRect = Rect(
        location[0],
        location[1],
        location[0] + view.width,
        location[1] + view.height
    )

    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    return suspendCancellableCoroutine { continuation ->
        PixelCopy.request(
            activity.window,
            captureRect,
            bitmap,
            { copyResult ->
                if (!continuation.isActive) {
                    bitmap.recycle()
                    return@request
                }

                if (copyResult == PixelCopy.SUCCESS) {
                    continuation.resume(bitmap)
                } else {
                    bitmap.recycle()
                    continuation.resume(null)
                }
            },
            Handler(Looper.getMainLooper())
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private suspend fun saveScreenshotToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    return withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val fileName = "AngleMeter_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AngleMeter")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext false

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    throw IllegalStateException("Could not compress screenshot bitmap")
                }
            } ?: throw IllegalStateException("Could not open output stream for screenshot")

            val finalizeValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, finalizeValues, null, null)
            true
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        } finally {
            bitmap.recycle()
        }
    }
}

@Composable
private fun AngleValueLabel(
    text: String,
    rotationDeg: Float,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = Color.Black,
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotationDeg
            }
            .background(
                color = Color(0xCCFFFFFF),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun SchemeToggleIcon(
    colorScheme: OverlayColorScheme,
    modifier: Modifier = Modifier
) {
    val leftColor = if (colorScheme == OverlayColorScheme.BlackBrown) {
        Color.Black
    } else {
        Color.White
    }
    val rightColor = if (colorScheme == OverlayColorScheme.BlackBrown) {
        Color(0xFF7A4A20)
    } else {
        Color(0xFFFFB6C1)
    }

    Canvas(modifier = modifier) {
        drawArc(
            color = leftColor,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = true
        )
        drawArc(
            color = rightColor,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = true
        )

        drawCircle(
            color = Color.Black.copy(alpha = 0.8f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
