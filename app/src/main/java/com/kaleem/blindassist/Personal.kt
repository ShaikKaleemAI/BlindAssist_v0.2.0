package com.kaleem.blindassist

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Free-fall -> impact -> stillness fall detector on accelerometer magnitude (m/s^2).
 * Thresholds ASSUMED; false alarms (phone dropped into a pocket, sitting down hard) and misses are UNVERIFIED.
 */
class FallDetector {
    private var freeStart = -1.0
    private var lastFree = -1.0
    private var freeDur = 0.0
    private var impactT = -1.0
    private var stillSince = -1.0

    fun onAccel(t: Double, m: Double): Boolean {
        if (m < 3.0) { if (freeStart < 0) freeStart = t; lastFree = t }
        else if (freeStart >= 0) { freeDur = lastFree - freeStart; freeStart = -1.0 }
        if (impactT < 0) {
            if (m > 25.0 && freeDur >= 0.1 && lastFree >= 0 && t - lastFree <= 0.5) { impactT = t; stillSince = -1.0; freeDur = 0.0 }
            return false
        }
        if (t - impactT > 10.0) { impactT = -1.0; return false }          // never became still: not a fall
        if (t - impactT > 0.5) {
            if (abs(m - 9.81) < 1.5) {
                if (stillSince < 0) stillSince = t
                if (t - stillSince >= 3.0) { impactT = -1.0; stillSince = -1.0; return true }
            } else stillSince = -1.0
        }
        return false
    }
}

/** Countdown after a suspected fall; the user cancels (voice "stop"/button) or the SOS action fires. Action itself NOT IMPLEMENTED. */
class SosCountdown(private val seconds: Double = 15.0) {
    private var startT = -1.0
    val active get() = startT >= 0
    fun start(t: Double) { if (!active) startT = t }
    fun cancel() { startT = -1.0 }
    /** true exactly once when the countdown expires without cancel. */
    fun tick(t: Double): Boolean {
        if (!active || t - startT < seconds) return false
        startT = -1.0; return true
    }
}

enum class Verbosity { QUIET, NORMAL, DETAILED }

/** Which alert levels may be spoken. CRITICAL is never suppressed. Rate-based suggestion never auto-lowers safety. */
class AdaptiveVerbosity(var mode: Verbosity = Verbosity.NORMAL) {
    private val times = ArrayList<Double>()

    fun allows(level: Level, inPath: Boolean): Boolean = when {
        level == Level.NONE -> false
        level == Level.CRITICAL -> true
        mode == Verbosity.DETAILED -> true
        mode == Verbosity.QUIET -> level == Level.WARNING && inPath
        else -> level >= Level.WARNING
    }

    fun noteSpeech(t: Double) { times.add(t); times.removeAll { t - it > 60.0 } }
    /** Suggest (ask the user), never apply automatically. */
    fun suggestQuiet(): Boolean = mode != Verbosity.QUIET && times.size > 15
}

class Place(val name: String, val lat: Double, val lon: Double)

/** Known places, kept on-device. Serialised as name|lat|lon lines. No network, no cloud. */
class PlaceStore {
    private val places = LinkedHashMap<String, Place>()

    fun add(name: String, lat: Double, lon: Double) {
        val clean = name.replace('|', ' ').replace('\n', ' ').trim()
        require(clean.isNotEmpty()) { "empty place name" }
        places[clean.lowercase()] = Place(clean, lat, lon)
    }

    fun find(name: String): Place? = places[name.trim().lowercase()]
    fun all(): List<Place> = places.values.toList()
    fun nearest(lat: Double, lon: Double): Place? = places.values.minByOrNull { distanceM(lat, lon, it.lat, it.lon) }
    fun serialize(): String = places.values.joinToString("\n") { "${it.name}|${it.lat}|${it.lon}" }

    companion object {
        fun parse(s: String): PlaceStore {
            val st = PlaceStore()
            for (l in s.lines()) {
                val p = l.split("|"); if (p.size != 3) continue
                val la = p[1].toDoubleOrNull() ?: continue; val lo = p[2].toDoubleOrNull() ?: continue
                st.add(p[0], la, lo)
            }
            return st
        }

        fun distanceM(la1: Double, lo1: Double, la2: Double, lo2: Double): Double {
            val r = 6371000.0
            val p1 = Math.toRadians(la1); val p2 = Math.toRadians(la2)
            val dp = p2 - p1; val dl = Math.toRadians(lo2 - lo1)
            val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
            return 2 * r * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}

/** On-device daily summary text (English only for now; Hindi/Telugu TODO + NEEDS NATIVE REVIEW). */
object DailySummary {
    fun build(minutesActive: Int, alertsByLabel: Map<String, Int>): String {
        if (minutesActive <= 0) return "Blind assist was not used today."
        val top = alertsByLabel.entries.sortedByDescending { it.value }.take(3)
        val tail = if (top.isEmpty()) "No obstacle alerts." else "Most alerts: " + top.joinToString(", ") { "${it.key} ${it.value}" } + "."
        return "Used for $minutesActive minutes. $tail These are alerts only, not a record of what was safe."
    }
}
