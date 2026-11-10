package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class NavigationTest {
    private val m = Geo.M_PER_DEG
    // route: 200 m east, then 200 m north (a LEFT turn), starting on the equator
    private val route = listOf(LatLon(0.0, 0.0), LatLon(0.0, 200 / m), LatLon(200 / m, 200 / m))
    private fun at(east: Double, north: Double) = LatLon(north / m, east / m)

    @Test fun bearingsAndTurns() {
        assertEquals(90.0, Geo.bearingDeg(route[0], route[1]), 0.5)
        assertEquals(0.0, Geo.bearingDeg(route[1], route[2]), 0.5)
        assertEquals(Turn.LEFT, Geo.turn(90.0, 0.0)); assertEquals(Turn.RIGHT, Geo.turn(0.0, 90.0))
        assertEquals(Turn.STRAIGHT, Geo.turn(10.0, 15.0)); assertEquals(Turn.U_TURN, Geo.turn(0.0, 180.0))
        assertEquals(Turn.SLIGHT_RIGHT, Geo.turn(350.0, 20.0))
    }

    @Test fun startReportsNextTurnWithoutAnnouncement() {
        val u = Navigator(route).update(at(0.0, 0.0))
        assertEquals(Turn.LEFT, u.nextTurn); assertEquals(200.0, u.toTurnM!!, 3.0)
        assertNull(u.announce); assertFalse(u.offRoute)
    }

    @Test fun announcesOncePerThreshold() {
        val n = Navigator(route)
        n.update(at(0.0, 0.0))
        val a = n.update(at(140.0, 0.0)); assertNotNull(a.announce); assertTrue(a.announce!!.contains("turn left"))
        assertNull(n.update(at(145.0, 0.0)).announce)
        assertNotNull(n.update(at(155.0, 0.0)).announce)            // 50 m threshold
    }

    @Test fun offRouteNeedsThreeConsecutiveFixes() {
        val n = Navigator(route); n.update(at(50.0, 0.0))
        assertFalse(n.update(at(60.0, -100.0)).offRoute); assertFalse(n.update(at(61.0, -100.0)).offRoute)
        val u = n.update(at(62.0, -100.0)); assertTrue(u.offRoute); assertTrue(u.announce!!.contains("cane"))
    }

    @Test fun goodFixResetsOffRouteCounter() {
        val n = Navigator(route)
        n.update(at(60.0, -100.0)); n.update(at(61.0, -100.0)); n.update(at(62.0, 2.0))
        assertFalse(n.update(at(63.0, -100.0)).offRoute)
    }

    @Test fun arrivesAtEnd() {
        val n = Navigator(route); n.update(at(100.0, 0.0)); n.update(at(200.0, 100.0))
        val u = n.update(at(200.0, 195.0)); assertTrue(u.arrived)
    }

    @Test fun routeNeedsTwoPoints() {
        try { Navigator(listOf(LatLon(0.0, 0.0))); fail() } catch (e: IllegalArgumentException) { }
    }

    @Test fun directGuidanceNeverClaimsSafeRoute() {
        val s = DirectGuidance.describe("Home", LatLon(0.0, 0.0), LatLon(0.0, 200 / m))
        assertTrue(s.contains("east")); assertTrue(s.contains("200")); assertTrue(s.contains("cannot plan a safe"))
        assertEquals("north-east", Geo.compass8(45.0)); assertEquals("north", Geo.compass8(359.0))
    }
}
