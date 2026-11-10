package com.kaleem.blindassist

class RawDet(val cls: Int, val score: Float, val box: DoubleArray)

/**
 * Decodes Ultralytics-style YOLOv8 TFLite output: [1, 4+nc, anchors] (channels-first) or [1, anchors, 4+nc],
 * boxes as normalised cx,cy,w,h, per-class scores (no objectness). YOLOv5-style output with objectness is NOT supported.
 * Pure Kotlin so it is JVM-tested; real-model compatibility is UNVERIFIED (no trained model exists yet).
 */
object YoloDecoder {
    fun decode(out: FloatArray, dim1: Int, dim2: Int, numClasses: Int, scoreThr: Float,
               imgW: Double, imgH: Double, iouThr: Double = 0.5): List<RawDet> {
        // Pick the layout by which dimension equals 4+numClasses (robust for any anchor count);
        // if ambiguous or neither matches, fall back to "channels are the smaller dimension".
        val want = 4 + numClasses
        val cf = when {
            dim1 == want && dim2 != want -> true
            dim2 == want && dim1 != want -> false
            else -> dim1 < dim2
        }
        val n = if (cf) dim2 else dim1
        val ch = if (cf) dim1 else dim2
        require(ch == 4 + numClasses) { "unexpected output channels $ch for $numClasses classes" }
        require(out.size >= n * ch) { "output buffer too small" }
        fun v(a: Int, c: Int): Float = if (cf) out[c * n + a] else out[a * ch + c]
        val cands = ArrayList<RawDet>()
        for (a in 0 until n) {
            var best = -1; var bs = scoreThr
            for (c in 0 until numClasses) { val s = v(a, 4 + c); if (s >= bs) { bs = s; best = c } }
            if (best < 0) continue
            val cx = v(a, 0).toDouble(); val cy = v(a, 1).toDouble(); val w = v(a, 2).toDouble(); val h = v(a, 3).toDouble()
            cands += RawDet(best, bs, doubleArrayOf(
                ((cx - w / 2) * imgW).coerceIn(0.0, imgW), ((cy - h / 2) * imgH).coerceIn(0.0, imgH),
                ((cx + w / 2) * imgW).coerceIn(0.0, imgW), ((cy + h / 2) * imgH).coerceIn(0.0, imgH)))
        }
        return nms(cands, iouThr)
    }

    fun nms(c: List<RawDet>, iouThr: Double): List<RawDet> {
        val kept = ArrayList<RawDet>()
        for (d in c.sortedByDescending { it.score }) if (kept.none { it.cls == d.cls && iou(it.box, d.box) > iouThr }) kept += d
        return kept
    }
}

/** Maps model class names (ml/dataset/hazard_labels.txt) to the names used by Cfg/I18n. */
object HazardLabels {
    private val map = mapOf("stairs_up" to "stairs", "stairs_down" to "stairs", "step" to "steps", "open_drain" to "open drain",
        "glass_door" to "glass door", "speed_bump" to "speed bump", "vehicle" to "car", "auto_rickshaw" to "auto rickshaw")
    fun map(raw: String) = map[raw] ?: raw.replace('_', ' ')
}
