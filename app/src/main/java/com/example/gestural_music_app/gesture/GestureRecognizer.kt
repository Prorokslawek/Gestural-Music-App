package com.example.gestural_music_app.gesture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer as MPGRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import java.util.concurrent.Executors
import kotlinx.coroutines.*

class GestureRecognizer(
    private val context: Context,
    private val listener: GestureListener
) : ImageAnalysis.Analyzer {

    interface GestureListener {
        fun onGestureDetected(gesture: GestureType, confidence: Float)
    }

    enum class GestureType {
        THUMB_UP, THUMB_DOWN, OPEN_PALM, CLOSED_FIST, POINTING_UP, VICTORY, NONE,ILOVEYOU
    }

    private var lastGesture: GestureType = GestureType.NONE
    private var lastConfidence: Float = 0f

    private var recognizer: MPGRecognizer? = null

    init {
        val options = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("gesture_recognizer.task")
                    .build()
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                handleResult(result)
            }
            .build()
        recognizer = MPGRecognizer.createFromOptions(context, options)
    }

    override fun analyze(image: ImageProxy) {
        try {
            val bitmap = imageProxyToBitmap(image)
            val rotatedBitmap = rotateAndScaleBitmap(bitmap, image.imageInfo.rotationDegrees)
            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            recognizer?.recognizeAsync(mpImage, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("GestureRecognizer", "Błąd podczas analizy gestu: ${e.message}", e)
        } finally {
            image.close()
        }
    }

    private fun handleResult(result: GestureRecognizerResult?) {
        val gesture = result?.gestures()?.firstOrNull()?.firstOrNull()
        val gestureType = gesture?.categoryName()?.let { mapToGestureType(it) } ?: GestureType.NONE
        val confidence = gesture?.score() ?: 0f

        // Wywołuj listener zawsze, nie tylko przy zmianie gestu
        if (confidence > 0.7f) {
            listener.onGestureDetected(gestureType, confidence)
        } else {
            listener.onGestureDetected(GestureType.NONE, 0f)
        }
        lastGesture = gestureType
        lastConfidence = confidence
    }


    private fun mapToGestureType(categoryName: String): GestureType =
        when (categoryName.lowercase()) {
            "thumb_up" -> GestureType.THUMB_UP
            "thumb_down" -> GestureType.THUMB_DOWN
            "open_palm" -> GestureType.OPEN_PALM
            "closed_fist" -> GestureType.CLOSED_FIST
            "pointing_up" -> GestureType.POINTING_UP
            "victory" -> GestureType.VICTORY
            "iloveyou" -> GestureType.ILOVEYOU
            else -> GestureType.NONE
        }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null
        )
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        val yuv = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(yuv, 0, yuv.size)
    }

    private fun rotateAndScaleBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        // Skalowanie do 256x256 px (optymalne dla MediaPipe, możesz zmienić)
        val scale = 256f / bitmap.width.coerceAtLeast(bitmap.height)
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }

    fun close() {
        recognizer?.close()
    }
}

