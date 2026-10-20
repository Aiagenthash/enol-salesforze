package com.enol.salesforze.data.sync

import android.content.Context
import androidx.work.*
import com.enol.salesforze.data.sync.workers.DeltaSyncWorker
import com.enol.salesforze.data.sync.workers.GeneralSyncWorker
import com.enol.salesforze.data.sync.workers.OrderSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {

    companion object {
        const val PERIODIC_DELTA_SYNC_TAG = "periodic_delta_sync"
        const val IMMEDIATE_DELTA_SYNC_TAG = "immediate_delta_sync"
        const val UPLOAD_SYNC_TAG = "upload_sync"
        const val PERIODIC_WORK_NAME = "enol_periodic_delta_sync"
    }

    private val connectedConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Schedules periodic background delta sync with exponential backoff and network constraints.
     */
    fun schedulePeriodicSync(intervalMinutes: Long = 15) {
        val periodicRequest = PeriodicWorkRequestBuilder<DeltaSyncWorker>(
            intervalMinutes, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // 5 min flex period
        )
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(PERIODIC_DELTA_SYNC_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    /**
     * Triggers an immediate one-time Delta Sync (download changes from backend).
     */
    fun triggerImmediateDeltaSync(): Operation {
        val immediateRequest = OneTimeWorkRequestBuilder<DeltaSyncWorker>()
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(IMMEDIATE_DELTA_SYNC_TAG)
            .build()

        return workManager.enqueueUniqueWork(
            "immediate_delta_sync",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }

    /**
     * Triggers an immediate upload of offline pending items:
     * Chained: OrderSyncWorker -> GeneralSyncWorker -> DeltaSyncWorker
     */
    fun triggerImmediateUploadSync(): Operation {
        val orderSyncRequest = OneTimeWorkRequestBuilder<OrderSyncWorker>()
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(UPLOAD_SYNC_TAG)
            .build()

        val generalSyncRequest = OneTimeWorkRequestBuilder<GeneralSyncWorker>()
            .setConstraints(connectedConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(UPLOAD_SYNC_TAG)
            .build()

        val deltaSyncRequest = OneTimeWorkRequestBuilder<DeltaSyncWorker>()
            .setConstraints(connectedConstraints)
            .addTag(IMMEDIATE_DELTA_SYNC_TAG)
            .build()

        return workManager.beginUniqueWork(
            "immediate_upload_and_refresh_sync",
            ExistingWorkPolicy.REPLACE,
            orderSyncRequest
        )
            .then(generalSyncRequest)
            .then(deltaSyncRequest)
            .enqueue()
    }

    /**
     * Observes live WorkInfo for Delta Sync to drive UI sync progress indicators.
     */
    fun getDeltaSyncWorkInfoFlow(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagFlow(IMMEDIATE_DELTA_SYNC_TAG)
    }

    /**
     * Observes live WorkInfo for offline upload jobs.
     */
    fun getUploadSyncWorkInfoFlow(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagFlow(UPLOAD_SYNC_TAG)
    }

    /**
     * Cancel all active background synchronization jobs.
     */
    fun cancelAllSyncWork() {
        workManager.cancelAllWorkByTag(PERIODIC_DELTA_SYNC_TAG)
        workManager.cancelAllWorkByTag(IMMEDIATE_DELTA_SYNC_TAG)
        workManager.cancelAllWorkByTag(UPLOAD_SYNC_TAG)
    }
}
