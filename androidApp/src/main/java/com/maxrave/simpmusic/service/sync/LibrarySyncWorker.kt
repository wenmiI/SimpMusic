package com.maxrave.simpmusic.service.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maxrave.domain.repository.AccountSyncRepository
import com.maxrave.domain.repository.LibrarySyncPhoneRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** One round; a computer that is away is simply tried again on the next run. */
class LibrarySyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val repository: LibrarySyncPhoneRepository by inject()
    private val account: AccountSyncRepository by inject()

    // Each returns at once when this phone is not paired, or not signed in.
    override suspend fun doWork(): Result {
        repository.syncNow()
        account.syncNow()
        return Result.success()
    }
}
