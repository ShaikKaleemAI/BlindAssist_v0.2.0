package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class AvoidanceTest {
    @Test fun nothingSeenIsClearStateNotSpoken() {
        assertEquals("clear", Avoidance.plan(emptyList()).state)
    }

    @Test fun personDeadAheadForcesVeer() {
        val g = Avoidance.plan(listOf(trackAt("person", 2.0, 0.0)))
        assertEquals("veer", g.state); assertNotNull(g.side)
    }

    @Test fun personFarToTheSideIsNotInPath() {
        assertFalse(Avoidance.inPath(trackAt("person", 2.0, 2.0)))
    }

    @Test fun wallOfCouchesIsBlocked() {
        val wall = listOf(-1.5, 0.0, 1.5).map { trackAt("couch", 2.5, it) }
        assertEquals("blocked", Avoidance.plan(wall).state)
    }

    @Test fun objectBeyondLookaheadIgnored() {
        assertEquals("clear", Avoidance.plan(listOf(trackAt("person", 6.0, 0.0))).state)
    }
}
