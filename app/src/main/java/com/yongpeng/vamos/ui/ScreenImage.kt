// Modified by Vamos (2026): bounded screenshot decode.
package com.yongpeng.vamos.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory

internal fun screenshotSampleSize(width: Int, targetWidth: Int): Int {
    var sample = 1
    while (targetWidth > 0 && width / (sample * 2) >= targetWidth && sample < 16384) sample *= 2
    return sample
}

internal fun decodeScreenImage(path: String, widthPx: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    options.inSampleSize = screenshotSampleSize(options.outWidth, widthPx)
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeFile(path, options)
}

internal fun visibleImeHeight(visible: Boolean, ime: Int, navigation: Int): Int =
    if (visible) (ime - navigation).coerceAtLeast(0) else 0
