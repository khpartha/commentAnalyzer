package com.example.commentanalyze

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: FloatingButtonView? = null
    private var resultsView: ResultsView? = null
    private var analysisService: AnalysisService? = null


    private var filterView: FilterView? = null

    fun showFilters() {
        println("OverlayService: showFilters called")

        // Hide floating button
        floatingView?.let {
            windowManager?.removeView(it)
        }

        // Create and show filter view
        if (filterView == null) {
            filterView = FilterView(this)

            val layoutParams = WindowManager.LayoutParams().apply {
                width = 600
                height = 500
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.CENTER
            }

            filterView?.setLayoutParams(layoutParams)
            windowManager?.addView(filterView, layoutParams)
        }

        filterView?.showFilters()
    }

    fun hideFilters() {
        println("OverlayService: hideFilters called")

        // Hide filter view
        filterView?.let {
            windowManager?.removeView(it)
            filterView = null
        }

        // Show floating button again
        createFloatingView()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "overlay_service_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Start as foreground service with media projection type
        createNotificationChannel()
        startForegroundService()

        createFloatingView()

        // Initialize analysis service
        analysisService = AnalysisService(this)

        // Initialize MediaProjection if permission exists
        val resultCode = ScreenshotPermissionManager.getResultCode()
        val data = ScreenshotPermissionManager.getData()
        if (resultCode != null && data != null) {
            analysisService?.initializeMediaProjection(resultCode, data)
            println("OverlayService: MediaProjection initialized")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Comment Analyzer Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Floating comment analyzer overlay"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Comment Analyzer Active")
            .setContentText("Tap the red circle to analyze comments")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    fun performAnalysis(callback: (List<ToxicText>) -> Unit) {
        println("OverlayService: performAnalysis called")
        analysisService?.analyzeScreen(
            context = this,
            onResult = callback,
            onPermissionNeeded = {
                // Request permission renewal
                requestPermissionRenewal()
            }
        )
    }

      fun requestPermissionRenewal() {
        println("OverlayService: Requesting permission renewal")
        // Send intent to MainActivity to request permission again
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("REQUEST_PERMISSION", true)
        }
        startActivity(intent)
    }

    fun showResults(toxicResults: List<ToxicText>) {
        println("OverlayService: showResults called with ${toxicResults.size} results")

        // Hide floating button
        floatingView?.let {
            windowManager?.removeView(it)
        }

        // Create and show results view
        if (resultsView == null) {
            resultsView = ResultsView(this)

            val layoutParams = WindowManager.LayoutParams().apply {
                width = 800
                height = 1200
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.CENTER
            }

            resultsView?.setLayoutParams(layoutParams)
            windowManager?.addView(resultsView, layoutParams)
        }

        // Show the results
        resultsView?.showResults(toxicResults)
    }

    fun hideResults() {
        println("OverlayService: hideResults called")

        // Hide results view
        resultsView?.let {
            windowManager?.removeView(it)
            resultsView = null
        }

        // Show floating button again
        createFloatingView()
    }

    private fun createFloatingView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = FloatingButtonView(this)

        val layoutParams = WindowManager.LayoutParams().apply {
            width = 150
            height = 150
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        floatingView?.setLayoutParams(layoutParams)
        windowManager?.addView(floatingView, layoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            windowManager?.removeView(it)
        }
        resultsView?.let {
            windowManager?.removeView(it)
        }
        analysisService?.cleanup()
    }

    fun clearScreenCapture() {
        analysisService?.clearScreenCapture()
    }
}