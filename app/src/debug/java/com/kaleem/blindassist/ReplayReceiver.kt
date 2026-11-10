package com.kaleem.blindassist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File

/**
 * Debug-only: turns a recorded walk video into a detection tape using the REAL on-device detector.
 *   adb push walk.mp4 /sdcard/Android/data/com.kaleem.blindassist/files/walk.mp4
 *   adb shell am broadcast -n com.kaleem.blindassist/.ReplayReceiver --es video walk.mp4 --ef fps 10 --ef pitch 0
 *   adb pull /sdcard/Android/data/com.kaleem.blindassist/files/walk.mp4.tape.csv
 * Then replay the tape on a PC with the JVM ReplayRunner (ablation). UNVERIFIED on device: video rotation
 * metadata handling and detector speed are untested.
 */
class ReplayReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val name = i.getStringExtra("video") ?: return
        val fps = i.getFloatExtra("fps", 10f).toDouble().coerceIn(1.0, 30.0)
        val pitch = i.getFloatExtra("pitch", 0f).toDouble()
        val pending = goAsync()
        Thread {
            try {
                val dir = ctx.getExternalFilesDir(null) ?: return@Thread
                val det = createDetector(ctx)
                if (det.limited) { Log.e("BA-REPLAY", "no model: tape would be empty"); return@Thread }
                val r = MediaMetadataRetriever().apply { setDataSource(File(dir, name).absolutePath) }
                val durMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val out = StringBuilder(Tape.HEADER).append('\n')
                var tMs = 0L
                while (tMs <= durMs) {
                    val bmp: Bitmap? = r.getFrameAtTime(tMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                    if (bmp != null) {
                        val t = tMs / 1000.0
                        val dets = det.detect(bmp)
                        if (dets.isEmpty()) out.append(Tape.format(TapeRow(t, bmp.width.toDouble(), bmp.height.toDouble(), pitch, "",
                            doubleArrayOf(0.0, 0.0, 0.0, 0.0), 0.0, null))).append('\n')
                        for (d in dets) out.append(Tape.format(TapeRow(t, bmp.width.toDouble(), bmp.height.toDouble(), pitch,
                            d.label, d.box, d.conf, null))).append('\n')
                    }
                    tMs += (1000.0 / fps).toLong()
                }
                r.release(); det.close()
                File(dir, "$name.tape.csv").writeText(out.toString())
                Log.i("BA-REPLAY", "wrote $name.tape.csv")
            } catch (e: Throwable) { Log.e("BA-REPLAY", "failed", e) } finally { pending.finish() }
        }.start()
    }
}
