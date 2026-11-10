package com.kaleem.blindassist

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class LatLon(val lat: Double, val lon: Double)
enum class Turn { STRAIGHT, SLIGHT_LEFT, LEFT, SHARP_LEFT, SLIGHT_RIGHT, RIGHT, SHARP_RIGHT, U_TURN }

object Geo {
    const val M_PER_DEG = 6371000.0 * Math.PI / 180.0      // spherical approximation; fine at walking scale

    fun toXY(o: LatLon, p: LatLon) = ((p.lon - o.lon) * cos(Math.toRadians(o.lat)) * M_PER_DEG) to ((p.lat - o.lat) * M_PER_DEG)

    /** Compass bearing 0..360 (0 = north, 90 = east). */
    fun bearingDeg(a: LatLon, b: LatLon): Double {
        val (x, y) = toXY(a, b)
        return (Math.toDegrees(atan2(x, y)) + 360.0) % 360.0
    }

    fun turn(inB: Double, outB: Double): Turn {
        val d = ((outB - inB + 540.0) % 360.0) - 180.0         // (-180, 180], + = right
        val a = abs(d); val right = d > 0
        return when {
            a < 20 -> Turn.STRAIGHT
            a < 60 -> if (right) Turn.SLIGHT_RIGHT else Turn.SLIGHT_LEFT
            a < 120 -> if (right) Turn.RIGHT else Turn.LEFT
            a < 160 -> if (right) Turn.SHARP_RIGHT else Turn.SHARP_LEFT
            else -> Turn.U_TURN
        }
    }

    fun compass8(b: Double): String =
        listOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")[((b + 22.5) / 45.0).toInt() % 8]
}

class NavUpdate(val offRoute: Boolean, val arrived: Boolean, val remainingM: Double,
                val nextTurn: Turn?, val toTurnM: Double?, val announce: String?)

/**
 * Follows a user-recorded or user-entered waypoint route. There is NO map data and NO routing engine offline:
 * the app cannot plan a route to an arbitrary place. GPS accuracy in streets/pockets is UNVERIFIED.
 * Instructions are English only (Hindi/Telugu TODO, NEEDS NATIVE REVIEW). Never says the route is safe.
 */
class Navigator(private val route: List<LatLon>, private val offRouteM: Double = 25.0, private val arriveM: Double = 10.0) {
    private val xy = route.map { Geo.toXY(route[0], it) }
    private val cum = DoubleArray(route.size)
    private val bearings = DoubleArray(max(route.size - 1, 0))
    private var seg = 0
    private var offCount = 0
    private val announced = HashSet<Pair<Int, Int>>()
    private val thresholds = listOf(100, 50, 20)

    init {
        require(route.size >= 2) { "route needs at least 2 points" }
        for (i in 1 until route.size) {
            cum[i] = cum[i - 1] + hypot(xy[i].first - xy[i - 1].first, xy[i].second - xy[i - 1].second)
            bearings[i - 1] = Geo.bearingDeg(route[i - 1], route[i])
        }
    }

    private fun project(px: Double, py: Double, i: Int): Pair<Double, Double> {      // (cross-track m, along-route m)
        val (x1, y1) = xy[i]; val (x2, y2) = xy[i + 1]
        val dx = x2 - x1; val dy = y2 - y1; val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (((px - x1) * dx + (py - y1) * dy) / len2).coerceIn(0.0, 1.0)
        return hypot(px - (x1 + t * dx), py - (y1 + t * dy)) to (cum[i] + t * kotlin.math.sqrt(len2))
    }

    fun update(p: LatLon): NavUpdate {
        val (px, py) = Geo.toXY(route[0], p)
        val total = cum.last()
        var best = -1; var bestCross = Double.MAX_VALUE; var bestAlong = 0.0
        for (i in max(seg - 1, 0)..min(seg + 3, route.size - 2)) {          // forward-biased window avoids jumping back
            val (c, a) = project(px, py, i); if (c < bestCross) { best = i; bestCross = c; bestAlong = a }
        }
        if (bestCross > offRouteM) for (i in 0 until route.size - 1) {
            val (c, a) = project(px, py, i); if (c < bestCross) { best = i; bestCross = c; bestAlong = a }
        }
        offCount = if (bestCross > offRouteM) offCount + 1 else 0
        if (offCount >= 3) return NavUpdate(true, false, total - bestAlong, null, null,
            "You are off the route. Stop and use your cane.")
        seg = best
        val remaining = total - bestAlong
        if (remaining < arriveM) return NavUpdate(false, true, remaining, null, null, "You have arrived at the end of the route.")

        var v = -1
        for (k in seg + 1 until route.size - 1) if (Geo.turn(bearings[k - 1], bearings[k]) != Turn.STRAIGHT) { v = k; break }
        if (v < 0) return NavUpdate(false, false, remaining, null, null, null)
        val turn = Geo.turn(bearings[v - 1], bearings[v]); val toTurn = cum[v] - bestAlong
        var msg: String? = null
        val crossed = thresholds.filter { it >= toTurn && Pair(v, it) !in announced }
        if (crossed.isNotEmpty()) {
            crossed.forEach { announced += Pair(v, it) }
            msg = "In ${(toTurn / 10.0).roundToInt() * 10} metres, ${describe(turn)}."
        }
        return NavUpdate(false, false, remaining, turn, toTurn, msg)
    }

    private fun describe(t: Turn) = when (t) {
        Turn.SLIGHT_LEFT -> "bear left"; Turn.LEFT -> "turn left"; Turn.SHARP_LEFT -> "sharp left"
        Turn.SLIGHT_RIGHT -> "bear right"; Turn.RIGHT -> "turn right"; Turn.SHARP_RIGHT -> "sharp right"
        Turn.U_TURN -> "turn around"; Turn.STRAIGHT -> "continue straight"
    }
}

/** No-map fallback for "take me to <place>": straight-line bearing and distance only. Not a safe walking route. */
object DirectGuidance {
    fun describe(name: String, from: LatLon, to: LatLon): String {
        val d = PlaceStore.distanceM(from.lat, from.lon, to.lat, to.lon).roundToInt()
        return "$name is about $d metres to the ${Geo.compass8(Geo.bearingDeg(from, to))}, in a straight line. " +
            "I cannot plan a safe walking route. Use your cane."
    }
}
