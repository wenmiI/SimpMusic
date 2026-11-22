package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.runtime.Composable

// iOS rotates with the device by itself (Info.plist allows landscape); there is no orientation to
// force and no system bar to hide that the full-screen player does not already cover.
@Composable
actual fun FullScreenRotationImmersive(
    onLaunch: () -> Unit,
    onDispose: () -> Unit,
) {
}
