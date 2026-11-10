package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class TrackerTest {
    private val fw = 640.0; private val fh = 480.0

    private fun det(z: Double): Det {
        val s = 314.0 * 3.0 / z              // apparent height ~ 1/z
        val b = doubleArrayOf(280.0, 240.0 - s / 2, 360.0, 240.0 + s / 2)
        return Det("person", b, 0.9, RangeEst(z, 0.0, 0.3, 0.0, emptySet()))
    }

    @Test fun iouBasics() {
        val a = doubleArrayOf(0.0, 0.0, 10.0, 10.0)
        assertEquals(1.0, iou(a, a), 1e-9)
        assertEquals(0.0, iou(a, doubleArrayOf(20.0, 20.0, 30.0, 30.0)), 1e-9)
    }

    @Test fun sameObjectKeepsSameTrackId() {
        val tr = Tracker()
        tr.update(listOf(det(5.0)), 0.0, fw, fh)
        val first = tr.tracks.keys.single()
        tr.update(listOf(det(4.9)), 0.1, fw, fh)
        assertEquals(setOf(first), tr.tracks.keys)
    }

    @Test fun staleTracksExpire() {
        val tr = Tracker()
        tr.update(listOf(det(5.0)), 0.0, fw, fh)
        tr.update(emptyList(), 2.0, fw, fh)
        assertTrue(tr.tracks.isEmpty())
    }

    @Test fun approachingObjectGetsFiniteTtc() {
        val tr = Tracker()
        var last: List<Track> = emptyList()
        for (i in 0..12) {                    // 1.5 m/s closing, 10 Hz, from 6 m
            val t = i * 0.1
            last = tr.update(listOf(det(6.0 - 1.5 * t)), t, fw, fh)
        }
        val ttc = last.single().ttc
        assertNotNull("looming/Kalman TTC should exist", ttc)
        assertTrue("ttc=$ttc", ttc!! in 1.5..5.0)
    }

    @Test fun stationaryObjectHasNoTtc() {
        val tr = Tracker()
        var last: List<Track> = emptyList()
        for (i in 0..12) last = tr.update(listOf(det(4.0)), i * 0.1, fw, fh)
        assertNull(last.single().ttc)
    }
}
