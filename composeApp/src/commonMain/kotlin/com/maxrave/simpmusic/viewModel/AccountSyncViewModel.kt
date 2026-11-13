package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.model.librarysync.AccountSyncException
import com.maxrave.domain.data.model.librarysync.AccountSyncProblem
import com.maxrave.domain.data.model.librarysync.AccountSyncState
import com.maxrave.domain.data.model.librarysync.PendingSettings
import com.maxrave.domain.repository.AccountSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Signing in to a sync account, then its status. Android and Desktop show the same screen. */
class AccountSyncViewModel(
    private val repository: AccountSyncRepository,
) : ViewModel() {
    /** The sign-in form's own state; once signed in the screen shows [state]. */
    data class SignIn(
        val busy: Boolean = false,
        val problem: AccountSyncProblem? = null,
        val detail: String? = null,
    )

    val state: StateFlow<AccountSyncState> = repository.state
    val pendingSettings: StateFlow<PendingSettings?> = repository.pendingSettings

    private val _signIn = MutableStateFlow(SignIn())
    val signIn: StateFlow<SignIn> = _signIn.asStateFlow()

    fun open() {
        _signIn.value = SignIn()
        if (repository.state.value.isSignedIn) syncNow()
    }

    fun signIn(
        server: String,
        user: String,
        password: String,
        create: Boolean,
    ) {
        if (_signIn.value.busy) return
        _signIn.value = SignIn(busy = true)
        viewModelScope.launch {
            _signIn.value =
                try {
                    repository.signIn(server, user.trim(), password, create)
                    SignIn()
                } catch (e: AccountSyncException) {
                    SignIn(problem = e.problem, detail = e.detail)
                }
        }
    }

    fun signOut() {
        viewModelScope.launch { repository.signOut() }
    }

    fun syncNow() {
        viewModelScope.launch { repository.syncNow() }
    }

    fun setShareDownloads(share: Boolean) {
        viewModelScope.launch { repository.setShareDownloads(share) }
    }

    fun applySettings(restart: () -> Unit) {
        viewModelScope.launch {
            repository.applyPendingSettings()
            restart()
        }
    }

    fun dismissSettings() {
        viewModelScope.launch { repository.dismissPendingSettings() }
    }
}
