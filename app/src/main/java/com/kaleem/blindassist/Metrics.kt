package com.kaleem.blindassist

import java.io.File

/**
 * Pure-Kotlin CSV metrics logger (JVM-testable). One row per [intervalS] seconds:
 * t_s, fps, latency_ms (mean camera-callback-to-alert-dispatch of the window), temp_c, battery_pct, limited.
 * Latency here is measured from analyzer entry, NOT from photon capture, so it under-reports true
 * camera-to-alert latency (UNVERIFIED vs the <150 ms target; see docs/SCORECARD.md).
 */
class MetricsLogger(private val file: File, private val intervalS: Double = 5.0) {
    private var windowStart = -1.0
    private var frames = 0
    private var latSum = 0.0

    init {
        if (!file.exists() || file.length() == 0L) file.writeText(HEADER + "\n")
    }

    /** Returns true if a row was written. temp/battery may be NaN / -1 when unknown. */
    fun frame(nowS: Double, latencyMs: Double, tempC: Double, batteryPct: Int, limited: Boolean): Boolean {
        if (windowStart < 0) windowStart = nowS
        frames++; latSum += latencyMs
        val span = nowS - windowStart
        if (span < intervalS) return false
        val fps = frames / span
        file.appendText(format(nowS, fps, latSum / frames, tempC, batteryPct, limited) + "\n")
        windowStart = nowS; frames = 0; latSum = 0.0
        return true
    }

    companion object {
        const val HEADER = "t_s,fps,latency_ms,temp_c,battery_pct,limited"
        fun format(t: Double, fps: Double, lat: Double, temp: Double, bat: Int, limited: Boolean) =
            String.format(java.util.Locale.US, "%.1f,%.2f,%.1f,%.1f,%d,%b", t, fps, lat, temp, bat, limited)
    }
}
