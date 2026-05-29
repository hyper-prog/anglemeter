package com.anglemeter.app.camera

import android.content.Context
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class CameraPreviewController(
    private val context: Context
) {

    private var cameraProvider: ProcessCameraProvider? = null

    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int,
        mirrorFrontPreview: Boolean = true,
        onError: ((String) -> Unit)? = null
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    cameraProvider = providerFuture.get()
                    bindPreview(lifecycleOwner, previewView, lensFacing, mirrorFrontPreview)
                } catch (exception: Exception) {
                    Log.e(TAG, "Failed to bind camera preview", exception)
                    onError?.invoke(exception.message ?: "Unknown camera error")
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun stopPreview() {
        cameraProvider?.unbindAll()
    }

    private fun bindPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int,
        mirrorFrontPreview: Boolean
    ) {
        val provider = cameraProvider ?: return

        val previewBuilder = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)

        if (mirrorFrontPreview) {
            previewBuilder.setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
        }

        val preview = previewBuilder.build().also { createdPreview ->
            createdPreview.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
    }

    companion object {
        private const val TAG = "CameraPreviewController"
    }
}
