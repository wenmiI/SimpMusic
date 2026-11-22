package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

// iOS only lets an app change the system volume through MPVolumeView's own slider, so there is no
// fraction to set from a row the app draws. Null hides the Apple Music style's volume row.
@Composable
actual fun rememberDeviceVolumeController(): DeviceVolumeController? = null
