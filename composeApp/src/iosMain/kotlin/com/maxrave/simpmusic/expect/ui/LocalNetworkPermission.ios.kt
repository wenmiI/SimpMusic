package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS asks for local network access by itself, the first time the app connects into the user's
 * network (NSLocalNetworkUsageDescription), so there is nothing to ask ahead of time.
 */
@Composable
actual fun rememberLocalNetworkPermission(): LocalNetworkPermissionRequester =
    remember {
        object : LocalNetworkPermissionRequester {
            override fun requestIfNeeded(url: String) = Unit
        }
    }
