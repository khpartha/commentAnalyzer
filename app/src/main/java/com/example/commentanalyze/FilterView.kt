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

@SuppressLint("ViewConstructor")
class FilterView(private val context: Context) : View(context) {

    private val backgroundPaint = Paint().apply {
        color = Color.argb(240, 40, 40, 40)  // Dark background
        isAntiAlias = true
    }

    private val headerPaint = Paint().apply {
        color = Color.parseColor("#8B0000")  // Dark red header
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
    }

    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val toggleOnPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")  // Green when on
        isAntiAlias = true
    }

    private val toggleOffPaint = Paint().apply {
        color = Color.parseColor("#757575")  // Gray when off
        isAntiAlias = true
    }

    private val closePaint = Paint().apply {
        color = Color.parseColor("#FF5722")  // Orange-red for close
        isAntiAlias = true
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var layoutParams: WindowManager.LayoutParams? = null

    private val viewWidth = 600f
    private val viewHeight = 500f
    private val itemHeight = 80f
    private val padding = 30f

    // Filter states - get from FilterManager
    private val filterCategories = listOf("hate", "sexual", "racist", "harassment")

    init {
        minimumWidth = viewWidth.toInt()
        minimumHeight = viewHeight.toInt()
    }

    fun showFilters() {
        // Update layout to filter size
        layoutParams?.width = viewWidth.toInt()
        layoutParams?.height = viewHeight.toInt()
        layoutParams?.let {
            windowManager.updateViewLayout(this, it)
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        val backgroundRect = RectF(0f, 0f, viewWidth, viewHeight)
        canvas.drawRoundRect(backgroundRect, 20f, 20f, backgroundPaint)

        // Draw header
        val headerRect = RectF(0f, 0f, viewWidth, 80f)
        canvas.drawRoundRect(headerRect, 20f, 20f, headerPaint)

        // Draw title
        canvas.drawText(
            "Content Filters",
            viewWidth / 2f,
            50f,
            titlePaint.apply { textAlign = Paint.Align.CENTER }
        )

        // Draw close button
        val closeRect = RectF(viewWidth - 80f, 10f, viewWidth - 10f, 70f)
        canvas.drawRoundRect(closeRect, 15f, 15f, closePaint)
        canvas.drawText(
            "✕",
            closeRect.centerX(),
            closeRect.centerY() + 12f,
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                textSize = 40f
            }
        )

        // Draw filter toggles
        drawFilterToggles(canvas)
    }

    private fun drawFilterToggles(canvas: Canvas) {
        val startY = 100f

        filterCategories.forEachIndexed { index, category ->
            val itemY = startY + (index * itemHeight)
            drawFilterItem(canvas, category, itemY)
        }
    }

    private fun drawFilterItem(canvas: Canvas, category: String, y: Float) {
        val isEnabled = FilterManager.isEnabled(category)

        // Category name
        canvas.drawText(
            category.uppercase(),
            padding + 20f,
            y + 50f,
            textPaint.apply {
                textAlign = Paint.Align.LEFT
                textSize = 36f
            }
        )

        // Toggle switch background
        val toggleRect = RectF(
            viewWidth - padding - 120f,
            y + 20f,
            viewWidth - padding - 20f,
            y + 60f
        )

        val togglePaint = if (isEnabled) toggleOnPaint else toggleOffPaint
        canvas.drawRoundRect(toggleRect, 20f, 20f, togglePaint)

        // Toggle switch circle
        val circleX = if (isEnabled) {
            toggleRect.right - 25f  // Right side when on
        } else {
            toggleRect.left + 25f   // Left side when off
        }

        canvas.drawCircle(
            circleX,
            toggleRect.centerY(),
            18f,
            Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
            }
        )

        // Status text
        val statusText = if (isEnabled) "ON" else "OFF"
        canvas.drawText(
            statusText,
            toggleRect.centerX(),
            toggleRect.centerY() + 8f,
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                textSize = 24f
                color = Color.WHITE
            }
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val x = event.x
                val y = event.y

                // Check close button
                if (x >= viewWidth - 80f && x <= viewWidth - 10f && y >= 10f && y <= 70f) {
                    closeFilters()
                    return true
                }

                // Check toggle clicks
                filterCategories.forEachIndexed { index, category ->
                    val itemY = 100f + (index * itemHeight)
                    val toggleRect = RectF(
                        viewWidth - padding - 120f,
                        itemY + 20f,
                        viewWidth - padding - 20f,
                        itemY + 60f
                    )

                    if (x >= toggleRect.left && x <= toggleRect.right &&
                        y >= toggleRect.top && y <= toggleRect.bottom) {
                        // Toggle this filter
                        FilterManager.toggle(category)
                        invalidate()
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun closeFilters() {
        // Hide this view and show the floating button again
        val overlayService = context as? OverlayService
        overlayService?.hideFilters()
    }

    fun setLayoutParams(params: WindowManager.LayoutParams) {
        this.layoutParams = params
    }
}