package vn.edu.smartsensor

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var engine: SensorEngine
    private lateinit var body: LinearLayout
    private lateinit var status: TextView
    private var page = 0
    private var last: SensorSnapshot = SensorSnapshot()
    private var diceGame: DiceGameView? = null
    private var diceScoreText: TextView? = null
    private var diceFeedbackText: TextView? = null
    private var diceRounds = 0
    private var dicePoints = 0
    private var lastHandledShake = 0
    private var motionText: TextView? = null
    private var sensitivityText: TextView? = null
    private var drivingGame: TiltDrivingGameView? = null
    private var drivingReadout: TextView? = null
    private var gyroGame: GyroShooterView? = null
    private var gyroValues: List<TextView> = emptyList()
    private var gameScore: TextView? = null
    private var gameFeedback: TextView? = null
    private var fireButton: Button? = null
    private var gyroHorizontalSensitivity = 1f
    private var gyroVerticalSensitivity = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFFF5FAFC.toInt()
        window.navigationBarColor = 0xFFF5FAFC.toInt()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        engine = SensorEngine(this) { snapshot ->
            last = snapshot
            renderValues()
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xFFF7FBFD.toInt(), 0xFFE9F5F7.toInt()))
            setPadding(dp(20), dp(22), dp(20), dp(16))
        }
        setContentView(root)
        root.addView(label("●  SMART SENSOR", 16f, 0xFF087F7A.toInt()).apply { letterSpacing = 0.12f; typeface = Typeface.DEFAULT_BOLD })
        status = label("Đang kiểm tra cảm biến…", 12f, 0xFF536B7A.toInt())
        root.addView(status)
        val tabs = LinearLayout(this).apply { setPadding(0, dp(18), 0, dp(12)) }
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(tabs) })
        listOf("🚗  Lái xe", "⌁  Lắc", "🦆  Bắn vịt").forEachIndexed { index, title ->
            val tab = Button(this).apply {
                text = title
                textSize = 13f
                isAllCaps = false
                minWidth = 0; minHeight = 0
                setPadding(dp(16), 0, dp(16), 0)
                styleTab(this, index == page)
                setOnClickListener {
                    page = index
                    showPage()
                    refreshTabs(tabs)
                }
            }
            tabs.addView(tab, LinearLayout.LayoutParams(-2, dp(46)).apply { marginEnd = dp(8) })
        }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { isFillViewport = true; clipToPadding = false; addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        showPage()
    }

    override fun onResume() {
        super.onResume()
        engine.start()
        gyroGame?.resume()
        drivingGame?.resume()
        showStatus()
    }
    override fun onPause() { drivingGame?.pause(); gyroGame?.pause(); engine.stop(); super.onPause() }

    private fun refreshTabs(tabs: LinearLayout) {
        for (i in 0 until tabs.childCount) styleTab(tabs.getChildAt(i) as Button, i == page)
    }

    private fun showStatus() {
        status.text = when (page) {
            0 -> "Không có cảm biến đo nghiêng trên thiết bị"
            2 -> "Không có gyroscope trên thiết bị"
            else -> "Không có gia tốc kế trên thiết bị"
        }
        val supported = when (page) {
            0 -> engine.hasTiltSensor
            2 -> engine.hasGyroscope
            else -> engine.hasAccelerometer
        }
        status.visibility = if (supported) View.GONE else View.VISIBLE
    }

    private fun showPage() {
        body.removeAllViews()
        motionText = null; sensitivityText = null
        diceGame = null; diceScoreText = null; diceFeedbackText = null
        drivingGame?.pause(); drivingGame = null; drivingReadout = null
        gyroGame?.pause(); gyroGame = null; gyroValues = emptyList()
        gameScore = null; gameFeedback = null; fireButton = null
        when (page) {
            0 -> {
                body.addView(cardTitle("TILT RACER"))
                body.addView(note("Nghiêng điện thoại để đổi làn và né xe").apply { setPadding(0, 0, 0, dp(14)) })
                val hero = panel().apply {
                    setPadding(dp(8), dp(8), dp(8), dp(14))
                    background = shape(0xFF123B45.toInt(), 24)
                }
                drivingGame = TiltDrivingGameView(this).also { game ->
                    game.onScoreChanged = { score, over ->
                        drivingReadout?.text = if (over) "Va chạm!  •  $score điểm" else "$score điểm"
                    }
                    hero.addView(game, LinearLayout.LayoutParams(-1, dp(430)))
                    game.resume()
                }
                drivingReadout = label("0 điểm", 20f, Color.WHITE).apply {
                    gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD
                    setPadding(0, dp(12), 0, 0)
                }
                hero.addView(drivingReadout)
                body.addView(hero)
                body.addView(actionButton("Chơi lại  ↻") {
                    setOnClickListener { drivingGame?.restart() }
                })
            }
            1 -> {
                body.addView(cardTitle("Lắc xúc xắc"))
                val hero = panel().apply {
                    setPadding(dp(8), dp(8), dp(8), dp(14))
                    background = shape(0xFF123B45.toInt(), 24)
                }
                diceGame = DiceGameView(this).also { hero.addView(it, LinearLayout.LayoutParams(-1, dp(260))) }
                diceScoreText = label("0 điểm  •  0/5 lượt", 20f, Color.WHITE).apply {
                    gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12), 0, 0)
                }
                hero.addView(diceScoreText)
                body.addView(hero)
                diceFeedbackText = label("Mục tiêu: 35 điểm trong 5 lượt", 14f, 0xFF536B7A.toInt()).apply {
                    gravity = Gravity.CENTER; setPadding(0, dp(14), 0, 0)
                }
                body.addView(diceFeedbackText)
                motionText = label("Chuyển động: —", 16f, 0xFF173047.toInt())
                body.addView(motionText?.apply { setPadding(0, dp(18), 0, dp(4)) })
                sensitivityText = label("Ngưỡng lắc: —", 15f, 0xFF536B7A.toInt()).apply { setPadding(0, dp(14), 0, 0) }
                body.addView(sensitivityText)
                body.addView(SeekBar(this).apply {
                    max = 100
                    progress = ((engine.threshold - 2.5f) * 10).toInt()
                    progressTintList = ColorStateList.valueOf(0xFF078F85.toInt())
                    thumbTintList = ColorStateList.valueOf(0xFF078F85.toInt())
                    isEnabled = engine.hasAccelerometer
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                            engine.threshold = 2.5f + progress / 10f
                            sensitivityText?.text = "Ngưỡng lắc: ${fmt(engine.threshold)} m/s²"
                        }
                        override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                        override fun onStopTrackingTouch(bar: SeekBar?) = Unit
                    })
                })
                body.addView(actionButton("Chơi lại  ↻") { setOnClickListener { resetDiceGame() } })
                resetDiceGame()
            }
            2 -> {
                body.addView(cardTitle("Bắn vịt bằng Gyro"))
                val hero = panel().apply {
                    setPadding(dp(8), dp(8), dp(8), dp(14))
                    background = shape(0xFF123B45.toInt(), 24)
                }
                gyroGame = GyroShooterView(this).also {
                    it.setSensitivity(gyroHorizontalSensitivity, gyroVerticalSensitivity)
                    hero.addView(it, LinearLayout.LayoutParams(-1, dp(275)))
                }
                gameScore = label("0 điểm  •  0/10 lượt", 20f, Color.WHITE).apply {
                    gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12), 0, 0)
                }
                hero.addView(gameScore)
                body.addView(hero)
                gameFeedback = label("Xoay điện thoại để ngắm vịt", 14f, 0xFF536B7A.toInt()).apply {
                    gravity = Gravity.CENTER; setPadding(0, dp(12), 0, 0)
                }
                body.addView(gameFeedback)
                body.addView(gyroSensitivityControl("Ngang", gyroHorizontalSensitivity) { value ->
                    gyroHorizontalSensitivity = value
                    gyroGame?.setSensitivity(gyroHorizontalSensitivity, gyroVerticalSensitivity)
                })
                body.addView(gyroSensitivityControl("Dọc", gyroVerticalSensitivity) { value ->
                    gyroVerticalSensitivity = value
                    gyroGame?.setSensitivity(gyroHorizontalSensitivity, gyroVerticalSensitivity)
                })
                fireButton = actionButton("BẮN  ◎") {
                    isEnabled = engine.hasGyroscope
                    setOnClickListener {
                        val result = gyroGame?.shoot() ?: return@setOnClickListener
                        gameScore?.text = "${result.score} điểm  •  ${result.shots}/10 lượt"
                        gameFeedback?.text = when {
                            result.shots == 10 -> if (result.hit) "Trúng vịt! Hết lượt — chơi lại nhé" else "Hết lượt — chơi lại nhé"
                            result.hit -> "Trúng vịt!"
                            else -> "Vịt bay thoát rồi!"
                        }
                        if (result.shots >= 10) fireButton?.isEnabled = false
                    }
                }
                fireButton?.let { body.addView(it) }
                body.addView(actionButton("Chơi lại  ↻", secondary = true) {
                    setOnClickListener {
                        gyroGame?.resetGame()
                        gameScore?.text = "0 điểm  •  0/10 lượt"
                        gameFeedback?.text = "Xoay điện thoại để ngắm vịt"
                        fireButton?.isEnabled = engine.hasGyroscope
                    }
                })
                val row = metricRow(listOf("XOAY X", "XOAY Y", "XOAY Z"), listOf(0xFFD45D4D.toInt(), 0xFF087F7A.toInt(), 0xFF426DAD.toInt()))
                gyroValues = row.second
                body.addView(row.first)
                body.addView(note("Tốc độ xoay: rad/s"))
            }
        }
        showStatus()
        renderValues()
    }

    private fun renderValues() {
        if (!last.hasReading && !last.hasGyroReading) return
        drivingGame?.setTilt(last.roll, last.hasReading)
        if (diceGame != null && last.shakes > lastHandledShake) {
            lastHandledShake = last.shakes
            rollDiceGame()
        }
        motionText?.text = "Chuyển động: ${fmt(last.motion)} m/s²"
        sensitivityText?.text = "Ngưỡng lắc: ${fmt(engine.threshold)} m/s²"
        if (gyroValues.size == 3) listOf(last.wx, last.wy, last.wz).forEachIndexed { i, value ->
            gyroValues[i].text = if (engine.hasGyroscope && last.hasGyroReading) fmt2(value) else "—"
        }
        if (last.hasGyroReading) gyroGame?.onGyro(last.wx, last.wy, last.gyroTimestampNs)
    }

    private fun resetDiceGame() {
        diceRounds = 0
        dicePoints = 0
        lastHandledShake = last.shakes
        diceGame?.reset()
        diceScoreText?.text = "0 điểm  •  0/5 lượt"
        diceFeedbackText?.text = "Mục tiêu: 35 điểm trong 5 lượt"
    }

    private fun rollDiceGame() {
        if (diceRounds >= 5) return
        val first = Random.nextInt(1, 7)
        val second = Random.nextInt(1, 7)
        diceRounds++
        dicePoints += first + second
        diceGame?.roll(first, second)
        diceScoreText?.text = "$dicePoints điểm  •  $diceRounds/5 lượt"
        diceFeedbackText?.text = when {
            diceRounds == 5 && dicePoints >= 35 -> "Thắng! Bạn đạt $dicePoints điểm"
            diceRounds == 5 -> "Chưa đạt 35 điểm — chơi lại nhé"
            else -> "+${first + second} điểm  •  Lắc tiếp!"
        }
    }

    private fun cardTitle(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(14), 0, dp(18))
        addView(label(title, 25f, 0xFF173047.toInt()).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) })
    }

    private fun shape(color: Int, radius: Int, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }
    private fun panel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(16), dp(14), dp(18))
        background = shape(Color.WHITE, 24, 0xFFD7E7EB.toInt())
        elevation = dp(2).toFloat()
    }
    private fun styleTab(button: Button, selected: Boolean) {
        button.backgroundTintList = null
        button.background = shape(if (selected) 0xFFD4F2EB.toInt() else Color.WHITE, 16, if (selected) 0xFF89D4CA.toInt() else 0xFFD7E7EB.toInt())
        button.setTextColor(if (selected) 0xFF076E69.toInt() else 0xFF455E6B.toInt())
        button.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        button.elevation = if (selected) dp(3).toFloat() else 0f
    }
    private fun actionButton(text: String, secondary: Boolean = false, configure: Button.() -> Unit): Button = Button(this).apply {
        this.text = text; textSize = 15f; isAllCaps = false
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(if (secondary) 0xFF087F7A.toInt() else Color.WHITE)
        backgroundTintList = null
        background = shape(if (secondary) 0xFFE2F5F1.toInt() else 0xFF078F85.toInt(), 16)
        val params = LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) }
        layoutParams = params
        configure()
    }
    private fun metricRow(titles: List<String>, colors: List<Int>): Pair<LinearLayout, List<TextView>> {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, 0) }
        val fields = titles.mapIndexed { i, title ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(6), dp(13))
                background = shape(Color.WHITE, 18, 0xFFD7E7EB.toInt())
            }
            tile.addView(label(title, 10f, 0xFF536B7A.toInt()).apply { letterSpacing = 0.09f })
            val value = label("—", 25f, colors[i]).apply { typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); setPadding(0, dp(4), 0, 0) }
            tile.addView(value)
            row.addView(tile, LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) marginStart = dp(8) })
            value
        }
        return row to fields
    }
    private fun gyroSensitivityControl(axis: String, initial: Float, onChanged: (Float) -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(7), 0, 0)
            val valueText = label("Độ nhạy $axis: ${fmt(initial)}×", 14f, 0xFF536B7A.toInt())
            addView(valueText)
            addView(SeekBar(this@MainActivity).apply {
                max = 100
                progress = ((initial - 0.5f) / 0.015f).toInt().coerceIn(0, 100)
                progressTintList = ColorStateList.valueOf(0xFF078F85.toInt())
                thumbTintList = ColorStateList.valueOf(0xFF078F85.toInt())
                isEnabled = engine.hasGyroscope
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        val value = 0.5f + progress * 0.015f
                        valueText.text = "Độ nhạy $axis: ${fmt(value)}×"
                        onChanged(value)
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(bar: SeekBar?) = Unit
                })
            }, LinearLayout.LayoutParams(-1, dp(40)))
        }
    private fun note(text: String) = label(text, 13f, 0xFF536B7A.toInt()).apply { setPadding(0, dp(16), 0, dp(3)); setLineSpacing(dp(3).toFloat(), 1f) }
    private fun label(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }
    private fun fmt(v: Float) = String.format(Locale.US, "%.1f", v)
    private fun fmt2(v: Float) = String.format(Locale.US, "%.2f", v)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

}
