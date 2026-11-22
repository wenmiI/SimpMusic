package com.maxrave.simpmusic.expect.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import platform.AVKit.AVRoutePickerView
import platform.UIKit.UIColor

/**
 * iOS has no Google Cast sender in this build; the system's own route picker stands in, which is
 * what an iPhone user reaches for anyway: AirPlay speakers and TVs, Bluetooth, CarPlay. It moves
 * the whole audio session, so the player needs to know nothing about it.
 */
@Composable
actual fun PlatformCastButton(
    modifier: Modifier,
    tint: Color,
) {
    UIKitView(
        factory = {
            AVRoutePickerView().apply {
                backgroundColor = UIColor.clearColor
                prioritizesVideoDevices = false
            }
        },
        modifier = modifier.size(40.dp),
        update = { picker ->
            picker.tintColor = tint.toUIColor()
            picker.activeTintColor = tint.toUIColor()
        },
    )
}

actual fun isPlatformCastAvailable(): Boolean = true

// The route picker is the whole UI; there is no list of receivers for an in-app picker to show.
@Composable
actual fun rememberCastReceivers(discover: Boolean): CastReceivers = NoCastReceivers

private val NoCastReceivers = CastReceivers(receivers = emptyList(), connect = {}, disconnect = {})

internal fun Color.toUIColor(): UIColor =
    UIColor.colorWithRed(red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())
