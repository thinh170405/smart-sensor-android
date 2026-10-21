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
import kotlin.math.min
import kotlin.random.Random

/** Small driving game controlled by the phone's gravity vector. */
class TiltDrivingGameView(context: Context) : View(context) {
    private data class TrafficCar(var lane: Int, var y: Float, val color: Int)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val traffic = mutableListOf<TrafficCar>()
    private val otherColors = intArrayOf(0xFFFFB84D.toInt(), 0xFF62C5D2.toInt(), 0xFFF0777F.toInt())
    private var targetX = 0f
    private var carX = 0f
    private var roadOffset = 0f
    private var elapsed = 0f
    private var spawnTimer = 0f
    private var lastFrameNs = 0L
    private var active = false
    private var hasSensor = false
    private var gameOver = false
    private var score = 0
    private var grassGradient: LinearGradient? = null
    private var roadGradient: LinearGradient? = null

    var onScoreChanged: ((Int, Boolean) -> Unit)? = null

    fun setTilt(roll: Float, available: Boolean) {
        hasSensor = available
        targetX = (roll / 24f).coerceIn(-1f, 1f)
    }

    fun restart() {
        traffic.clear()
        targetX = 0f
        carX = 0f
        roadOffset = 0f
        elapsed = 0f
        spawnTimer = 0f
        lastFrameNs = 0L
        gameOver = false
        score = 0
        onScoreChanged?.invoke(score, false)
        if (active) postInvalidateOnAnimation()
    }

    fun resume() {
        active = true
        lastFrameNs = 0L
        postInvalidateOnAnimation()
    }

