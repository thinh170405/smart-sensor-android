package vn.edu.smartsensor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

data class ShotResult(val score: Int, val shots: Int, val hit: Boolean)

/** A small target game: gyro rates move the reticle; the button fires. */
class GyroShooterView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var aimX = 0.5f
    private var aimY = 0.5f
    private var smoothX = 0f
    private var smoothY = 0f
    private var lastGyroNs = 0L
    private var targetPhase = 0f
    private var score = 0
    private var shots = 0
    private var lastShotMs = 0L
    private var lastHit = false
    private var running = true
    private var horizontalSensitivity = 1f
    private var verticalSensitivity = 1f
    private var skyGradient: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        skyGradient = LinearGradient(0f, 0f, 0f, h.toFloat(),
            0xFF6BC9E4.toInt(), 0xFFE2F7DC.toInt(), Shader.TileMode.CLAMP)
    }

    fun setSensitivity(horizontal: Float, vertical: Float) {
        horizontalSensitivity = horizontal.coerceIn(0.5f, 2f)
        verticalSensitivity = vertical.coerceIn(0.5f, 2f)
    }

    fun onGyro(wx: Float, wy: Float, timestampNs: Long) {
        if (timestampNs == 0L || timestampNs == lastGyroNs) return
        val dt = if (lastGyroNs == 0L) 0f else
            ((timestampNs - lastGyroNs) / 1_000_000_000f).coerceIn(0f, 0.12f)
        lastGyroNs = timestampNs
        smoothX = smoothX * 0.65f + wx * 0.35f
        smoothY = smoothY * 0.65f + wy * 0.35f
        if (abs(smoothY) > 0.015f) aimX = (aimX - smoothY * dt * 0.78f * horizontalSensitivity).coerceIn(0.08f, 0.92f)
        if (abs(smoothX) > 0.015f) aimY = (aimY + smoothX * dt * 0.78f * verticalSensitivity).coerceIn(0.10f, 0.90f)
        invalidate()
    }

    fun shoot(): ShotResult {
        if (shots >= 10) return ShotResult(score, shots, false)
        shots++
        val target = targetPosition(SystemClock.uptimeMillis())
        val distance = hypot(aimX * width - target.first, aimY * height - target.second)
        lastHit = distance <= min(width, height) * 0.14f
        if (lastHit) {
            score++
            targetPhase += 2.25f
        }
        lastShotMs = SystemClock.uptimeMillis()
        invalidate()
        return ShotResult(score, shots, lastHit)
    }

    fun resetGame() {
        aimX = 0.5f; aimY = 0.5f
        smoothX = 0f; smoothY = 0f
        lastGyroNs = 0L
        score = 0; shots = 0; targetPhase = 0f
        lastShotMs = 0L
        invalidate()
    }

    fun pause() { running = false }
    fun resume() { running = true; postInvalidateOnAnimation() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = skyGradient
        canvas.drawRoundRect(0f, 0f, w, h, dp(18f), dp(18f), paint)
        paint.shader = null
        paint.color = 0xFFFFE7A2.toInt()
        canvas.drawCircle(w * 0.84f, h * 0.19f, min(w, h) * 0.10f, paint)
        drawCloud(canvas, w * 0.16f, h * 0.17f, w * 0.12f)
        drawCloud(canvas, w * 0.62f, h * 0.32f, w * 0.09f)
        paint.color = 0xFF89BC79.toInt()
        canvas.drawOval(-w * 0.12f, h * 0.70f, w * 0.58f, h * 1.12f, paint)
        paint.color = 0xFF5C9C69.toInt()
        canvas.drawOval(w * 0.32f, h * 0.67f, w * 1.12f, h * 1.18f, paint)
        paint.color = 0xFF377D58.toInt()
        canvas.drawRect(0f, h * 0.86f, w, h, paint)
        for (i in 0..8) {
            val bx = w * (i + 0.3f) / 9f
            paint.color = 0xFF275F4C.toInt()
            canvas.drawCircle(bx, h * 0.87f, w * 0.045f, paint)
        }

        val target = targetPosition(SystemClock.uptimeMillis())
        drawDuck(canvas, target.first, target.second, min(w, h) * 0.16f,
            sin(SystemClock.uptimeMillis() / 125f), sin(SystemClock.uptimeMillis() / 1000f * 0.72f + targetPhase) >= 0f)

        val ax = aimX * w
        val ay = aimY * h
        val flash = SystemClock.uptimeMillis() - lastShotMs < 180L
        val reticleColor = if (flash && lastHit) 0xFF41E58D.toInt() else 0xFF173D43.toInt()
        paint.color = 0x55FFFFFF
        paint.style = Paint.Style.FILL
        canvas.drawCircle(ax, ay, dp(if (flash) 30f else 26f), paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(if (flash) 3.5f else 2.5f)
        paint.color = reticleColor
        canvas.drawCircle(ax, ay, dp(if (flash) 24f else 20f), paint)
        val gap = dp(25f)
        val outer = dp(37f)
        canvas.drawLine(ax - outer, ay, ax - gap, ay, paint)
        canvas.drawLine(ax + gap, ay, ax + outer, ay, paint)
        canvas.drawLine(ax, ay - outer, ax, ay - gap, paint)
        canvas.drawLine(ax, ay + gap, ax, ay + outer, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(ax, ay, dp(3f), paint)
        paint.color = 0xFF174E59.toInt()
        paint.textSize = dp(11f)
        paint.isFakeBoldText = true
        canvas.drawText("DUCK HUNT  •  GYRO", dp(20f), dp(26f), paint)
        paint.isFakeBoldText = false

        if (running && isAttachedToWindow) postInvalidateOnAnimation()
    }

    private fun targetPosition(timeMs: Long): Pair<Float, Float> {
        val t = timeMs / 1000f
        return Pair(width * (0.5f + 0.32f * sin(t * 0.72f + targetPhase)),
            height * (0.42f + 0.19f * sin(t * 0.51f + targetPhase * 0.7f)))
    }

    private fun drawCloud(canvas: Canvas, x: Float, y: Float, size: Float) {
        paint.style = Paint.Style.FILL
        paint.color = 0xCCFFFFFF.toInt()
        canvas.drawOval(x - size, y, x + size, y + size * 0.35f, paint)
        canvas.drawCircle(x - size * 0.35f, y, size * 0.38f, paint)
        canvas.drawCircle(x + size * 0.22f, y - size * 0.08f, size * 0.48f, paint)
    }

    private fun drawDuck(canvas: Canvas, x: Float, y: Float, size: Float, flap: Float, facingRight: Boolean) {
        canvas.save()
        canvas.translate(x, y)
        if (!facingRight) canvas.scale(-1f, 1f)
        canvas.scale(size / 100f, size / 100f)
        paint.style = Paint.Style.FILL
        paint.color = 0x553A5961
        canvas.drawOval(-43f, 29f, 48f, 48f, paint)
        paint.color = 0xFF513C33.toInt()
        val tail = Path().apply { moveTo(-37f, 3f); lineTo(-63f, -11f); lineTo(-54f, 15f); close() }
        canvas.drawPath(tail, paint)
        paint.color = 0xFFD7A05D.toInt()
        canvas.drawOval(-43f, -23f, 44f, 31f, paint)
        paint.color = 0xFF8F633D.toInt()
        val wing = Path().apply {
            moveTo(-21f, -11f); quadTo(-49f, -40f - flap * 25f, -8f, -37f - flap * 25f)
            quadTo(23f, -24f, 16f, 6f); close()
        }
        canvas.drawPath(wing, paint)
        paint.color = 0xFF317E68.toInt()
        canvas.drawCircle(34f, -25f, 22f, paint)
        paint.color = 0xFFE9D7A8.toInt()
        canvas.drawOval(19f, -24f, 49f, -7f, paint)
        paint.color = 0xFFFFBE4D.toInt()
        val beak = Path().apply { moveTo(50f, -20f); lineTo(77f, -12f); lineTo(49f, -7f); close() }
        canvas.drawPath(beak, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(42f, -31f, 6f, paint)
        paint.color = 0xFF17313A.toInt()
        canvas.drawCircle(44f, -31f, 3f, paint)
        canvas.restore()
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
