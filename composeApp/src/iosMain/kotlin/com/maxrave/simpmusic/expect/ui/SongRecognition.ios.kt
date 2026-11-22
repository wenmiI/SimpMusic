package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

// Song recognition is Android-only; the search bar never opens this on iOS.
@Composable
actual fun SongRecognitionSheet(
    onFind: (String) -> Unit,
    onDismiss: () -> Unit,
) {}
