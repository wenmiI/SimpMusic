package com.maxrave.simpmusic.expect.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.maxrave.simpmusic.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.ios_pairing_unavailable
import simpmusic.composeapp.generated.resources.login_sync_android_title
import simpmusic.composeapp.generated.resources.ok

// The QR pairing needs the loginSync transport, which is Android and Desktop only, plus a camera
// scanner. Sync account carries the same things through the self-hosted host, and works here.
@Composable
actual fun LoginSyncDialog(onDismiss: () -> Unit) = PairingUnavailableDialog(Res.string.login_sync_android_title, onDismiss)

@Composable
internal fun PairingUnavailableDialog(
    title: StringResource,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(title), style = typo().titleSmall) },
        text = { Text(text = stringResource(Res.string.ios_pairing_unavailable), style = typo().bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.ok), color = LocalContentColor.current) }
        },
    )
}
