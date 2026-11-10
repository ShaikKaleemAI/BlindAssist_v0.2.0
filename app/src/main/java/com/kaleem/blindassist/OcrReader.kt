package com.kaleem.blindassist

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit

/** On-device ML Kit OCR (bundled Latin model). Hindi/Telugu text is NOT recognised. UNVERIFIED on device. Call off the main thread. */
class OcrReader {
    private val rec = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    fun read(bmp: Bitmap): String = try {
        Tasks.await(rec.process(InputImage.fromBitmap(bmp, 0)), 5, TimeUnit.SECONDS).text
    } catch (e: Exception) { "" }
    fun close() = try { rec.close() } catch (_: Exception) {}
}
