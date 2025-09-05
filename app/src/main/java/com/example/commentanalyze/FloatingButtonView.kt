package com.example.commentanalyze

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.*

@SuppressLint("ViewConstructor")
class FloatingButtonView(private val context: Context) : View(context) {

    private val circlePaint = Paint().apply {
        color = Color.parseColor("#8B0000") // Dark Red (hex code)
        alpha = 200  // transparency
        isAntiAlias = true
    }

    private val menuPaint = Paint().apply {
        color = Color.argb(200, 80, 0, 0)  // Darker red for menu
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val closePaint = Paint().apply {
        color = Color.parseColor("#FF5722")  // Orange-red for close button
        isAntiAlias = true
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var layoutParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // Menu state
    private var isMenuExpanded = false
    private val menuWidth = 300f
    private val menuHeight = 260f
    private val buttonHeight = 60f




    init {
        minimumWidth = 150
        minimumHeight = 150
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isMenuExpanded) {
            drawExpandedMenu(canvas)
        } else {
            drawCircle(canvas)
        }
    }

    private fun drawCircle(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(centerX, centerY) - 10f

        canvas.drawCircle(centerX, centerY, radius, circlePaint)
    }

    private fun drawExpandedMenu(canvas: Canvas) {
        // Draw menu background
        val menuRect = RectF(0f, 0f, menuWidth, menuHeight)
        canvas.drawRoundRect(menuRect, 20f, 20f, menuPaint)

        // Draw buttons
        val buttonWidth = menuWidth - 40f
        val startX = 20f

        // Analyze button
        val analyzeRect = RectF(startX, 20f, startX + buttonWidth, 20f + buttonHeight)
        canvas.drawRoundRect(analyzeRect, 10f, 10f, circlePaint)
        canvas.drawText("Analyze", analyzeRect.centerX(), analyzeRect.centerY() + 10f, textPaint)

        // New Analyze button (new)
        val newAnalyzeRect = RectF(startX, 90f, startX + buttonWidth, 150f)
        canvas.drawRoundRect(newAnalyzeRect, 10f, 10f, circlePaint)
        canvas.drawText("New Analyze", newAnalyzeRect.centerX(), newAnalyzeRect.centerY() + 10f, textPaint)

        // Filter button
        val filterRect = RectF(startX, 160f, startX + buttonWidth, 160f + buttonHeight)
        canvas.drawRoundRect(filterRect, 10f, 10f, circlePaint)
        canvas.drawText("Filter", filterRect.centerX(), filterRect.centerY() + 10f, textPaint)
        // X (Close) button
        val closeRect = RectF(menuWidth - 50f, 10f, menuWidth - 10f, 50f)
        canvas.drawRoundRect(closeRect, 10f, 10f, closePaint)
        canvas.drawText("✕", closeRect.centerX(), closeRect.centerY() + 10f, textPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams?.x ?: 0
                initialY = layoutParams?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isMenuExpanded) {  // Only allow dragging when menu is closed
                    val newX = initialX + (event.rawX - initialTouchX).toInt()
                    val newY = initialY + (event.rawY - initialTouchY).toInt()

                    layoutParams?.x = newX
                    layoutParams?.y = newY
                    layoutParams?.let { windowManager.updateViewLayout(this, it) }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val deltaX = kotlin.math.abs(event.rawX - initialTouchX)
                val deltaY = kotlin.math.abs(event.rawY - initialTouchY)

                if (deltaX < 10 && deltaY < 10) {
                    handleClick(event.x, event.y)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleClick(x: Float, y: Float) {
        if (isMenuExpanded) {
            // Check which button was clicked
            when {
                // Analyze button area
                x in 20f..(menuWidth - 20f) && y in 20f..80f -> {
                    onAnalyzeClicked()
                }
                // New Analyze button area
                x in 20f..(menuWidth - 20f) && y in 90f..150f -> {
                    onNewAnalyzeClicked()
                }
                // Filter button area
                x in 20f..(menuWidth - 20f) && y in 160f..220f -> {
                    onFilterClicked()
                }
                // Close button area
                x in (menuWidth - 50f)..(menuWidth - 10f) && y in 10f..50f -> {
                    onCloseClicked()
                }
                else -> {
                    collapseMenu()
                }
            }
        } else {
            // Circle clicked - expand menu
            expandMenu()
        }
    }

    private fun expandMenu() {
        isMenuExpanded = true

        // Update layout params for larger menu
        layoutParams?.width = menuWidth.toInt()
        layoutParams?.height = menuHeight.toInt()
        layoutParams?.let {
            windowManager.updateViewLayout(this, it)
        }

        invalidate()  // Redraw
    }

    private fun collapseMenu() {
        isMenuExpanded = false

        // Update layout params back to circle size
        layoutParams?.width = 150
        layoutParams?.height = 150
        layoutParams?.let {
            windowManager.updateViewLayout(this, it)
        }

        invalidate()  // Redraw
    }
    private fun onNewAnalyzeClicked() {
        println("New Analyze clicked - requesting fresh permission")
        collapseMenu()

        // Clear existing permission and request new one
        ScreenshotPermissionManager.clearPermission()

        // Also clear the MediaProjection instance
        val overlayService = context as? OverlayService
        overlayService?.clearScreenCapture()

        // Request fresh permission through MainActivity
        overlayService?.requestPermissionRenewal()
    }
    private fun onAnalyzeClicked() {
        println("=== ANALYZE CLICKED - DEBUG START ===")
        collapseMenu()

        try {
            if (!ScreenshotPermissionManager.hasPermission()) {
                println("No screenshot permission available")
                return
            }

            // Get the overlay service instance
            val overlayService = context as? OverlayService
            if (overlayService != null) {
                println("Using OverlayService for analysis...")
                overlayService.performAnalysis { toxicResults ->
                    println("=== CALLBACK RECEIVED ===")
                    // Filter results based on user preferences
                    val filteredResults = FilterManager.filterResults(toxicResults)
                    overlayService.showResults(filteredResults)
                }
            } else {
                println("ERROR: Could not get OverlayService reference")
            }

        } catch (e: Exception) {
            println("ERROR in onAnalyzeClicked: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun onFilterClicked() {
        println("Filter clicked!")
        collapseMenu()

        // Show filter view
        val overlayService = context as? OverlayService
        overlayService?.showFilters()
    }

    private fun onCloseClicked() {
        println("Close clicked - stopping service!")
        // Stop the overlay service
        val overlayService = context as? OverlayService
        overlayService?.stopSelf()
    }

    fun setLayoutParams(params: WindowManager.LayoutParams) {
        this.layoutParams = params
    }
}