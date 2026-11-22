package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** The photo library asks for add-only access itself, at the moment of the first save. */
@Composable
actual fun rememberSaveImagePermission(onResult: (granted: Boolean) -> Unit): SaveImagePermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        object : SaveImagePermissionRequester {
            override fun requestIfNeeded() = currentOnResult(true)
        }
    }
}
