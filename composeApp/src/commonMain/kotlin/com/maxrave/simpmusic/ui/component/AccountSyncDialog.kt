package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxrave.domain.data.model.librarysync.AccountSyncProblem
import com.maxrave.domain.data.model.librarysync.AccountSyncState
import com.maxrave.simpmusic.Platform
import com.maxrave.simpmusic.expect.ui.PendingSettingsCard
import com.maxrave.simpmusic.expect.ui.SyncProblemBox
import com.maxrave.simpmusic.expect.ui.rememberLocalNetworkPermission
import com.maxrave.simpmusic.expect.ui.restartAfterSyncedSettings
import com.maxrave.simpmusic.expect.ui.syncedAgo
import com.maxrave.simpmusic.getPlatform
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.AccountSyncViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.account_sync_create
import simpmusic.composeapp.generated.resources.account_sync_create_account
import simpmusic.composeapp.generated.resources.account_sync_footer
import simpmusic.composeapp.generated.resources.account_sync_intro
import simpmusic.composeapp.generated.resources.account_sync_never_synced
import simpmusic.composeapp.generated.resources.account_sync_not_a_host
import simpmusic.composeapp.generated.resources.account_sync_paired
import simpmusic.composeapp.generated.resources.account_sync_password
import simpmusic.composeapp.generated.resources.account_sync_server
import simpmusic.composeapp.generated.resources.account_sync_server_hint
import simpmusic.composeapp.generated.resources.account_sync_share_downloads
import simpmusic.composeapp.generated.resources.account_sync_share_downloads_description
import simpmusic.composeapp.generated.resources.account_sync_sign_in
import simpmusic.composeapp.generated.resources.account_sync_sign_out
import simpmusic.composeapp.generated.resources.account_sync_signed_in_as
import simpmusic.composeapp.generated.resources.account_sync_signed_out
import simpmusic.composeapp.generated.resources.account_sync_signup_closed
import simpmusic.composeapp.generated.resources.account_sync_title
import simpmusic.composeapp.generated.resources.account_sync_unreachable
import simpmusic.composeapp.generated.resources.account_sync_user
import simpmusic.composeapp.generated.resources.account_sync_user_exists
import simpmusic.composeapp.generated.resources.account_sync_wrong_password
import simpmusic.composeapp.generated.resources.cancel
import simpmusic.composeapp.generated.resources.library_sync_failed
import simpmusic.composeapp.generated.resources.library_sync_last_synced_at
import simpmusic.composeapp.generated.resources.library_sync_settings_differ_android
import simpmusic.composeapp.generated.resources.library_sync_settings_differ_desktop
import simpmusic.composeapp.generated.resources.library_sync_sync_now
import simpmusic.composeapp.generated.resources.library_sync_syncing
import simpmusic.composeapp.generated.resources.ok

/**
 * Settings → Devices → Sync account: signs this device in to an account on a host the user runs, then
 * shows how the account is doing. The same on Android and Desktop: towards the host both are devices.
 */
