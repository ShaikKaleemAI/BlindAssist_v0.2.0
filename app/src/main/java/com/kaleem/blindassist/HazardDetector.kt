package com.kaleem.blindassist

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

const val HAZARD_ASSET = "models/hazard_yolo_int8.tflite"
const val HAZARD_LABELS_ASSET = "models/hazard_labels.txt"

/**
 * Custom hazard detector (YOLO INT8 TFLite). Loads only if the user dropped the trained model + labels into assets/models.
 * UNVERIFIED: never run (no trained model exists). Image is stretched (not letterboxed) to the input size:
 * if training used letterbox this costs accuracy; verify with ml/eval.py on-device output.
 * NNAPI is OFF by default (Cfg.useNnapi): INT8 YOLO on NNAPI is known to misbehave on some phones; CPU is the safe default.
 */
class TfliteHazardDetector(ctx: Context) : Detector {
    override val limited = false
    private val labels: List<String> = ctx.assets.open(HAZARD_LABELS_ASSET).bufferedReader().readLines()
        .map { it.trim() }.filter { it.isNotEmpty() }
    private var nnapi: NnApiDelegate? = null
    private val interp: Interpreter
    private val inW: Int; private val inH: Int
    private val inType: DataType
    private val inScale: Float; private val inZero: Int
    private val outType: DataType
    private val outScale: Float; private val outZero: Int
    private val outD1: Int; private val outD2: Int
    private val outBuf: ByteBuffer

    init {
        val fd = ctx.assets.openFd(HAZARD_ASSET)
        val model: MappedByteBuffer = fd.createInputStream().channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        interp = build(model)
        val it0 = interp.getInputTensor(0); val shp = it0.shape()            // [1, h, w, 3]
        inH = shp[1]; inW = shp[2]; inType = it0.dataType()
        inScale = it0.quantizationParams().scale; inZero = it0.quantizationParams().zeroPoint
        val ot = interp.getOutputTensor(0); val os = ot.shape()
        outD1 = os[1]; outD2 = os[2]; outType = ot.dataType()
        outScale = ot.quantizationParams().scale; outZero = ot.quantizationParams().zeroPoint
        outBuf = ByteBuffer.allocateDirect(ot.numBytes()).order(ByteOrder.nativeOrder())
        require(minOf(outD1, outD2) == 4 + labels.size) { "labels (${labels.size}) do not match model output ${os.toList()}" }
    }

    private fun build(model: MappedByteBuffer): Interpreter {
        if (Cfg.useNnapi) {
            try {
                nnapi = NnApiDelegate()
                return Interpreter(model, Interpreter.Options().addDelegate(nnapi).setNumThreads(2))
            } catch (e: Throwable) { Log.w("BA", "NNAPI failed, CPU fallback", e); nnapi?.close(); nnapi = null }
        }
        return Interpreter(model, Interpreter.Options().setNumThreads(4))
    }

    override fun detect(bmp: Bitmap): List<Det> {
        val scaled = Bitmap.createScaledBitmap(bmp, inW, inH, true)
        val px = IntArray(inW * inH); scaled.getPixels(px, 0, inW, 0, 0, inW, inH)
        val bytes = when (inType) { DataType.FLOAT32 -> 4; else -> 1 }
        val inBuf = ByteBuffer.allocateDirect(inW * inH * 3 * bytes).order(ByteOrder.nativeOrder())
        for (p in px) for (shift in intArrayOf(16, 8, 0)) {
            val c = (p shr shift) and 0xFF
            when (inType) {
                DataType.FLOAT32 -> inBuf.putFloat(c / 255f)
                DataType.UINT8 -> inBuf.put(c.toByte())
                else -> inBuf.put((Math.round(c / 255f / inScale) + inZero).coerceIn(-128, 127).toByte())
            }
        }
        inBuf.rewind(); outBuf.rewind()
        interp.run(inBuf, outBuf)
        outBuf.rewind()
        val n = outD1 * outD2
        val out = FloatArray(n)
        when (outType) {
            DataType.FLOAT32 -> outBuf.asFloatBuffer().get(out)
            DataType.UINT8 -> for (i in 0 until n) out[i] = ((outBuf.get().toInt() and 0xFF) - outZero) * outScale
            else -> for (i in 0 until n) out[i] = (outBuf.get().toInt() - outZero) * outScale
        }
        return YoloDecoder.decode(out, outD1, outD2, labels.size, Cfg.SCORE_MIN, bmp.width.toDouble(), bmp.height.toDouble())
            .map { Det(HazardLabels.map(labels[it.cls]), it.box, it.score.toDouble()) }
            .filter { it.label in Cfg.NAV }
    }

    override fun close() { try { interp.close() } catch (_: Exception) {}; try { nnapi?.close() } catch (_: Exception) {} }
}
