package vn.edu.smartsensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class SensorSnapshot(
    val ax: Float = 0f, val ay: Float = 0f, val az: Float = 0f,
    val gx: Float = 0f, val gy: Float = 0f, val gz: Float = 0f,
    val pitch: Float = 0f, val roll: Float = 0f,
    val wx: Float = 0f, val wy: Float = 0f, val wz: Float = 0f,
    val gyroTimestampNs: Long = 0L, val hasGyroReading: Boolean = false,
    val motion: Float = 0f, val shakes: Int = 0,
    val hasReading: Boolean = false
)

class SensorEngine(context: Context, private val onUpdate: (SensorSnapshot) -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val hasAccelerometer get() = accelerometer != null
    val hasTiltSensor get() = gravity != null || accelerometer != null
    val hasGyroscope get() = gyroscope != null
    val tiltSource get() = when {
        gravity != null -> "Cảm biến trọng lực"
        accelerometer != null -> "Gia tốc kế (lọc thông thấp)"
        else -> "Không có cảm biến phù hợp"
    }

    var threshold = 5.5f
    var snapshot = SensorSnapshot()
        private set

    private val filtered = FloatArray(3)
    private var filterReady = false
    private var lastShakeNs = 0L
    private var lastUiNs = 0L
    private var armed = true
    private var active = false

    fun start() {
        if (active) return
        active = true
        // UI sampling is enough for the demo; event timestamps are still used for cooldown.
        accelerometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gravity?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        if (!active) return
        manager.unregisterListener(this)
        active = false
        filterReady = false
        armed = true
    }

    fun resetCount() {
        snapshot = snapshot.copy(shakes = 0)
        onUpdate(snapshot)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                val g = event.values
                updateGravity(g[0], g[1], g[2])
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val a = event.values
                if (!filterReady) {
                    for (i in 0..2) filtered[i] = a[i]
                    filterReady = true
                } else {
                    for (i in 0..2) filtered[i] = 0.85f * filtered[i] + 0.15f * a[i]
                }
                if (gravity == null) updateGravity(filtered[0], filtered[1], filtered[2])
                val dx = a[0] - filtered[0]
                val dy = a[1] - filtered[1]
                val dz = a[2] - filtered[2]
                val motion = sqrt(dx * dx + dy * dy + dz * dz)
                var count = snapshot.shakes
                if (motion < threshold * 0.55f) armed = true
                if (armed && motion >= threshold && event.timestamp - lastShakeNs >= 650_000_000L) {
                    count++
                    armed = false
                    lastShakeNs = event.timestamp
                }
                snapshot = snapshot.copy(ax = a[0], ay = a[1], az = a[2], motion = motion, shakes = count, hasReading = true)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val w = event.values
                snapshot = snapshot.copy(wx = w[0], wy = w[1], wz = w[2],
                    gyroTimestampNs = event.timestamp, hasGyroReading = true)
            }
        }
        if (event.timestamp - lastUiNs >= 50_000_000L) {
            lastUiNs = event.timestamp
            onUpdate(snapshot)
        }
    }

    private fun updateGravity(x: Float, y: Float, z: Float) {
        // Portrait screen is locked: x points right, y up, z out of the screen.
        val pitch = Math.toDegrees(atan2(-y.toDouble(), abs(z.toDouble()))).toFloat()
        val roll = Math.toDegrees(atan2(x.toDouble(), abs(z.toDouble()))).toFloat()
        snapshot = snapshot.copy(gx = x, gy = y, gz = z, pitch = pitch, roll = roll, hasReading = true)
    }
}
