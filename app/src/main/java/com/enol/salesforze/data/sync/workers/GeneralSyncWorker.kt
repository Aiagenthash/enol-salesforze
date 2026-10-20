package com.enol.salesforze.data.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.remote.api.EnterpriseFieldApi
import com.enol.salesforze.data.remote.models.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import java.io.IOException

@HiltWorker
class GeneralSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: EnterpriseFieldApi,
    private val database: AppDatabase,
    private val json: Json
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingItems = database.pendingSyncQueueDao().getAllPendingOnce()
        if (pendingItems.isEmpty()) {
            return Result.success()
        }

        var anyFailed = false

        for (item in pendingItems) {
            // Skip order entity types (handled exclusively by OrderSyncWorker)
            if (item.entityType.equals("order", ignoreCase = true)) {
                continue
            }

            try {
                var isSuccess = false

                when (item.entityType) {
                    "client.create" -> {
                        val req = json.decodeFromString<CreateClientRequest>(item.payloadJson)
                        val res = api.createClient(req)
                        isSuccess = res.success
                    }
                    "visit.check_in" -> {
                        val req = json.decodeFromString<CheckInVisitRequest>(item.payloadJson)
                        val res = api.checkInVisit(req)
                        isSuccess = res.success
                    }
                    "visit.check_out" -> {
                        val req = json.decodeFromString<CheckOutVisitRequest>(item.payloadJson)
                        val res = api.checkOutVisit(item.entityId, req)
                        isSuccess = res.success
                    }
                    "attendance.start" -> {
                        val req = json.decodeFromString<StartShiftRequest>(item.payloadJson)
                        val res = api.startShift(req)
                        isSuccess = res.success
                    }
                    "attendance.end" -> {
                        val req = json.decodeFromString<EndShiftRequest>(item.payloadJson)
                        val res = api.endShift(item.entityId, req)
                        isSuccess = res.success
                    }
                    "tada.submit" -> {
                        val req = json.decodeFromString<SubmitTadaClaimRequest>(item.payloadJson)
                        val res = api.submitTadaClaim(req)
                        isSuccess = res.success
                    }
                    "mechanic.register" -> {
                        val req = json.decodeFromString<RegisterMechanicRequest>(item.payloadJson)
                        val res = api.registerMechanic(req)
                        isSuccess = res.success
                    }
                    "mechanic.points" -> {
                        val req = json.decodeFromString<AwardPointsRequest>(item.payloadJson)
                        val res = api.awardMechanicPoints(item.entityId, req)
                        isSuccess = res.success
                    }
                    "breadcrumbs.upload" -> {
                        val req = json.decodeFromString<BreadcrumbBatchRequest>(item.payloadJson)
                        val res = api.batchUploadBreadcrumbs(req)
                        isSuccess = res.success
                        if (isSuccess) {
                            database.breadcrumbDao().markAsSynced(item.entityId)
                        }
                    }
                    else -> {
                        // Unknown mutation type, mark as processed to prevent queue deadlock
                        isSuccess = true
                    }
                }

                if (isSuccess) {
                    database.pendingSyncQueueDao().deleteById(item.id)
                } else {
                    anyFailed = true
                    database.pendingSyncQueueDao().incrementAttempt(item.id, "Server returned error response")
                }
            } catch (e: IOException) {
                anyFailed = true
                database.pendingSyncQueueDao().incrementAttempt(item.id, "Network error: ${e.message}")
            } catch (e: Exception) {
                anyFailed = true
                database.pendingSyncQueueDao().incrementAttempt(item.id, "General sync error: ${e.message}")
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}
