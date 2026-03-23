/**
 * SpinWheelView.kt
 *
 * Author  : Sangita Patel
 * GitHub  : https://github.com/sangitapatel
 * License : MIT — Copyright (c) 2026 Sangita Patel
 *
 * Original work — written entirely by Sangita Patel.
 * No portion is copied or derived from any other library.
 *
 * Architecture
 * ────────────
 * • All drawing happens in onDraw() using a single off-screen
 *   Canvas (wheelBitmap) that is rebuilt only when slices/size change.
 * • Rotation is driven by a ValueAnimator with a custom
 *   DeceleratingSpinInterpolator — no third-party animation library needed.
 * • The winning slice is calculated geometrically from the final
 *   rotation angle before the listener is fired.
 */

package com.sangitapatel.spinwheel

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// SpinWheelView
// ─────────────────────────────────────────────────────────────────────────────

class SpinWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // ── Listener ──────────────────────────────────────────────────────────────

    interface OnSpinListener {
        /** Fired when the wheel starts spinning. */
        fun onSpinStart(view: SpinWheelView)

        /**
         * Fired when the wheel stops.
         * @param slice   The winning [WheelSlice].
         * @param index   Its index in the [slices] list.
         */
        fun onSpinEnd(view: SpinWheelView, slice: WheelSlice, index: Int)
    }

    // ── Public configuration ──────────────────────────────────────────────────

    /**
     * The list of slices to display. Setting this rebuilds the wheel.
     * Minimum 2 slices required.
     */
    var slices: List<WheelSlice> = emptyList()
        set(value) {
            require(value.size >= 2) { "SpinWheelView needs at least 2 slices." }
            field = value
            computeSliceAngles()
            invalidateWheelBitmap()
            invalidate()
        }

    /** Pointer (needle) color. */
    @ColorInt
    var pointerColor: Int = Color.parseColor("#E53935")
        set(value) {
            field = value; invalidate()
        }

    /** Stroke width of divider lines between slices (px). */
    var dividerWidth: Float = 2f.dp
        set(value) {
            field = value; invalidateWheelBitmap(); invalidate()
        }

    /** Divider line color. */
    @ColorInt
    var dividerColor: Int = Color.WHITE
        set(value) {
            field = value; invalidateWheelBitmap(); invalidate()
        }

    /** Outer ring stroke width (px). */
    var borderWidth: Float = 6f.dp
        set(value) {
            field = value; invalidateWheelBitmap(); invalidate()
        }

    /** Outer ring color. */
    @ColorInt
    var borderColor: Int = Color.parseColor("#37474F")
        set(value) {
            field = value; invalidateWheelBitmap(); invalidate()
        }

    /** Label text size (px). */
    var labelTextSize: Float = 14f.sp
        set(value) {
            field = value; invalidateWheelBitmap(); invalidate()
        }

    /** Default label color (overridden per-slice by [WheelSlice.textColor]). */
    @ColorInt
    var labelTextColor: Int = Color.WHITE

    /** Minimum spin duration in milliseconds. */
    var minSpinDuration: Long = 3_000L

    /** Maximum spin duration in milliseconds. */
    var maxSpinDuration: Long = 6_000L

    /** When true a single tap anywhere on the wheel triggers a spin. */
    var tapToSpin: Boolean = true

    /** When true the wheel slightly overshoots then snaps back to the winner. */
    var bounceEnabled: Boolean = true

    /** Show a circular hub at the center. */
    var showHub: Boolean = true
        set(value) {
            field = value; invalidateWheelBitmap(); invalidate()
        }

    /** Hub fill color. */
    @ColorInt
    var hubColor: Int = Color.WHITE
        set(value) {
            field = value; invalidateWheelBitmap(); invalidate()
        }

    /** Hub radius as a fraction of the wheel radius (0.05 – 0.40). */
    var hubRadiusFraction: Float = 0.12f
        set(value) {
            field = value.coerceIn(0.05f, 0.40f); invalidateWheelBitmap(); invalidate()
        }

    /** Attach your listener here. */
    var spinListener: OnSpinListener? = null

    // ── Private geometry / drawing state ─────────────────────────────────────

    // Pre-computed start angle (degrees) for each slice; index matches slices list
    private val startAngles = mutableListOf<Float>()

    // Sweep angle (degrees) for each slice
    private val sweepAngles = mutableListOf<Float>()

    // Total weight sum used to compute arc fractions
    private var totalWeight = 0f

    // Off-screen wheel bitmap — only rebuilt when slices/size/style change
    private var wheelBitmap: Bitmap? = null
    private var needRebuild = true

    // Current rotation in degrees (0 = pointer at top = 270° in standard math)
    private var currentAngle = 0f

    // Rotation animator
    private var spinAnimator: ValueAnimator? = null
    private var isSpinning = false

    private val oval = RectF()
    private val matrix = Matrix()
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG)


    private val Float.dp get() = this * context.resources.displayMetrics.density
    private val Float.sp get() = this * context.resources.displayMetrics.scaledDensity


    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        readXmlAttributes(attrs)
    }

    private fun readXmlAttributes(attrs: AttributeSet?) {
        attrs ?: return
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SpinWheelView)
        try {
            pointerColor = ta.getColor(R.styleable.SpinWheelView_swv_pointerColor, pointerColor)
            dividerWidth = ta.getDimension(R.styleable.SpinWheelView_swv_dividerWidth, dividerWidth)
            dividerColor = ta.getColor(R.styleable.SpinWheelView_swv_dividerColor, dividerColor)
            borderWidth = ta.getDimension(R.styleable.SpinWheelView_swv_borderWidth, borderWidth)
            borderColor = ta.getColor(R.styleable.SpinWheelView_swv_borderColor, borderColor)
            labelTextSize =
                ta.getDimension(R.styleable.SpinWheelView_swv_labelTextSize, labelTextSize)
            labelTextColor =
                ta.getColor(R.styleable.SpinWheelView_swv_labelTextColor, labelTextColor)
            minSpinDuration =
                ta.getInt(R.styleable.SpinWheelView_swv_minSpinDuration, minSpinDuration.toInt())
                    .toLong()
            maxSpinDuration =
                ta.getInt(R.styleable.SpinWheelView_swv_maxSpinDuration, maxSpinDuration.toInt())
                    .toLong()
            tapToSpin = ta.getBoolean(R.styleable.SpinWheelView_swv_tapToSpin, tapToSpin)
            bounceEnabled =
                ta.getBoolean(R.styleable.SpinWheelView_swv_bounceEnabled, bounceEnabled)
            showHub = ta.getBoolean(R.styleable.SpinWheelView_swv_showHub, showHub)
            hubColor = ta.getColor(R.styleable.SpinWheelView_swv_hubColor, hubColor)
            hubRadiusFraction =
                ta.getFloat(R.styleable.SpinWheelView_swv_hubRadiusFraction, hubRadiusFraction)
        } finally {
            ta.recycle()
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Always square
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        invalidateWheelBitmap()
    }


    private fun computeSliceAngles() {
        startAngles.clear()
        sweepAngles.clear()
        if (slices.isEmpty()) return
        totalWeight = slices.sumOf { it.weight.toDouble() }.toFloat()
        var cursor = 0f
        for (slice in slices) {
            val sweep = 360f * slice.weight / totalWeight
            startAngles.add(cursor)
            sweepAngles.add(sweep)
            cursor += sweep
        }
    }


    private fun invalidateWheelBitmap() {
        needRebuild = true
        wheelBitmap?.recycle()
        wheelBitmap = null
    }

    private fun rebuildWheelBitmap() {
        val w = width;
        val h = height
        if (w <= 0 || h <= 0 || slices.isEmpty()) return

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val cx = w / 2f
        val cy = h / 2f
        val inset = borderWidth + 2f.dp
        val radius = min(cx, cy) - inset
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)

        for (i in slices.indices) {
            arcPaint.color = slices[i].fillColor
            arcPaint.style = Paint.Style.FILL
            // -90f so first slice starts at the top (pointer position)
            c.drawArc(oval, startAngles[i] - 90f, sweepAngles[i], true, arcPaint)
        }

        // ── Divider lines ─────────────────────────────────────────────────────
        divPaint.color = dividerColor
        divPaint.strokeWidth = dividerWidth
        for (i in slices.indices) {
            val rad = Math.toRadians((startAngles[i] - 90f).toDouble())
            c.drawLine(
                cx, cy,
                (cx + radius * cos(rad)).toFloat(),
                (cy + radius * sin(rad)).toFloat(),
                divPaint
            )
        }

        txtPaint.textSize = labelTextSize
        for (i in slices.indices) {
            val slice = slices[i]
            val midAngle = Math.toRadians((startAngles[i] + sweepAngles[i] / 2f - 90f).toDouble())
            val labelR = radius * 0.65f   // 65 % from center
            val lx = (cx + labelR * cos(midAngle)).toFloat()
            val ly = (cy + labelR * sin(midAngle)).toFloat()

            c.save()
            c.rotate((startAngles[i] + sweepAngles[i] / 2f), lx, ly)
            txtPaint.color = slice.textColor
            // Vertical centering offset
            val textHeight = txtPaint.descent() - txtPaint.ascent()
            c.drawText(slice.label, lx, ly + textHeight / 2f - txtPaint.descent(), txtPaint)
            c.restore()
        }

        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidth
        c.drawCircle(cx, cy, radius + borderWidth / 2f, borderPaint)

        if (showHub) {
            val hubR = radius * hubRadiusFraction
            hubPaint.color = hubColor
            c.drawCircle(cx, cy, hubR, hubPaint)
            hubRingPaint.color = borderColor
            hubRingPaint.strokeWidth = dividerWidth
            c.drawCircle(cx, cy, hubR, hubRingPaint)
        }

        wheelBitmap = bmp
        needRebuild = false
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty()) return

        if (needRebuild) rebuildWheelBitmap()
        val bmp = wheelBitmap ?: return

        val cx = width / 2f
        val cy = height / 2f

        // Rotate the cached bitmap around center
        matrix.reset()
        matrix.postRotate(currentAngle, cx, cy)
        canvas.drawBitmap(bmp, matrix, null)

        // Draw pointer (never rotates)
        drawPointer(canvas, cx)
    }

    private fun drawPointer2(canvas: Canvas, cx: Float) {
        val pointerH = 28f.dp
        val pointerW = 14f.dp
        val top = 0f

        val path = Path().apply {
            moveTo(cx, top)                              // tip
            lineTo(cx - pointerW / 2f, top + pointerH)  // bottom-left
            lineTo(cx + pointerW / 2f, top + pointerH)  // bottom-right
            close()
        }

        pointerPaint.color = pointerColor
        pointerPaint.style = Paint.Style.FILL
        canvas.drawPath(path, pointerPaint)

        // Thin white outline for contrast
        pointerPaint.color = Color.WHITE
        pointerPaint.style = Paint.Style.STROKE
        pointerPaint.strokeWidth = 1.5f.dp
        canvas.drawPath(path, pointerPaint)
    }
    private fun drawPointer(canvas: Canvas, cx: Float) {
        val pointerH = 28f.dp
        val pointerW = 14f.dp
        val top = 0f                           // base ઉપર (top edge)
        val tipY = pointerH                    // tip નીચે (wheel તરફ point કરે)

        val path = Path().apply {
            moveTo(cx, tipY)                              // tip — નીચે, wheel તરફ
            lineTo(cx - pointerW / 2f, top)               // base-left — ઉપર
            lineTo(cx + pointerW / 2f, top)               // base-right — ઉપર
            close()
        }

        pointerPaint.color = pointerColor
        pointerPaint.style = Paint.Style.FILL
        canvas.drawPath(path, pointerPaint)

        pointerPaint.color = Color.WHITE
        pointerPaint.style = Paint.Style.STROKE
        pointerPaint.strokeWidth = 1.5f.dp
        canvas.drawPath(path, pointerPaint)
    }
    private fun drawPointer1(canvas: Canvas, cx: Float) {
        val pointerH = 28f.dp
        val pointerW = 14f.dp
        val bottom = height.toFloat()          // wheel ની bottom edge
        val top    = bottom - pointerH         // base ઉપર, tip નીચે

        val path = Path().apply {
            moveTo(cx, bottom)                 // ← tip: નીચે
            lineTo(cx - pointerW / 2f, top)   // ← base-left: ઉપર
            lineTo(cx + pointerW / 2f, top)   // ← base-right: ઉપર
            close()
        }

        pointerPaint.color = pointerColor
        pointerPaint.style = Paint.Style.FILL
        canvas.drawPath(path, pointerPaint)

        pointerPaint.color = Color.WHITE
        pointerPaint.style = Paint.Style.STROKE
        pointerPaint.strokeWidth = 1.5f.dp
        canvas.drawPath(path, pointerPaint)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!tapToSpin || isSpinning) return false
        if (event.action == MotionEvent.ACTION_UP) {
            val cx = width / 2f
            val cy = height / 2f
            val dx = event.x - cx
            val dy = event.y - cy
            val dist = sqrt(dx * dx + dy * dy)
            val radius = min(cx, cy)
            if (dist <= radius) {
                spin()
                return true
            }
        }
        return super.onTouchEvent(event)
    }


    /**
     * Start spinning. Safe to call from any thread.
     * No-op if already spinning.
     *
     * @param targetIndex Optional index to guarantee a specific winner (0-based).
     *                    Pass -1 (default) for a random result.
     */
    fun spin(targetIndex: Int = -1) {
        if (isSpinning || slices.isEmpty()) return

        val winnerIdx = when {
            targetIndex in slices.indices -> targetIndex
            else -> pickWeightedRandom()
        }

        // How many full rotations + alignment to winner slice centre
        val fullTurns = Random.nextInt(5, 10) * 360f
        val winnerMid = startAngles[winnerIdx] + sweepAngles[winnerIdx] / 2f
        // Target: the winner slice centre aligns with pointer (top = 0°)
        val alignOffset = (360f - winnerMid) - (currentAngle % 360f)
        val totalAngle = fullTurns + ((alignOffset % 360f + 360f) % 360f)

        val duration = Random.nextLong(minSpinDuration, maxSpinDuration)
        val fromAngle = currentAngle
        val toAngle = fromAngle + totalAngle

        spinAnimator?.cancel()
        spinAnimator = ValueAnimator.ofFloat(fromAngle, toAngle).apply {
            this.duration = duration
            interpolator = DeceleratingSpinInterpolator()
            addUpdateListener { anim ->
                currentAngle = anim.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    isSpinning = true
                    spinListener?.onSpinStart(this@SpinWheelView)
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (bounceEnabled) {
                        playBounce(winnerIdx)
                    } else {
                        currentAngle = toAngle % 360f
                        finishSpin(winnerIdx)
                    }
                }
            })
            start()
        }
    }

    private fun playBounce(winnerIdx: Int) {
        val base = currentAngle
        val overshoot = 8f   // degrees past the target
        val bounce = ValueAnimator.ofFloat(0f, overshoot, 0f).apply {
            duration = 350L
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { anim ->
                currentAngle = base + anim.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentAngle = base % 360f
                    finishSpin(winnerIdx)
                }
            })
        }
        bounce.start()
    }

    private fun finishSpin(winnerIdx: Int) {
        isSpinning = false
        spinListener?.onSpinEnd(this, slices[winnerIdx], winnerIdx)
    }

    /**
     * Pick a random winner respecting slice weights.
     */
    private fun pickWeightedRandom(): Int {
        val r = Random.nextFloat() * totalWeight
        var acc = 0f
        for (i in slices.indices) {
            acc += slices[i].weight
            if (r <= acc) return i
        }
        return slices.lastIndex
    }


    /** True while the wheel is animating. */
    val isCurrentlySpinning: Boolean get() = isSpinning

    /** Immediately stop the wheel (no winner callback). */
    fun stopNow() {
        spinAnimator?.cancel()
        isSpinning = false
    }

    /** Reset wheel to its initial position (angle = 0). */
    fun resetAngle() {
        if (isSpinning) return
        currentAngle = 0f
        invalidate()
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        spinAnimator?.cancel()
        wheelBitmap?.recycle()
        wheelBitmap = null
    }


    /**
     * DeceleratingSpinInterpolator
     *
     * Starts fast, decelerates smoothly to a stop.
     * Uses a custom cubic-ease-out curve tuned for spin-wheel feel:
     *   f(t) = 1 - (1 - t)^3  blended with a linear warmup for the first 5 %.
     *
     * This gives a natural "coin-spin" feel without any spring overshoot
     * (the bounce is handled separately if enabled).
     */
    private class DeceleratingSpinInterpolator : android.animation.TimeInterpolator {
        override fun getInterpolation(t: Float): Float {
            return when {
                t < 0.05f -> t * 4f       // quick linear warmup (avoids sudden jerk at t=0)
                else -> {
                    val u = 1f - t
                    1f - u * u * u         // cubic ease-out
                }
            }
        }
    }
}
