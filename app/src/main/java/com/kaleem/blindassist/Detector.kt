package com.kaleem.blindassist

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector

const val MODEL_ASSET = "models/efficientdet_lite0.tflite"

interface Detector {
    /** true = no real detector loaded: the app only warns, it does NOT see obstacles. */
    val limited: Boolean
    fun detect(bmp: Bitmap): List<Det>
    fun close()
}

/** Limited-mode fallback: detects nothing. The service must warn the user loudly and repeatedly. */
class NullDetector : Detector {
    override val limited = true
    override fun detect(bmp: Bitmap): List<Det> = emptyList()
    override fun close() {}
}

/** On-device COCO object detector (EfficientDet-Lite0 via MediaPipe). */
class MediaPipeDetector(ctx: Context) : Detector {
    override val limited = false
    private val od: ObjectDetector = ObjectDetector.createFromOptions(
        ctx,
        ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
            .setScoreThreshold(Cfg.SCORE_MIN)
            .setMaxResults(12)
            .setRunningMode(RunningMode.IMAGE)
            .build())

    override fun detect(bmp: Bitmap): List<Det> {
        val res = od.detect(BitmapImageBuilder(bmp).build())
        val out = ArrayList<Det>()
        for (d in res.detections()) {
            val c = d.categories().firstOrNull() ?: continue
            val name = c.categoryName()
            if (name !in Cfg.NAV) continue
            val b = d.boundingBox()
            out += Det(name, doubleArrayOf(b.left.toDouble(), b.top.toDouble(), b.right.toDouble(), b.bottom.toDouble()),
                c.score().toDouble())
        }
        return out
    }

    override fun close() = od.close()
}

/** Hazard model if present, else COCO EfficientDet-Lite0, else [NullDetector] (limited mode). Each step falls through on any failure. */
fun createDetector(ctx: Context): Detector {
    val files = try { ctx.assets.list("models")?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() }
    if ("hazard_yolo_int8.tflite" in files && "hazard_labels.txt" in files) {
        try { return TfliteHazardDetector(ctx) } catch (e: Throwable) { Log.e("BA", "hazard model failed, trying COCO", e) }
    }
    if ("efficientdet_lite0.tflite" in files) {
        try { return MediaPipeDetector(ctx) } catch (e: Throwable) { Log.e("BA", "COCO model failed -> limited mode", e) }
    }
    Log.w("BA", "no usable model -> limited mode")
    return NullDetector()
}
