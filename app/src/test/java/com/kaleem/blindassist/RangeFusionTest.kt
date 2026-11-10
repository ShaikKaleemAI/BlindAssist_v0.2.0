package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class RangeFusionTest {
    private val fw = 640.0; private val fh = 480.0
    private val cam = testCam

    /** Synthetic person exactly `z` m ahead on flat ground, camera 1.10 m, pitch 0. */
    private fun personBox(z: Double): DoubleArray {
        val fx = cam.fx(fw)
        val bh = fx * 1.70 / z; val bw = fx * 0.50 / z
        val y2 = fh / 2 + fx * cam.heightM / z
        return doubleArrayOf(fw / 2 - bw / 2, y2 - bh, fw / 2 + bw / 2, y2)
    }

    @Test fun cuesAgreeOnSyntheticPerson() {
        for (z in listOf(2.0, 3.0, 4.5)) {
            val e = estimateRange("person", personBox(z), fw, fh, cam, 0.0)
            assertNotNull(e)
            assertTrue("z=$z got ${e!!.z}", abs(e.z - z) / z < 0.05)
            assertEquals(0.0, e.bearing, 0.5)
        }
    }

    @Test fun unknownLabelIsNotRanged() {
        assertNull(estimateRange("tv", personBox(3.0), fw, fh, cam, 0.0))
    }

    @Test fun bottomClippedBoxIsFlaggedAndBoundOnlyIsClose() {
        val b = doubleArrayOf(200.0, 100.0, 440.0, fh)   // touches bottom edge
        val e = estimateRange("person", b, fw, fh, cam, 0.0)
        assertNotNull(e)
        assertTrue(e!!.flags.contains("clipped_bottom"))
    }

    @Test fun groundZIsNullAboveHorizon() {
        assertNull(cam.groundZ(320.0, 100.0, fw, fh, 0.0))
    }

    @Test fun clockHourWraps() {
        assertEquals(12, clockHour(0.0)); assertEquals(1, clockHour(30.0)); assertEquals(11, clockHour(-30.0))
    }
}
