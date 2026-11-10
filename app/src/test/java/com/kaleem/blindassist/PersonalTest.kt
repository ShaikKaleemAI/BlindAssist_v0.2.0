package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PersonalTest {
    private fun feed(fd: FallDetector, from: Double, to: Double, f: (Double) -> Double): Int {
        var n = 0; var t = from
        while (t < to) { if (fd.onAccel(t, f(t))) n++; t += 0.02 }
        return n
    }

    @Test fun detectsFreefallImpactStillness() {
        val fd = FallDetector(); var fires = 0
        fires += feed(fd, 0.0, 1.0) { 9.81 }
        fires += feed(fd, 1.0, 1.3) { 1.0 }          // free fall
        fires += feed(fd, 1.3, 1.34) { 40.0 }        // impact
        fires += feed(fd, 1.34, 8.0) { 9.81 }        // lying still
        assertEquals(1, fires)
    }

    @Test fun walkingIsNotAFall() {
        val fd = FallDetector()
        assertEquals(0, feed(fd, 0.0, 30.0) { 9.81 + 3.0 * sin(2 * PI * 1.8 * it) })
    }

    @Test fun impactWithoutStillnessIsNotAFall() {
        val fd = FallDetector(); var fires = 0
        fires += feed(fd, 0.0, 1.0) { 9.81 }
        fires += feed(fd, 1.0, 1.3) { 1.0 }
        fires += feed(fd, 1.3, 1.34) { 40.0 }
        fires += feed(fd, 1.34, 20.0) { 9.81 + 3.0 * sin(2 * PI * 1.8 * it) }   // keeps moving
        assertEquals(0, fires)
    }

    @Test fun sosCountdownCancelAndExpire() {
        val s = SosCountdown(15.0); s.start(0.0)
        assertFalse(s.tick(10.0)); s.cancel(); assertFalse(s.tick(20.0))
        s.start(100.0); assertTrue(s.tick(115.5)); assertFalse(s.tick(120.0))
    }

    @Test fun criticalAlwaysAllowedInEveryMode() {
        for (m in Verbosity.values()) assertTrue(AdaptiveVerbosity(m).allows(Level.CRITICAL, false))
    }

    @Test fun verbosityLevels() {
        val q = AdaptiveVerbosity(Verbosity.QUIET)
        assertTrue(q.allows(Level.WARNING, true)); assertFalse(q.allows(Level.WARNING, false)); assertFalse(q.allows(Level.INFO, true))
        val n = AdaptiveVerbosity(Verbosity.NORMAL)
        assertTrue(n.allows(Level.WARNING, false)); assertFalse(n.allows(Level.INFO, true))
        assertTrue(AdaptiveVerbosity(Verbosity.DETAILED).allows(Level.INFO, false))
    }

    @Test fun suggestsQuietButNeverAutoApplies() {
        val v = AdaptiveVerbosity(Verbosity.NORMAL)
        for (i in 0..20) v.noteSpeech(i * 2.0)
        assertTrue(v.suggestQuiet()); assertEquals(Verbosity.NORMAL, v.mode)
    }

    @Test fun placeStoreRoundTripAndNearest() {
        val s = PlaceStore(); s.add("Home", 17.3850, 78.4867); s.add("Bus stop", 17.3900, 78.4900)
        val back = PlaceStore.parse(s.serialize())
        assertEquals(2, back.all().size); assertNotNull(back.find("home"))
        assertEquals("Home", back.nearest(17.3851, 78.4868)!!.name)
    }

    @Test fun haversineMatchesKnownScale() {
        val d = PlaceStore.distanceM(0.0, 0.0, 0.001, 0.0)      // 0.001 deg latitude ~ 111 m
        assertTrue("d=$d", abs(d - 111.2) < 2.0)
    }

    @Test fun placeNameSanitised() {
        val s = PlaceStore(); s.add("a|b\nc", 1.0, 2.0)
        assertEquals(1, PlaceStore.parse(s.serialize()).all().size)
    }

    @Test fun dailySummaryDisclaimsSafety() {
        val t = DailySummary.build(30, mapOf("person" to 5, "car" to 2))
        assertTrue(t.contains("person 5")); assertFalse(t.lowercase().contains("path is clear"))
        assertEquals("Blind assist was not used today.", DailySummary.build(0, emptyMap()))
    }
}
