package com.maxrave.simpmusic.expect

import com.maxrave.data.db.documentDirectory

/**
 * The app's Documents folder, which the Files app shows under On My iPhone › SimpMusic
 * (UIFileSharingEnabled + LSSupportsOpeningDocumentsInPlace in Info.plist).
 */
actual fun getDownloadFolderPath(): String = documentDirectory()
