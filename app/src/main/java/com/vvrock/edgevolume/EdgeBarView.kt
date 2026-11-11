package com.vvrock.edgevolume

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * The thin edge strip. The whole view is touchable; only a thin rounded line is drawn
 * along the outer edge. Sliding emits one step per [stepPx] of finger travel:
 * +1 = up (louder), -1 = down (quieter), 0 = tap (just shows the volume panel).
 */
@SuppressLint("ViewConstructor")
class EdgeBarView(
    context: Context,
    private val onStep: (Int) -> Unit,
) : View(context) {

    var rightSide = true
    var stepPx = 60f
    var idleAlpha = 0.2f

    private val density = resources.displayMetrics.density
    private val lineWidth = 3f * density
    private val margin = 1.5f * density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9A9A9A.toInt() }
    private val rect = RectF()

    private var active = false
    private var downY = 0f
    private var lastY = 0f
    private var accum = 0f
    private var stepped = false

    override fun onDraw(canvas: Canvas) {
        val alpha = if (active) maxOf(0.7f, idleAlpha) else idleAlpha
        if (alpha <= 0f) return
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        val left = if (rightSide) width - margin - lineWidth else margin
        rect.set(left, 0f, left + lineWidth, height.toFloat())
        canvas.drawRoundRect(rect, lineWidth / 2, lineWidth / 2, paint)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        // Ask gesture navigation not to treat this strip as the back-swipe zone.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = ev.rawY
                lastY = ev.rawY
                accum = 0f
                stepped = false
                setActive(true)
            }
            MotionEvent.ACTION_MOVE -> {
                accum += lastY - ev.rawY   // finger moving up => positive
                lastY = ev.rawY
                while (accum >= stepPx) { accum -= stepPx; step(+1) }
                while (accum <= -stepPx) { accum += stepPx; step(-1) }
            }
            MotionEvent.ACTION_UP -> {
                if (!stepped && abs(ev.rawY - downY) < touchSlop) onStep(0)
                setActive(false)
            }
            MotionEvent.ACTION_CANCEL -> setActive(false)
        }
        return true
    }

    private fun step(dir: Int) {
        stepped = true
        onStep(dir)
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun setActive(a: Boolean) {
        if (active != a) { active = a; invalidate() }
    }
}
