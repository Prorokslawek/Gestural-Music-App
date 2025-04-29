
package com.example.gestural_music_app.userinterface

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.gestural_music_app.gesture.GestureRecognizer
import java.util.concurrent.Executors


@OptIn(UnstableApi::class)
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onGestureDetected: (GestureRecognizer.GestureType, Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val gestureRecognizer = remember {
        GestureRecognizer(
            context = context,
            listener = object : GestureRecognizer.GestureListener {
                override fun onGestureDetected(gesture: GestureRecognizer.GestureType, confidence: Float) {
                    Log.d("CameraPreview", "Gesture detected: $gesture with confidence $confidence")
                    onGestureDetected(gesture, confidence)
                }
            }
        )
    }

    DisposableEffect(key1 = context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, gestureRecognizer)
                    }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    Log.d("CameraPreview", "Camera bound to lifecycle")
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera provider is not available", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.matchParentSize()
        )
    }
}


