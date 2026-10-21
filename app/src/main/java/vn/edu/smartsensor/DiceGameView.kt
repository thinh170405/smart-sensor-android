package vn.edu.smartsensor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import kotlin.math.min
import kotlin.math.sin

/** Two animated dice driven by recognized shake events. */
class DiceGameView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var first = 1
    private var second = 1
    private var rollStartMs = 0L
    private var tableGradient: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tableGradient = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
            0xFF26796E.toInt(), 0xFF103E4C.toInt(), Shader.TileMode.CLAMP)
    }

    fun roll(a: Int, b: Int) {
        first = a.coerceIn(1, 6)
        second = b.coerceIn(1, 6)
        rollStartMs = SystemClock.uptimeMillis()
        postInvalidateOnAnimation()
    }

    fun reset() {
        first = 1; second = 1; rollStartMs = 0L
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = tableGradient
        canvas.drawRoundRect(0f, 0f, w, h, dp(18f), dp(18f), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2f)
        paint.color = 0x55C7F7E9
        canvas.drawRoundRect(dp(10f), dp(10f), w - dp(10f), h - dp(10f), dp(14f), dp(14f), paint)
        paint.style = Paint.Style.FILL
        paint.color = 0x335CF4D0
        canvas.drawOval(w * 0.11f, h * 0.56f, w * 0.89f, h * 0.86f, paint)
        paint.color = 0xFFFFDA88.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        paint.textSize = dp(12f)
        canvas.drawText("SHAKE TO ROLL", w / 2f, h * 0.16f, paint)
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT

        val elapsed = SystemClock.uptimeMillis() - rollStartMs
        val animating = rollStartMs > 0L && elapsed < 650L
        val progress = (elapsed / 650f).coerceIn(0f, 1f)
        val side = min(w * 0.30f, h * 0.46f)
        val faceA = if (animating) 1 + ((elapsed / 65L).toInt() % 6) else first
        val faceB = if (animating) 1 + (((elapsed / 65L).toInt() + 3) % 6) else second
        val wobble = if (animating) (1f - progress) * 15f else 0f
        drawDie(canvas, w * 0.32f, h * 0.51f, side, faceA, wobble * sin(elapsed / 65f))
        drawDie(canvas, w * 0.68f, h * 0.51f, side, faceB, -wobble * sin(elapsed / 72f))
        if (animating && isAttachedToWindow) postInvalidateOnAnimation()
    }

    private fun drawDie(canvas: Canvas, cx: Float, cy: Float, side: Float, face: Int, rotation: Float) {
        val saved = canvas.save()
        canvas.rotate(rotation, cx, cy)
        val half = side / 2f
        paint.style = Paint.Style.FILL
        paint.color = 0x99061F2B.toInt()
        canvas.drawOval(cx - half + dp(3f), cy + half - dp(5f),
            cx + half + dp(10f), cy + half + dp(14f), paint)
        paint.shader = LinearGradient(cx - half, cy - half, cx + half, cy + half,
            Color.WHITE, 0xFFD7E8E9.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, dp(16f), dp(16f), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(2f)
        paint.color = 0xFFB5D4D7.toInt()
        canvas.drawRoundRect(cx - half, cy - half, cx + half, cy + half, dp(16f), dp(16f), paint)
        paint.style = Paint.Style.FILL
        paint.color = 0x33FFFFFF
        canvas.drawRoundRect(cx - half + dp(7f), cy - half + dp(6f),
            cx + half - dp(7f), cy - half + dp(12f), dp(4f), dp(4f), paint)
        val o = side * 0.25f
        val positions = when (face) {
            1 -> listOf(0f to 0f)
            2 -> listOf(-o to -o, o to o)
            3 -> listOf(-o to -o, 0f to 0f, o to o)
            4 -> listOf(-o to -o, o to -o, -o to o, o to o)
            5 -> listOf(-o to -o, o to -o, 0f to 0f, -o to o, o to o)
            else -> listOf(-o to -o, o to -o, -o to 0f, o to 0f, -o to o, o to o)
        }
        positions.forEach { (x, y) ->
            paint.color = 0xFF173E50.toInt()
            canvas.drawCircle(cx + x, cy + y, side * 0.072f, paint)
            paint.color = 0xFF0C9E96.toInt()
            canvas.drawCircle(cx + x - side * 0.008f, cy + y - side * 0.008f, side * 0.052f, paint)
        }
        canvas.restoreToCount(saved)
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
