package com.maxrave.simpmusic.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.maxrave.domain.data.model.ui.ScreenSizeInfo
import platform.UIKit.UIApplication
import kotlin.math.roundToInt

// The Compose view fills the window, so its container is the screen less nothing.
@Composable
actual fun getScreenSizeInfo(): ScreenSizeInfo {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current
    return ScreenSizeInfo(
        hDP = with(density) { window.containerSize.height.toDp().value.roundToInt() },
        wDP = with(density) { window.containerSize.width.toDp().value.roundToInt() },
        hPX = window.containerSize.height,
        wPX = window.containerSize.width,
    )
}

@Composable
actual fun KeepScreenOn() {
    DisposableEffect(Unit) {
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}

// Picture in picture for the video player is not wired up on iOS.
@Composable
actual fun rememberIsInPipMode(): Boolean = false
