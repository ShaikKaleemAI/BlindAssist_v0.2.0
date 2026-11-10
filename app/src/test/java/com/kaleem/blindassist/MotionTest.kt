package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class MotionTest {
    @Test fun orientationClassification() {
        assertEquals(Orientation.UPRIGHT, PocketOrientation.classify(0.0, 9.8, 0.0))
        assertEquals(Orientation.UPSIDE_DOWN, PocketOrientation.classify(0.0, -9.8, 0.0))
        assertEquals(Orientation.FLAT, PocketOrientation.classify(0.0, 0.5, 9.8))
        assertEquals(Orientation.SIDEWAYS, PocketOrientation.classify(9.8, 0.0, 0.0))
        assertEquals(Orientation.UNKNOWN, PocketOrientation.classify(0.0, 0.0, 0.0))
    }

    @Test fun rollZeroWhenUpright() { assertEquals(0.0, PocketOrientation.rollDeg(0.0, 9.8), 1e-9) }

    @Test fun stepDetectorCountsSyntheticWalk() {
        val sd = StepDetector()
        for (i in 0 until 500) { val t = i / 50.0; sd.onAccel(t, 0.0, 9.81 + 3.0 * sin(2 * PI * 1.8 * t), 0.0) }
        assertTrue("steps=${sd.steps}", sd.steps in 16..20)
        assertEquals(1.8, sd.cadenceHz()!!, 0.2)
    }

    @Test fun stepDetectorIgnoresStillPhone() {
        val sd = StepDetector()
        for (i in 0 until 500) sd.onAccel(i / 50.0, 0.0, 9.81, 0.0)
        assertEquals(0, sd.steps)
    }

    @Test fun pitchSmootherTracksMeanAndSway() {
        val ps = PitchSmoother(); var m = 0.0
        for (i in 0 until 300) { val t = i / 30.0; m = ps.update(t, 5.0 + 4.0 * sin(2 * PI * 1.8 * t)) }
        assertEquals(5.0, m, 1.5)
        assertTrue("sway=${ps.swayDeg}", ps.swayDeg in 1.5..4.5)
    }

    @Test fun heightCalibrationRecoversKnownHeight() {
        val fw = 640.0; val fh = 480.0; val h = 1.10
        val c = Cam(60.0, h, 0.0); val fx = c.fx(fw)
        val v1 = fh / 2 + fx * h / 4.0; val v2 = fh / 2 + fx * h / 3.0     // same point at 4 m then 3 m
        val est = HeightCalibration.heightFromPair(60.0, fw, fh, v1, 0.0, v2, 0.0, 1.0)
        assertNotNull(est); assertTrue("est=$est", abs(est!! - h) < 0.02)
    }

    @Test fun heightCalibrationRejectsNoMotion() {
        assertNull(HeightCalibration.heightFromPair(60.0, 640.0, 480.0, 400.0, 0.0, 400.0, 0.0, 1.0))
    }

    @Test fun medianWorks() {
        assertEquals(2.0, HeightCalibration.median(listOf(3.0, 1.0, 2.0))!!, 1e-9)
        assertNull(HeightCalibration.median(emptyList()))
    }

    @Test fun thermalPolicyMonotonic() {
        assertEquals(0, ThermalPolicy.frameSkip(Double.NaN))
        assertTrue(ThermalPolicy.frameSkip(36.0) < ThermalPolicy.frameSkip(42.0))
        assertTrue(ThermalPolicy.shouldWarn(45.0)); assertFalse(ThermalPolicy.shouldWarn(30.0))
    }
}