@Composable
fun AccountSyncDialog(onDismiss: () -> Unit) {
    val viewModel: AccountSyncViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signIn by viewModel.signIn.collectAsStateWithLifecycle()
    val pending by viewModel.pendingSettings.collectAsStateWithLifecycle()
    val localNetwork = rememberLocalNetworkPermission()

    var server by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    // Not saved: a password does not belong in the saved instance state.
    var password by androidx.compose.runtime.remember { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.open() }

    val canSubmit = server.isNotBlank() && user.isNotBlank() && password.isNotEmpty() && !signIn.busy
    val submit = {
        // A host on the VPN is "the local network" to Android 17, which then asks first.
        localNetwork.requestIfNeeded(if ("://" in server) server else "http://$server")
        viewModel.signIn(server, user, password, create)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.account_sync_title), style = typo().titleSmall) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isSignedIn) {
                    SignedIn(state, viewModel)
                    pending?.let {
                        PendingSettingsCard(
                            pending = it,
                            differ =
                                stringResource(
                                    if (getPlatform() == Platform.Android) {
                                        Res.string.library_sync_settings_differ_android
                                    } else {
                                        Res.string.library_sync_settings_differ_desktop
                                    },
                                ),
                            onNotNow = viewModel::dismissSettings,
                            onApply = { viewModel.applySettings(::restartAfterSyncedSettings) },
                        )
                    }
                    Text(text = stringResource(Res.string.account_sync_footer), style = typo().bodySmall)
                } else {
                    Text(text = stringResource(Res.string.account_sync_intro), style = typo().bodySmall)
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = { Text(stringResource(Res.string.account_sync_server)) },
                        placeholder = { Text(stringResource(Res.string.account_sync_server_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text(stringResource(Res.string.account_sync_user)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(Res.string.account_sync_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().toggleable(value = create, role = Role.Checkbox, onValueChange = { create = it }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = create, onCheckedChange = null)
                        Text(text = stringResource(Res.string.account_sync_create), style = typo().bodyMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                    if (signIn.busy) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    signIn.problem?.let { SyncProblemBox(problemText(it, signIn.detail, server)) }
                    // Why the last account stopped, when that is why this form is showing.
                    if (signIn.problem == null) state.problem?.let { SyncProblemBox(problemText(it, state.problemDetail, server)) }
                }
            }
        },
        confirmButton = {
            if (state.isSignedIn) {
                TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.ok), color = LocalContentColor.current) }
            } else {
                TextButton(onClick = submit, enabled = canSubmit) {
                    Text(
                        text = stringResource(if (create) Res.string.account_sync_create_account else Res.string.account_sync_sign_in),
                        color = LocalContentColor.current,
                    )
                }
            }
        },
        dismissButton = {
            if (state.isSignedIn) {
                TextButton(onClick = viewModel::syncNow, enabled = !state.syncing) {
                    Text(text = stringResource(Res.string.library_sync_sync_now), color = LocalContentColor.current)
                }
            } else {
                TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.cancel), color = LocalContentColor.current) }
            }
        },
    )
}

@Composable
private fun SignedIn(
    state: AccountSyncState,
    viewModel: AccountSyncViewModel,
) {
    val account = state.account ?: return
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(Res.string.account_sync_signed_in_as, account.user, account.serverName), style = typo().bodyMedium)
            Text(text = account.server, style = typo().bodySmall)
            Text(
                text =
                    when {
                        state.syncing -> stringResource(Res.string.library_sync_syncing)
                        state.lastSyncAt != null -> stringResource(Res.string.library_sync_last_synced_at, syncedAgo(state.lastSyncAt!!))
                        else -> stringResource(Res.string.account_sync_never_synced)
                    },
                style = typo().bodySmall,
            )
        }
        TextButton(onClick = viewModel::signOut) {
            Text(text = stringResource(Res.string.account_sync_sign_out), color = LocalContentColor.current)
        }
    }
    state.problem?.let { SyncProblemBox(problemText(it, state.problemDetail, account.serverName)) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = state.shareDownloads, role = Role.Switch, onValueChange = viewModel::setShareDownloads),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(Res.string.account_sync_share_downloads), style = typo().bodyMedium)
            Text(text = stringResource(Res.string.account_sync_share_downloads_description), style = typo().bodySmall)
        }
        Switch(checked = state.shareDownloads, onCheckedChange = null)
    }
}

@Composable
private fun problemText(
    problem: AccountSyncProblem,
    detail: String?,
    host: String,
): String =
    when (problem) {
        AccountSyncProblem.UNREACHABLE -> stringResource(Res.string.account_sync_unreachable, host)
        AccountSyncProblem.NOT_A_HOST -> stringResource(Res.string.account_sync_not_a_host)
        AccountSyncProblem.WRONG_PASSWORD -> stringResource(Res.string.account_sync_wrong_password)
        AccountSyncProblem.SIGNUP_CLOSED -> stringResource(Res.string.account_sync_signup_closed)
        AccountSyncProblem.USER_EXISTS -> stringResource(Res.string.account_sync_user_exists)
        AccountSyncProblem.SIGNED_OUT -> stringResource(Res.string.account_sync_signed_out)
        AccountSyncProblem.PAIRED -> stringResource(Res.string.account_sync_paired)
        AccountSyncProblem.FAILED -> stringResource(Res.string.library_sync_failed, detail.orEmpty())
    }
