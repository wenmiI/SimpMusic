package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.library_sync_title
import kotlin.system.exitProcess

@Composable
actual fun LibrarySyncDialog(onDismiss: () -> Unit) = PairingUnavailableDialog(Res.string.library_sync_title, onDismiss)

// iOS cannot relaunch an app, so this only ends it; the settings apply when it is opened again.
actual fun restartAfterSyncedSettings() {
    exitProcess(0)
}
