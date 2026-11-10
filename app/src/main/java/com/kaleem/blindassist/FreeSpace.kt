package com.kaleem.blindassist

import kotlin.math.max

enum class ZoneState { FLAT, OBSTACLE, DROP, UNKNOWN }
class FreeSpaceResult(val left: ZoneState, val center: ZoneState, val right: ZoneState, val fitOk: Boolean)

/**
 * Ground-plane anchored free-space / drop-off check on RELATIVE inverse depth.
 * For a planar ground, inverse depth is linear in image row. Fit that line on the bottom rows (assumed ground),
 * then flag rows above where depth is nearer than the plane (OBSTACLE) or farther than it (DROP).
 * FLAT means "consistent with the fitted plane", never "safe". Tested only on synthetic data; real depth-model
 * noise, scale ambiguity and the assumption that the bottom rows are ground are all UNVERIFIED.
 */
object FreeSpace {
    private fun median(v: List<Float>): Float { val s = v.sorted(); return s[s.size / 2] }

    fun analyze(inv: Array<FloatArray>, fitFromFrac: Double = 0.8, thr: Float = 0.12f, minRows: Int = 3): FreeSpaceResult {
        val rows = inv.size
        val unknown = FreeSpaceResult(ZoneState.UNKNOWN, ZoneState.UNKNOWN, ZoneState.UNKNOWN, false)
        if (rows < 10 || inv[0].size < 3) return unknown
        val cols = inv[0].size
        val f0 = (rows * fitFromFrac).toInt().coerceIn(1, rows - 3)
        val rr = (f0 until rows).map { it.toDouble() }
        val vv = (f0 until rows).map { median(inv[it].toList()).toDouble() }
        val mr = rr.average(); val mv = vv.average()
        val sxx = rr.sumOf { (it - mr) * (it - mr) }
        if (sxx < 1e-9) return unknown
        val a = rr.indices.sumOf { (rr[it] - mr) * (vv[it] - mv) } / sxx
        val b = mv - a * mr
        if (a <= 1e-4) return unknown                                   // plane must get closer toward the bottom
        fun pred(r: Int) = a * r + b
        val zones = listOf(0 until cols / 3, cols / 3 until 2 * cols / 3, 2 * cols / 3 until cols)
        val states = zones.map { zc ->
            var obs = 0; var drop = 0
            for (r in 0 until f0) {
                if (pred(r) <= 0.05) continue                            // at/above the horizon: no ground expected
                val res = median(zc.map { inv[r][it] }) - pred(r)
                if (res > thr) obs++ else if (res < -thr) drop++
            }
            when { obs >= max(minRows, 1) -> ZoneState.OBSTACLE; drop >= max(minRows, 1) -> ZoneState.DROP; else -> ZoneState.FLAT }
        }
        return FreeSpaceResult(states[0], states[1], states[2], true)
    }
}
