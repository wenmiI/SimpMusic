package com.maxrave.simpmusic.expect

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

// The screen's bounds follow the interface orientation, unlike UIDevice.orientation, which also
// reports face-up and face-down.
@OptIn(ExperimentalForeignApi::class)
actual fun currentOrientation(): Orientation =
    UIScreen.mainScreen.bounds.useContents {
        if (size.width > size.height) Orientation.LANDSCAPE else Orientation.PORTRAIT
    }
