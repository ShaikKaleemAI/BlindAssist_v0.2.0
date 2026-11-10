package com.kaleem.blindassist

import org.junit.After
import org.junit.Assert.*
import org.junit.Assume
import org.junit.Test
import java.io.File

class ReplayTest {
    private val fw = 640.0; private val fh = 480.0; private val cam = testCam
    @After fun reset() = Cfg.resetAblation()

    /** Person approaching from 6 m to 3.2 m at 10 Hz (stays inside the frame at pocket height). */
    private fun syntheticTape(): List<TapeRow> {
        val fx = cam.fx(fw); val rows = ArrayList<TapeRow>()
        for (i in 0..20) {
            val t = i * 0.1; val z = 6.0 - 1.4 * t
            val bh = fx * 1.7 / z; val bw = fx * 0.5 / z; val y2 = fh / 2 + fx * cam.heightM / z
            rows += TapeRow(t, fw, fh, 0.0, "person", doubleArrayOf(fw / 2 - bw / 2, y2 - bh, fw / 2 + bw / 2, y2), 0.9, z)
        }
        return rows
    }

    @Test fun tapeRoundTrip() {
        val rows = syntheticTape()
        val back = Tape.parse(listOf(Tape.HEADER) + rows.map { Tape.format(it) })
        assertEquals(rows.size, back.size)
        assertEquals(rows[5].trueZ!!, back[5].trueZ!!, 1e-6)
    }

    @Test fun replayRangesApproachingPerson() {
        val r = ReplayRunner.run(syntheticTape(), 60.0, 1.10)
        assertEquals(21, r.frames)
        assertTrue(r.relErrors.isNotEmpty())
        assertTrue("mean err=${r.metrics()["range_rel_err_mean"]}", r.relErrors.average() < 0.25)
    }

    @Test fun disablingAllRangeCuesYieldsNoRanging() {
        Cfg.useWidth = false; Cfg.useHeight = false; Cfg.useGround = false
        assertTrue(ReplayRunner.run(syntheticTape(), 60.0, 1.10).relErrors.isEmpty())
    }

    @Test fun emptyFramesAreCounted() {
        val rows = (0..9).map { TapeRow(it * 0.1, fw, fh, 0.0, "", doubleArrayOf(0.0, 0.0, 0.0, 0.0), 0.0, null) }
        val r = ReplayRunner.run(rows, 60.0, 1.10)
        assertEquals(10, r.frames); assertEquals(0, r.speech)
    }

    /** Ablation entry: BA_TAPE=<tape.csv> BA_FLAGS='{"width_cue":false,...}' gradle testDebugUnitTest; writes build/ablation_metrics.txt */
    @Test fun ablationFromEnvironment() {
        val tape = System.getenv("BA_TAPE"); Assume.assumeTrue(tape != null)
        val flags = System.getenv("BA_FLAGS") ?: ""
        fun off(k: String) = flags.contains("\"$k\": false") || flags.contains("\"$k\":false")
        Cfg.useWidth = !off("width_cue"); Cfg.useHeight = !off("height_cue"); Cfg.useGround = !off("ground_cue")
        Cfg.useKalman = !off("kalman"); Cfg.useLooming = !off("looming_ttc"); Cfg.useAvoidance = !off("avoidance")
        Cfg.useHysteresis = !off("hysteresis")
        val r = ReplayRunner.run(Tape.parse(File(tape!!).readLines()), 60.0, System.getenv("BA_CAM_H")?.toDouble() ?: 1.10)
        File("build").mkdirs()
        File("build/ablation_metrics.txt").writeText(r.metrics().entries.joinToString("\n") { "METRIC ${it.key} ${it.value}" })
    }
}
