package com.kaleem.blindassist

import kotlin.math.atan
import kotlin.math.tan

/** Builds a Track at a known ground position (x lateral, z forward, metres) without running the detector. */
fun trackAt(label: String, z: Double, x: Double = 0.0, t: Double = 0.0, hits: Int = 3): Track {
    val bearing = Math.toDegrees(atan(x / z))
    val est = RangeEst(z, x, 0.2, bearing, emptySet())
    val tr = Track(label, doubleArrayOf(280.0, 100.0, 360.0, 400.0), est, t, 0.9)
    tr.hits = hits
    return tr
}

val testCam get() = Cam(hfovDeg = 60.0, heightM = 1.10, defaultPitchDeg = 0.0)
