
//ScreenCaptureHelper
package com.example.commentanalyze

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class ScreenCaptureHelper(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var mediaProjectionManager: MediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    fun getScreenCaptureIntent(): Intent {
        return mediaProjectionManager.createScreenCaptureIntent()
    }

    fun setMediaProjection(resultCode: Int, data: Intent) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        // Register required callback for newer Android versions
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                println("MediaProjection stopped")
            }

            override fun onCapturedContentResize(width: Int, height: Int) {
                println("MediaProjection content resized: ${width}x${height}")
            }

            override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                println("MediaProjection visibility changed: $isVisible")
            }
        }, Handler(Looper.getMainLooper()))

        println("MediaProjection initialized with callback")
    }
    suspend fun captureScreen(): Bitmap? = suspendCancellableCoroutine { continuation ->
        println("=== DEBUG: Starting captureScreen ===")

        if (mediaProjection == null) {
            println("ERROR: MediaProjection is null!")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)
        var isResumed = false // Add this flag to prevent multiple resumes

        val virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null, null
        )

        imageReader.setOnImageAvailableListener({
            if (!isResumed) { // Check if already resumed
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    try {
                        val bitmap = imageToBitmap(image, width, height)
                        image.close()
                        virtualDisplay?.release()
                        isResumed = true // Mark as resumed
                        continuation.resume(bitmap)
                    } catch (e: Exception) {
                        image.close()
                        virtualDisplay?.release()
                        isResumed = true
                        continuation.resume(null)
                    }
                } else if (!isResumed) {
                    virtualDisplay?.release()
                    isResumed = true
                    continuation.resume(null)
                }
            }
        }, Handler(Looper.getMainLooper()))

        continuation.invokeOnCancellation {
            virtualDisplay?.release()
        }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }


    fun clearMediaProjection() {
        mediaProjection?.stop()
        mediaProjection = null
        println("MediaProjection cleared and stopped")
    }
    fun release() {
        mediaProjection?.stop()
    }
}