package com.kaleem.blindassist

/** One detector output row of a recorded walk. A row with an empty label marks a frame with no detections. */
class TapeRow(val t: Double, val fw: Double, val fh: Double, val pitch: Double, val label: String,
              val box: DoubleArray, val conf: Double, val trueZ: Double?)

object Tape {
    const val HEADER = "t,fw,fh,pitch,label,x1,y1,x2,y2,conf,true_z"

    fun parse(lines: List<String>): List<TapeRow> = lines.drop(1).filter { it.isNotBlank() }.map { l ->
        val c = l.split(",")
        TapeRow(c[0].toDouble(), c[1].toDouble(), c[2].toDouble(), c[3].toDouble(), c[4],
            doubleArrayOf(c[5].toDouble(), c[6].toDouble(), c[7].toDouble(), c[8].toDouble()),
            c[9].toDouble(), c.getOrNull(10)?.toDoubleOrNull())
    }

    fun format(r: TapeRow) = String.format(java.util.Locale.US, "%.3f,%.0f,%.0f,%.2f,%s,%.1f,%.1f,%.1f,%.1f,%.3f,%s",
        r.t, r.fw, r.fh, r.pitch, r.label, r.box[0], r.box[1], r.box[2], r.box[3], r.conf, r.trueZ?.toString() ?: "")
}

class ReplayResult(val frames: Int, val durationS: Double, val events: List<Pair<Double, Ev>>, val relErrors: List<Double>) {
    val speech get() = events.count { it.second is Ev.Speech }
    val critical get() = events.count { (it.second as? Ev.Speech)?.pri == P_CRITICAL }
    val beeps get() = events.count { it.second is Ev.Beep }

    /** Metrics for the ablation harness. alerts_per_min counts ALL spoken alerts, not verified false alarms. */
    fun metrics(): Map<String, Double> = mapOf(
        "frames" to frames.toDouble(),
        "speech_alerts_per_min" to if (durationS > 0) speech * 60.0 / durationS else 0.0,
        "critical_alerts" to critical.toDouble(),
        "range_samples" to relErrors.size.toDouble(),
        "range_rel_err_mean" to if (relErrors.isEmpty()) Double.NaN else relErrors.average())
}

/** Replays a detection tape through the real Pipeline (no Android needed). */
object ReplayRunner {
    fun run(rows: List<TapeRow>, hfovDeg: Double, camHeightM: Double, lang: String = "en"): ReplayResult {
        val pipe = Pipeline(Cam(hfovDeg, camHeightM, 0.0), lang)
        val events = ArrayList<Pair<Double, Ev>>()
        val errs = ArrayList<Double>()
        val groups = rows.groupBy { it.t }.toSortedMap()
        for ((t, g) in groups) {
            val dets = g.filter { it.label.isNotEmpty() }.map { Det(it.label, it.box, it.conf) }
            for (e in pipe.process(dets, t, g[0].fw, g[0].fh, g[0].pitch)) events += t to e
            for (r in g) {
                val tz = r.trueZ ?: continue
                val tr = pipe.lastTracks.firstOrNull { it.label == r.label && iou(it.box, r.box) >= 0.5 } ?: continue
                val z = tr.z ?: continue
                if (tz > 0) errs += kotlin.math.abs(z - tz) / tz
            }
        }
        val dur = if (groups.isEmpty()) 0.0 else groups.lastKey() - groups.firstKey()
        return ReplayResult(groups.size, dur, events, errs)
    }
}