    fun pause() {
        active = false
        lastFrameNs = 0L
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        grassGradient = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(),
            0xFF4A9B78.toInt(), 0xFF1F685F.toInt(), Shader.TileMode.CLAMP)
        roadGradient = LinearGradient(0f, 0f, w.toFloat(), 0f,
            0xFF1C3041.toInt(), 0xFF354C5B.toInt(), Shader.TileMode.CLAMP)
    }

    override fun onDetachedFromWindow() {
        pause()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.elapsedRealtimeNanos()
        val dt = if (lastFrameNs == 0L) 0f else ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNs = now
        if (active && !gameOver && hasSensor) update(dt)
        drawScene(canvas)
        if (active && !gameOver) postInvalidateOnAnimation()
    }

    private fun update(dt: Float) {
        carX += (targetX - carX) * min(1f, dt * 8f)
        val speed = 0.35f + min(0.38f, elapsed * 0.008f)
        roadOffset = (roadOffset + dt * speed) % 0.25f
        elapsed += dt
        val nextScore = (elapsed * 10).toInt()
        if (nextScore != score) {
            score = nextScore
            onScoreChanged?.invoke(score, false)
        }
        spawnTimer += dt
        val interval = (1.35f - elapsed * 0.006f).coerceAtLeast(0.75f)
        if (spawnTimer >= interval) {
            spawnTimer = 0f
            traffic += TrafficCar(Random.nextInt(3), -0.18f, otherColors.random())
        }
        for (car in traffic) car.y += dt * speed
        traffic.removeAll { it.y > 1.18f }
        val playerLaneX = carX * 0.31f
        if (traffic.any { abs(it.y - 0.79f) < 0.115f && abs(laneX(it.lane) - playerLaneX) < 0.20f }) {
            gameOver = true
            onScoreChanged?.invoke(score, true)
        }
    }

    private fun laneX(lane: Int) = (lane - 1) * 0.31f

    private fun drawScene(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val roadLeft = w * 0.105f
        val roadRight = w * 0.895f
        paint.shader = grassGradient
        canvas.drawRoundRect(0f, 0f, w, h, 26f, 26f, paint)
        paint.shader = null
        paint.color = 0xFF182E3D.toInt()
        canvas.drawRect(roadLeft - w * 0.025f, 0f, roadRight + w * 0.025f, h, paint)
        paint.shader = roadGradient
        canvas.drawRect(roadLeft, 0f, roadRight, h, paint)
        paint.shader = null
        paint.color = 0xFFF0D589.toInt()
        val edge = w * 0.008f
        canvas.drawRect(roadLeft + edge, 0f, roadLeft + edge * 2f, h, paint)
        canvas.drawRect(roadRight - edge * 2f, 0f, roadRight - edge, h, paint)
        paint.color = 0x99E8F7F6.toInt()
        val stripeWidth = w * 0.007f
        val stripeHeight = h * 0.09f
        for (divider in 1..2) {
            val x = roadLeft + (roadRight - roadLeft) * divider / 3f
            for (i in -1..5) {
                val y = h * (i * 0.25f + roadOffset)
                canvas.drawRoundRect(x - stripeWidth, y, x + stripeWidth, y + stripeHeight, stripeWidth, stripeWidth, paint)
            }
        }
        for (i in -1..8) {
            val y = h * (i * 0.17f + roadOffset)
            paint.color = 0xFFF8D898.toInt()
            canvas.drawRoundRect(w * 0.04f, y, w * 0.065f, y + h * 0.045f, 4f, 4f, paint)
            canvas.drawRoundRect(w * 0.935f, y, w * 0.96f, y + h * 0.045f, 4f, 4f, paint)
            paint.color = 0xFF165951.toInt()
            canvas.drawCircle(w * 0.055f, y + h * 0.105f, w * 0.022f, paint)
            canvas.drawCircle(w * 0.945f, y + h * 0.105f, w * 0.022f, paint)
        }
        for (car in traffic) {
            val cx = w * (0.5f + laneX(car.lane))
            drawCar(canvas, cx, h * car.y, w * 0.15f, h * 0.115f, car.color, false)
        }
        drawCar(canvas, w * (0.5f + carX * 0.31f), h * 0.79f, w * 0.18f, h * 0.145f, 0xFF1EC3AD.toInt(), true)
        paint.color = 0xDD0B2735.toInt()
        canvas.drawRoundRect(w * 0.31f, h * 0.025f, w * 0.69f, h * 0.095f, 14f, 14f, paint)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        paint.textSize = w * 0.035f
        canvas.drawText("DODGE TRAFFIC", w / 2f, h * 0.071f, paint)
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        if (!hasSensor || gameOver) {
            paint.color = 0xCC173047.toInt()
            canvas.drawRoundRect(w * 0.13f, h * 0.38f, w * 0.87f, h * 0.60f, 22f, 22f, paint)
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.isFakeBoldText = true
            paint.textSize = w * 0.06f
            canvas.drawText(if (gameOver) "VA CHẠM!" else "CHỜ CẢM BIẾN", w / 2f, h * 0.475f, paint)
            paint.isFakeBoldText = false
            paint.textSize = w * 0.035f
            canvas.drawText(if (gameOver) "Bấm Chơi lại để thử tiếp" else "Nghiêng điện thoại để lái", w / 2f, h * 0.535f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawCar(canvas: Canvas, cx: Float, cy: Float, carWidth: Float, carHeight: Float, color: Int, player: Boolean) {
        canvas.save()
        canvas.translate(cx - carWidth / 2f, cy - carHeight / 2f)
        canvas.scale(carWidth / 100f, carHeight / 160f)

        paint.shader = null
        paint.color = 0x66071925
        canvas.drawOval(1f, 10f, 99f, 166f, paint)
        paint.color = 0xFF122431.toInt()
        for (y in floatArrayOf(26f, 113f)) {
            canvas.drawRoundRect(-5f, y, 16f, y + 28f, 7f, 7f, paint)
            canvas.drawRoundRect(84f, y, 105f, y + 28f, 7f, 7f, paint)
        }
        if (player) {
            paint.color = 0xFF0D3540.toInt()
            canvas.drawRoundRect(7f, 142f, 93f, 152f, 4f, 4f, paint)
            canvas.drawRoundRect(14f, 134f, 22f, 151f, 3f, 3f, paint)
            canvas.drawRoundRect(78f, 134f, 86f, 151f, 3f, 3f, paint)
        }

        val body = Path().apply {
            moveTo(28f, 5f); lineTo(72f, 5f)
            cubicTo(83f, 5f, 91f, 15f, 93f, 29f)
            lineTo(97f, 126f); cubicTo(97f, 141f, 86f, 151f, 72f, 152f)
            lineTo(28f, 152f); cubicTo(14f, 151f, 3f, 141f, 3f, 126f)
            lineTo(7f, 29f); cubicTo(9f, 15f, 17f, 5f, 28f, 5f); close()
        }
        paint.shader = LinearGradient(0f, 0f, 100f, 145f,
            if (player) 0xFF91FCE0.toInt() else Color.WHITE, color, Shader.TileMode.CLAMP)
        canvas.drawPath(body, paint)
        paint.shader = null
        paint.color = 0x44FFFFFF
        canvas.drawRoundRect(14f, 20f, 21f, 130f, 4f, 4f, paint)
        paint.color = 0x33071C2D
        canvas.drawRoundRect(80f, 20f, 87f, 130f, 4f, 4f, paint)

        paint.color = 0xFF14394A.toInt()
        canvas.drawRoundRect(18f, 48f, 82f, 106f, 13f, 13f, paint)
        paint.shader = LinearGradient(20f, 48f, 77f, 101f,
            0xFFBDF3FA.toInt(), 0xFF367891.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(23f, 52f, 77f, 82f, 8f, 8f, paint)
        paint.shader = null
        paint.color = 0xFFE4FBFF.toInt()
        canvas.drawRoundRect(27f, 55f, 36f, 79f, 4f, 4f, paint)
        paint.color = 0xFF25566A.toInt()
        canvas.drawRoundRect(24f, 89f, 76f, 102f, 5f, 5f, paint)
        paint.color = 0x99FFFFFF.toInt()
        canvas.drawRoundRect(34f, 119f, 66f, 123f, 2f, 2f, paint)

        paint.color = 0xFFFFF0BD.toInt()
        canvas.drawRoundRect(11f, 13f, 30f, 24f, 4f, 4f, paint)
        canvas.drawRoundRect(70f, 13f, 89f, 24f, 4f, 4f, paint)
        paint.color = 0xFFFE626A.toInt()
        canvas.drawRoundRect(10f, 132f, 31f, 143f, 3f, 3f, paint)
        canvas.drawRoundRect(69f, 132f, 90f, 143f, 3f, 3f, paint)
        paint.color = 0xFF102D3A.toInt()
        canvas.drawRoundRect(31f, 140f, 69f, 146f, 3f, 3f, paint)
        canvas.restore()
    }
}
