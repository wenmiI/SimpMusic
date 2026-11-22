package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.maxrave.data.db.documentDirectory
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.jetbrains.skia.Image

private const val TAG = "ImageIo"
private const val COVER_DIR = "playlist_covers"

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }.onFailure { Logger.w(TAG, "Could not decode image: ${it.message}") }.getOrNull()

actual suspend fun persistPickedImage(
    bytes: ByteArray,
    fileName: String,
): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            // Next to the database, which is where the rest of the app's own data lives on iOS.
            val dir = documentDirectory().toPath() / COVER_DIR
            FileSystem.SYSTEM.createDirectories(dir)
            val file = dir / fileName
            FileSystem.SYSTEM.write(file) { write(bytes) }
            "file://$file"
        }.onFailure { Logger.w(TAG, "Could not persist image: ${it.message}") }.getOrNull()
    }
