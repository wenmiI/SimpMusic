package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.presentFromTop
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val TAG = "PhotoPicker"

/**
 * The system photo picker, which needs no library permission: it hands over only what was chosen.
 * The file it gives is deleted as soon as the callback returns, so it is copied into tmp first and
 * that path is what comes back — readLocalImageBytes reads plain paths on iOS.
 */
@Composable
actual fun photoPickerResult(onResultUri: (String?) -> Unit): PhotoPickerLauncher {
    val currentOnResultUri by rememberUpdatedState(onResultUri)
    val delegate = remember { PhotoPickerDelegate { currentOnResultUri(it) } }
    return remember {
        object : PhotoPickerLauncher {
            override fun launch() {
                val configuration =
                    PHPickerConfiguration().apply {
                        filter = PHPickerFilter.imagesFilter
                        selectionLimit = 1
                    }
                val picker = PHPickerViewController(configuration)
                picker.delegate = delegate
                presentFromTop(picker)
            }
        }
    }
}

private class PhotoPickerDelegate(
    private val onResult: (String?) -> Unit,
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider
        if (provider == null) {
            onResult(null)
            return
        }
        provider.loadFileRepresentationForTypeIdentifier(UTTypeImage.identifier) { url, error ->
            val copy =
                url?.let { source ->
                    val target = NSURL.fileURLWithPath(NSTemporaryDirectory() + NSUUID().UUIDString + "-" + (source.lastPathComponent ?: "image"))
                    if (NSFileManager.defaultManager.copyItemAtURL(source, target, error = null)) target.path else null
                }
            if (copy == null) Logger.w(TAG, "Could not copy the picked image: ${error?.localizedDescription}")
            dispatch_async(dispatch_get_main_queue()) { onResult(copy) }
        }
    }
}
