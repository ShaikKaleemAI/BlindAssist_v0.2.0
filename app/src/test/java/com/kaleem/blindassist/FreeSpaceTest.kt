package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class FreeSpaceTest {
    private val rows = 100; private val cols = 60
    private fun plane() = Array(rows) { r -> FloatArray(cols) { if (r < 30) 0f else (r - 30) / 70f } }

    @Test fun flatGroundIsFlat() {
        val r = FreeSpace.analyze(plane())
        assertTrue(r.fitOk); assertEquals(ZoneState.FLAT, r.left); assertEquals(ZoneState.FLAT, r.center); assertEquals(ZoneState.FLAT, r.right)
    }

    @Test fun nearerBlobInCenterIsObstacle() {
        val g = plane(); for (r in 50..70) for (c in 22..38) g[r][c] += 0.3f
        val r = FreeSpace.analyze(g)
        assertEquals(ZoneState.OBSTACLE, r.center); assertEquals(ZoneState.FLAT, r.left); assertEquals(ZoneState.FLAT, r.right)
    }

    @Test fun fartherPatchIsDropOff() {
        val g = plane(); for (r in 45..60) for (c in 22..38) g[r][c] -= 0.2f
        assertEquals(ZoneState.DROP, FreeSpace.analyze(g).center)
    }

    @Test fun degenerateInputIsUnknownNotFlat() {
        assertEquals(ZoneState.UNKNOWN, FreeSpace.analyze(Array(100) { FloatArray(60) { 0.5f } }).center)
        assertFalse(FreeSpace.analyze(Array(5) { FloatArray(5) }).fitOk)
    }
}
