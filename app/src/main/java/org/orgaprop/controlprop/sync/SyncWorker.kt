package org.orgaprop.controlprop.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.java.KoinJavaComponent.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val syncManager: SyncManager by inject(SyncManager::class.java)

    override suspend fun doWork(): Result {
        return when (syncManager.syncPendingControls()) {
            SyncManager.SyncResult.SUCCESS -> Result.success()
            is SyncManager.SyncResult.PARTIAL_SUCCESS -> Result.success()
            else -> Result.retry()
        }
    }

}
