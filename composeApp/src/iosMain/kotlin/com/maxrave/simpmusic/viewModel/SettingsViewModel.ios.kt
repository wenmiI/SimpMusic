package com.maxrave.simpmusic.viewModel

import com.eygraber.uri.Uri
import com.maxrave.common.DB_NAME
import com.maxrave.common.SETTINGS_FILENAME
import com.maxrave.data.db.documentDirectory
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSUserDefaults
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.restore_success
import kotlin.system.exitProcess

private const val TAG = "BackupRestore"

/** Where createDataStoreInstance keeps the settings on iOS. */
private fun settingsPath(): Path = "${documentDirectory()}/$SETTINGS_FILENAME.preferences_pb".toPath()

// Both pickers hand back plain paths on iOS (see FilePicker.ios.kt); a file: uri is accepted too.
private fun Uri.toFilePath(): Path = toString().removePrefix("file://").toPath()

actual suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction? = null

actual suspend fun readPickedFile(uri: Uri): ByteArray = withContext(Dispatchers.IO) { FileSystem.SYSTEM.read(uri.toFilePath()) { readByteArray() } }

actual suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit,
) {
    withContext(Dispatchers.IO) {
        // okio inflates deflated entries too, so backups made on Android and Desktop restore here.
        val zip = FileSystem.SYSTEM.openZip(uri.toFilePath())
        for (entry in zip.list("/".toPath())) {
            Logger.d(TAG, "Processing entry: ${entry.name}")
            when (entry.name) {
                "$SETTINGS_FILENAME.preferences_pb" -> {
                    FileSystem.SYSTEM.write(settingsPath()) { zip.read(entry) { writeAll(this) } }
                }

                DB_NAME -> {
                    commonRepository.databaseDaoCheckpoint()
                    commonRepository.closeDatabase()
                    val database = commonRepository.getDatabasePath()?.toPath() ?: continue
                    FileSystem.SYSTEM.write(database) { zip.read(entry) { writeAll(this) } }
                }
            }
        }
    }
    withContext(Dispatchers.Main) {
        showToast(getString(Res.string.restore_success), ToastGravity.Bottom)
        showToast("SimpMusic will close; open it again to apply the backup", ToastGravity.Bottom)
        delay(2000)
        // iOS cannot relaunch an app, so this only ends it.
        exitProcess(0)
    }
}

actual suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
) {
    withContext(Dispatchers.IO) {
        commonRepository.databaseDaoCheckpoint()
        val database = commonRepository.getDatabasePath()?.toPath()
        FileSystem.SYSTEM.write(uri.toFilePath()) {
            val zip = StoredZipWriter(this)
            zip.add("$SETTINGS_FILENAME.preferences_pb", FileSystem.SYSTEM.read(settingsPath()) { readByteArray() })
            if (database != null) zip.add(DB_NAME, FileSystem.SYSTEM.read(database) { readByteArray() })
            zip.finish()
        }
    }
}

actual fun getPackageName(): String = ""

actual fun getFileDir(): String = ""

/** Picked up at the next launch: iOS reads an app's language once, when it starts. */
actual fun changeLanguageNative(code: String) {
    NSUserDefaults.standardUserDefaults.setObject(listOf(code), forKey = "AppleLanguages")
}
