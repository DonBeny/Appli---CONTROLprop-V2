package org.orgaprop.controlprop.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncUtils {

    private const val SYNC_WORK_NAME = "sync_work"

    fun scheduleSync(context: Context, immediate: Boolean = false) {
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .apply {
                if (!immediate) {
                    setInitialDelay(15, TimeUnit.MINUTES)
                }
            }
            .build()

        workManager.enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelPendingSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
    }

}
