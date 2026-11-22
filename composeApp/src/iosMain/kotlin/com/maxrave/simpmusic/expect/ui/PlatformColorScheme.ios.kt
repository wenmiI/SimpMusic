package com.maxrave.simpmusic.expect.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.maxrave.simpmusic.AppViewController

// iOS derives no palette from the wallpaper.
@Composable
actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? = null

actual fun isWallpaperDynamicColorSupported(): Boolean = false

/**
 * The status bar's text follows the app theme rather than the system one, or a dark app on a light
 * phone gets black clock digits on black. The root view controller owns that style on iOS.
 */
@Composable
actual fun SystemBarAppearanceEffect(isDark: Boolean) {
    LaunchedEffect(isDark) {
        AppViewController.current?.setDarkTheme(isDark)
    }
}
