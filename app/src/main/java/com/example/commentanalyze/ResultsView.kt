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
class ResultsView(private val context: Context) : View(context) {

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
        textSize = 28f
        isAntiAlias = true
    }

    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val typePaint = Paint().apply {
        color = Color.parseColor("#FF6B6B")  // Light red for type
        textSize = 24f
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val closePaint = Paint().apply {
        color = Color.parseColor("#FF5722")  // Orange-red for close
        isAntiAlias = true
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var layoutParams: WindowManager.LayoutParams? = null

    private var toxicResults: List<ToxicText> = emptyList()
    private var scrollOffset = 0f
    private val itemHeight = 180f
    private val padding = 30f
    private var lastTouchY = 0f

    private val viewWidth = 600f
    private val viewHeight = 800f

    init {
        minimumWidth = viewWidth.toInt()
        minimumHeight = viewHeight.toInt()
    }

    fun showResults(results: List<ToxicText>) {
        this.toxicResults = results
        this.scrollOffset = 0f

        // Update layout to results size
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
            "Toxic Comments Found: ${toxicResults.size}",
            viewWidth / 2f,
            50f,
            titlePaint.apply { textAlign = Paint.Align.CENTER }
        )

        // Draw close button
        val closeRect = RectF(viewWidth - 60f, 10f, viewWidth - 10f, 60f)
        canvas.drawRoundRect(closeRect, 15f, 15f, closePaint)
        canvas.drawText(
            "✕",
            closeRect.centerX(),
            closeRect.centerY() + 10f,
            textPaint.apply { textAlign = Paint.Align.CENTER }
        )

        // Draw results
        if (toxicResults.isEmpty()) {
            canvas.drawText(
                "No toxic content detected",
                viewWidth / 2f,
                viewHeight / 2f,
                textPaint.apply { textAlign = Paint.Align.CENTER }
            )
        } else {
            drawToxicComments(canvas)
        }
    }

    private fun drawToxicComments(canvas: Canvas) {
        val startY = 100f + scrollOffset

        toxicResults.forEachIndexed { index, toxic ->
            val itemY = startY + (index * itemHeight)

            // Only draw items that are visible
            if (itemY > -itemHeight && itemY < viewHeight) {
                drawToxicItem(canvas, toxic, itemY)
            }
        }
    }

    private fun drawToxicItem(canvas: Canvas, toxic: ToxicText, y: Float) {
        val itemRect = RectF(padding, y, viewWidth - padding, y + itemHeight - 10f)

        // Item background
        val itemPaint = Paint().apply {
            color = Color.argb(100, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawRoundRect(itemRect, 10f, 10f, itemPaint)

        // Type badge
        val typeRect = RectF(padding + 10f, y + 10f, padding + 80f, y + 35f)
        canvas.drawRoundRect(typeRect, 5f, 5f, typePaint)

        canvas.drawText(
            toxic.type.uppercase(),
            typeRect.centerX(),
            typeRect.centerY() + 8f,
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                textSize = 20f
            }
        )

        // Confidence
        canvas.drawText(
            "${(toxic.confidence * 100).toInt()}%",
            viewWidth - padding - 40f,
            y + 30f,
            textPaint.apply {
                textAlign = Paint.Align.CENTER
                textSize = 24f
            }
        )

        // Comment text (wrapped)
        val textStartX = padding + 10f
        val textStartY = y + 55f
        val maxWidth = viewWidth - padding * 2 - 20f

        drawWrappedText(
            canvas,
            toxic.text,
            textStartX,
            textStartY,
            maxWidth,
            textPaint.apply {
                textAlign = Paint.Align.LEFT
                textSize = 26f
            }
        )
    }

    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        // Limit to 25 words and add "..." if longer
        val words = text.split(" ")
        val limitedText = if (words.size > 25) {
            words.take(25).joinToString(" ") + "..."
        } else {
            text
        }

        val wordsToDisplay = limitedText.split(" ")
        var currentLine = ""
        var currentY = y
        val lineHeight = 30f

        for (word in wordsToDisplay) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentLine = word
                currentY += lineHeight
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - lastTouchY
                scrollOffset += deltaY

                // Limit scrolling bounds
                val maxScroll = 0f
                val minScroll = -(toxicResults.size * itemHeight - (viewHeight - 120f))

                scrollOffset = scrollOffset.coerceIn(minScroll, maxScroll)

                lastTouchY = event.y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val x = event.x
                val y = event.y

                // Check close button
                if (x >= viewWidth - 100f && x <= viewWidth - 15f && y >= 15f && y <= 85f) {
                    closeResults()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun closeResults() {
        // Hide this view and show the floating button again
        val overlayService = context as? OverlayService
        overlayService?.hideResults()
    }

    fun setLayoutParams(params: WindowManager.LayoutParams) {
        this.layoutParams = params
    }
}