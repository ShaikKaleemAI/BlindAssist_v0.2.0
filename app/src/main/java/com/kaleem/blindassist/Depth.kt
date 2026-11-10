package com.kaleem.blindassist

import android.graphics.Bitmap

/** Relative inverse depth (larger = closer), rows x cols, row 0 = top. NOT metric unless calibrated. */
interface DepthEstimator {
    val available: Boolean
    fun inverseDepth(bmp: Bitmap): Array<FloatArray>?
}

/** No depth model is shipped (none could be verified offline). TODO(user): export Depth Anything V2 small / MiDaS to INT8 TFLite and implement this interface. */
class NullDepth : DepthEstimator {
    override val available = false
    override fun inverseDepth(bmp: Bitmap): Array<FloatArray>? = null
}
