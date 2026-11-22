package com.maxrave.simpmusic.expect

import com.maxrave.logger.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIImage
import kotlin.coroutines.resume

private const val TAG = "ShareImage"

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
    }

/** Into the photo library, which asks for add-only access the first time (NSPhotoLibraryAddUsageDescription). */
actual suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean {
    val image = UIImage.imageWithData(bytes.toNSData())
    if (image == null) {
        Logger.e(TAG, "Could not decode $fileName for the photo library")
        return false
    }
    return suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            { PHAssetChangeRequest.creationRequestForAssetFromImage(image) },
        ) { success, error ->
            if (!success) Logger.e(TAG, "Could not save $fileName: ${error?.localizedDescription}")
            continuation.resume(success)
        }
    }
}

/** The system share sheet, handed a real file so the receiving app gets its name and type. */
actual suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
    mimeType: String,
): Boolean {
    val url =
        withContext(Dispatchers.Default) {
            val target = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
            if (bytes.toNSData().writeToURL(target, atomically = true)) target else null
        }
    if (url == null) {
        Logger.e(TAG, "Could not share $fileName: writing the temporary copy failed")
        return false
    }
    withContext(Dispatchers.Main) {
        presentFromTop(UIActivityViewController(activityItems = listOf(url), applicationActivities = null))
    }
    return true
}
