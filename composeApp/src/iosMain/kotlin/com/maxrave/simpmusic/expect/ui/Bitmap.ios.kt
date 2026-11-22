package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.toBitmap
import com.maxrave.simpmusic.extension.toResizedBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

// iOS draws through skiko like Desktop, so these are the Desktop actuals.

actual fun ImageBitmap.toByteArray(): ByteArray? {
    val image = Image.makeFromBitmap(this.asSkiaBitmap())
    return image.encodeToData(EncodedImageFormat.JPEG, 100)?.bytes
}

actual fun ImageBitmap.toPngByteArray(): ByteArray? {
    val image = Image.makeFromBitmap(this.asSkiaBitmap())
    return image.encodeToData(EncodedImageFormat.PNG)?.bytes
}

actual fun coil3.Image.toImageBitmap(): ImageBitmap = this.toBitmap().asComposeImageBitmap()

// Skia bitmaps live in memory and can always be read, so a plain resize is enough here.
actual fun ImageBitmap.toReadableBitmap(
    width: Int,
    height: Int,
): ImageBitmap = toResizedBitmap(width, height)
