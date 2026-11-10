package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MetricsTest {
    @Test fun writesHeaderAndRowsEveryWindow() {
        val f = File.createTempFile("metrics", ".csv"); f.deleteOnExit(); f.delete()
        val m = MetricsLogger(f, 1.0)
        var wrote = 0
        for (i in 0..20) if (m.frame(i * 0.1, 50.0, 30.0, 80, false)) wrote++
        val lines = f.readLines()
        assertEquals(MetricsLogger.HEADER, lines[0])
        assertEquals(wrote, lines.size - 1)
        assertTrue(wrote >= 1)
    }
}
