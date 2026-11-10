package com.kaleem.blindassist

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/** Speech-to-text engine plug-in point. NO engine is shipped: see docs/VOICE_PLAN.md. */
interface VoiceEngine {
    val available: Boolean
    fun start(onText: (String) -> Unit)
    fun stop()
}

class NullVoice : VoiceEngine {
    override val available = false
    override fun start(onText: (String) -> Unit) {}
    override fun stop() {}
}

/** Requires a wake phrase at the start of an utterance so ordinary conversation and street noise do not trigger commands. */
object WakeWord {
    // Hindi/Telugu wake phrases are machine-drafted: NEEDS NATIVE REVIEW.
    private val words = mapOf(
        "en" to listOf("hey assist", "assist"),
        "hi" to listOf("हे असिस्ट", "असिस्ट"),
        "te" to listOf("హే అసిస్ట్", "అసిస్ట్"))

    private fun continuesWord(c: Char): Boolean =
        c.isLetterOrDigit() || Character.getType(c) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(c) == Character.COMBINING_SPACING_MARK.toInt()

    /** Text after the wake phrase, or null if the utterance does not start with one. */
    fun strip(lang: String, text: String): String? {
        val t = text.trim().lowercase()
        for (w in words[lang] ?: emptyList()) {
            if (!t.startsWith(w)) continue
            val rest = t.substring(w.length)
            if (rest.isNotEmpty() && continuesWord(rest[0])) continue       // "assistant" is not "assist"
            return rest.trimStart(',', ' ', '.', '،')
        }
        return null
    }
}

/** Accelerometer listener feeding the fall and step detectors. Thresholds ASSUMED; behaviour in a real pocket UNVERIFIED. */
class MotionMonitor(ctx: Context, private val onFall: () -> Unit) : SensorEventListener {
    private val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val fall = FallDetector()
    val steps = StepDetector()

    fun start() { sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) } }
    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        val t = e.timestamp / 1e9
        val x = e.values[0].toDouble(); val y = e.values[1].toDouble(); val z = e.values[2].toDouble()
        if (fall.onAccel(t, sqrt(x * x + y * y + z * z))) onFall()
        steps.onAccel(t, x, y, z)
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}
