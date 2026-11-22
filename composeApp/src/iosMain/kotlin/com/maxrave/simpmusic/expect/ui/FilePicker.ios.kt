package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.maxrave.data.db.documentDirectory
import com.maxrave.simpmusic.expect.presentFromTop
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

/**
 * The Files picker, asked for a COPY: the file lands in the app's own tmp folder, so the plain path
 * handed back stays readable without holding a security-scoped bookmark — which is what
 * restoreNative and readPickedFile open on every platform but Android.
 */
@Composable
actual fun filePickerResult(onResultUri: (String?) -> Unit): FilePickerLauncher {
    val currentOnResultUri by rememberUpdatedState(onResultUri)
    // Held here because UIKit keeps only a weak reference to a picker's delegate.
    val delegate = remember { DocumentPickerDelegate { currentOnResultUri(it) } }
    return remember {
        object : FilePickerLauncher {
            override fun launch() {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem), asCopy = true)
                picker.delegate = delegate
                picker.allowsMultipleSelection = false
                presentFromTop(picker)
            }
        }
    }
}

/**
 * Straight into the app's Documents folder, which the Files app shows as On My iPhone › SimpMusic.
 * A save panel would need the finished file before it opens, and the backup is only written after
 * this hands back where it goes.
 */
@Composable
actual fun fileSaverResult(
    fileName: String,
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher {
    val currentOnResultUri by rememberUpdatedState(onResultUri)
    val currentFileName by rememberUpdatedState(fileName)
    return remember {
        object : FilePickerLauncher {
            override fun launch() {
                currentOnResultUri(documentDirectory() + "/" + currentFileName)
            }
        }
    }
}

private class DocumentPickerDelegate(
    private val onResult: (String?) -> Unit,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    @ObjCSignatureOverride
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        onResult((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.path)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}
