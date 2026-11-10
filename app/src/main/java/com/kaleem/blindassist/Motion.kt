package com.kaleem.blindassist

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.sqrt

enum class Orientation { UPRIGHT, UPSIDE_DOWN, FLAT, SIDEWAYS, UNKNOWN }

/** Pocket orientation + roll from the gravity vector in device axes (x right, y up, z out of screen). Axis convention ASSUMED. */
object PocketOrientation {
    fun classify(gx: Double, gy: Double, gz: Double): Orientation {
        val n = sqrt(gx * gx + gy * gy + gz * gz)
        if (n < 1.0) return Orientation.UNKNOWN
        val ux = gx / n; val uy = gy / n; val uz = gz / n
        return when {
            abs(uz) > 0.8 -> Orientation.FLAT
            uy > 0.6 -> Orientation.UPRIGHT
            uy < -0.6 -> Orientation.UPSIDE_DOWN
            abs(ux) > 0.6 -> Orientation.SIDEWAYS
            else -> Orientation.UNKNOWN
        }
    }

    /** Sideways tilt in degrees; 0 when upright. Sign convention UNVERIFIED on device. Not yet applied to the image. */
    fun rollDeg(gx: Double, gy: Double): Double = Math.toDegrees(atan2(gx, gy))
}

/** Peak-based step detector on accelerometer magnitude. Thresholds ASSUMED; tune on recorded walks. */
class StepDetector(private val minIntervalS: Double = 0.3, private val threshold: Double = 1.5) {
    private var mean = 9.81
    private var prev = 0.0
    private var prev2 = 0.0
    private var lastStepT = -1e9
    private val times = ArrayList<Double>()
    var steps = 0
        private set

    fun onAccel(t: Double, ax: Double, ay: Double, az: Double): Boolean {
        val m = sqrt(ax * ax + ay * ay + az * az)
        mean += 0.02 * (m - mean)
        val d = m - mean
        var step = false
        if (prev > threshold && prev >= prev2 && prev >= d && t - lastStepT >= minIntervalS) {
            step = true; lastStepT = t; steps++
            times.add(t); if (times.size > 8) times.removeAt(0)
        }
        prev2 = prev; prev = d
        return step
    }

    fun cadenceHz(): Double? =
        if (times.size < 3 || times.last() - times.first() <= 0.0) null else (times.size - 1) / (times.last() - times.first())
}

/** Tracks mean pitch and sway amplitude (EMA). Library only: NOT wired into range fusion (see docs/SCORECARD.md). */
class PitchSmoother(private val tauS: Double = 0.6) {
    private var m = 0.0
    private var v = 0.0
    private var t0 = 0.0
    private var init = false

    fun update(t: Double, pitchDeg: Double): Double {
        if (!init) { m = pitchDeg; t0 = t; init = true; return m }
        val dt = (t - t0).coerceAtLeast(1e-3); t0 = t
        val a = 1 - exp(-dt / tauS)
        val d = pitchDeg - m
        m += a * d
        v += a * (d * d - v)
        return m
    }

    val swayDeg: Double get() = sqrt(v)
}

/** Camera-height auto-calibration: same static ground point seen twice while walking a known forward distance. */
object HeightCalibration {
    fun heightFromPair(hfovDeg: Double, fw: Double, fh: Double, v1: Double, pitch1: Double,
                       v2: Double, pitch2: Double, walkedM: Double): Double? {
        val c = Cam(hfovDeg, 1.0, 0.0)               // unit-height camera: ground range scales linearly with height
        val z1 = c.groundZ(fw / 2, v1, fw, fh, pitch1) ?: return null
        val z2 = c.groundZ(fw / 2, v2, fw, fh, pitch2) ?: return null
        val dz = z1 - z2
        return if (dz < 0.2 || walkedM <= 0) null else walkedM / dz
    }

    fun median(xs: List<Double>): Double? {
        if (xs.isEmpty()) return null
        val s = xs.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }
}

/** Thresholds ASSUMED, not validated on any phone. */
object ThermalPolicy {
    const val WARN_C = 43.0
    fun frameSkip(tempC: Double): Int = when {
        tempC.isNaN() -> 0
        tempC < 38.0 -> 0
        tempC < 41.0 -> 1
        tempC < 44.0 -> 2
        else -> 3
    }
    fun shouldWarn(tempC: Double) = !tempC.isNaN() && tempC >= WARN_C
}
