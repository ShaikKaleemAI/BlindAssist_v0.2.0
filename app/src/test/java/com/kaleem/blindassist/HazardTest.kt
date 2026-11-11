package com.kaleem.blindassist

import org.junit.Assert.*
import org.junit.Test

class HazardTest {
    /** channels-first [4+nc, n]: build anchors with (cx,cy,w,h,scores...) normalised. */
    private fun cf(anchors: List<FloatArray>, nc: Int): FloatArray {
        val n = anchors.size; val out = FloatArray((4 + nc) * n)
        for ((a, v) in anchors.withIndex()) for (c in v.indices) out[c * n + a] = v[c]
        return out
    }

    @Test fun decodesAndScalesBox() {
        val out = cf(listOf(floatArrayOf(0.5f, 0.5f, 0.2f, 0.4f, 0.1f, 0.9f), floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f)), 2)
        val d = YoloDecoder.decode(out, 6, 2, 2, 0.5f, 640.0, 480.0)
        assertEquals(1, d.size); assertEquals(1, d[0].cls)
        assertArrayEquals(doubleArrayOf(256.0, 144.0, 384.0, 336.0), d[0].box, 1e-3)
    }

    @Test fun channelsLastLayoutGivesSameResult() {
        val a = listOf(floatArrayOf(0.5f, 0.5f, 0.2f, 0.4f, 0.1f, 0.9f), floatArrayOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f))
        val flat = FloatArray(12); for ((i, v) in a.withIndex()) for (c in v.indices) flat[i * 6 + c] = v[c]
        val d = YoloDecoder.decode(flat, 2, 6, 2, 0.5f, 640.0, 480.0)
        assertEquals(1, d.size); assertEquals(1, d[0].cls)
    }

    @Test fun nmsSuppressesOverlapSameClassOnly() {
        val a = floatArrayOf(0.5f, 0.5f, 0.2f, 0.4f, 0.9f, 0.0f); val b = floatArrayOf(0.51f, 0.5f, 0.2f, 0.4f, 0.8f, 0.0f)
        val c = floatArrayOf(0.5f, 0.5f, 0.2f, 0.4f, 0.0f, 0.85f)
        val d = YoloDecoder.decode(cf(listOf(a, b, c), 2), 6, 3, 2, 0.5f, 100.0, 100.0)
        assertEquals(2, d.size); assertEquals(setOf(0, 1), d.map { it.cls }.toSet())
    }

    @Test fun belowThresholdDropped() {
        val out = cf(listOf(floatArrayOf(0.5f, 0.5f, 0.2f, 0.4f, 0.3f, 0.3f)), 2)
        assertTrue(YoloDecoder.decode(out, 6, 1, 2, 0.5f, 100.0, 100.0).isEmpty())
    }

    @Test fun wrongChannelCountRejected() {
        try { YoloDecoder.decode(FloatArray(60), 6, 10, 3, 0.5f, 1.0, 1.0); fail() } catch (e: IllegalArgumentException) { }
    }

    @Test fun labelMapping() {
        assertEquals("stairs", HazardLabels.map("stairs_down")); assertEquals("open drain", HazardLabels.map("open_drain"))
        assertEquals("car", HazardLabels.map("vehicle")); assertEquals("person", HazardLabels.map("person"))
        assertEquals("some thing", HazardLabels.map("some_thing"))
    }

    @Test fun everyModelClassIsAlertableAndLocalised() {
        for (raw in listOf("stairs", "stairs_up", "stairs_down", "step", "pothole", "open_drain", "manhole", "curb", "pole", "glass_door",
            "speed_bump", "vehicle", "cow", "auto_rickshaw", "person")) {
            val l = HazardLabels.map(raw)
            assertTrue("$l not in NAV", l in Cfg.NAV)
            assertTrue("$l not in GROUNDED", l in Cfg.GROUNDED)
            for (lang in listOf("hi", "te")) assertNotEquals("$lang label for $l missing", l, I18n.label(lang, l))
        }
    }

    @Test fun potholeRangedFromGroundCueAlone() {
        val cam = testCam; val fw = 640.0; val fh = 480.0
        val y2 = fh / 2 + cam.fx(fw) * cam.heightM / 3.0
        val e = estimateRange("pothole", doubleArrayOf(280.0, y2 - 20, 360.0, y2), fw, fh, cam, 0.0)
        assertNotNull(e); assertEquals(3.0, e!!.z, 0.4)
    }
}
