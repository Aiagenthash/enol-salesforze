package com.enol.salesforze.data.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.remote.api.EnterpriseFieldApi
import com.enol.salesforze.data.remote.models.CreateOrderRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json
import java.io.IOException

@HiltWorker
class OrderSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: EnterpriseFieldApi,
    private val database: AppDatabase,
    private val json: Json
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pendingOrders = database.pendingSyncQueueDao().getPendingByEntityTypeOnce("order")
        if (pendingOrders.isEmpty()) {
            return Result.success()
        }

        var anyFailed = false

        for (queueItem in pendingOrders) {
            try {
                val orderRequest = json.decodeFromString<CreateOrderRequest>(queueItem.payloadJson)

                // Dispatch to backend API with strict X-Idempotency-Key
                val response = api.createOrder(
                    idempotencyKey = queueItem.idempotencyKey,
                    request = orderRequest
                )

                if (response.success && response.data != null) {
                    val serverOrder = response.data
                    database.withTransaction {
                        // Update local order with authoritative server fields
                        database.orderDao().updateOrderStatus(
                            orderId = queueItem.entityId,
                            status = serverOrder.status,
                            updatedAt = serverOrder.updatedAt
                        )
                        // Remove successfully synced item from queue
                        database.pendingSyncQueueDao().deleteById(queueItem.id)
                    }
                } else {
                    anyFailed = true
                    database.pendingSyncQueueDao().incrementAttempt(
                        id = queueItem.id,
                        errorMessage = response.error ?: "Order submission returned unsuccessful response"
                    )
                }
            } catch (e: IOException) {
                anyFailed = true
                database.pendingSyncQueueDao().incrementAttempt(
                    id = queueItem.id,
                    errorMessage = "Network error: ${e.message}"
                )
            } catch (e: Exception) {
                anyFailed = true
                database.pendingSyncQueueDao().incrementAttempt(
                    id = queueItem.id,
                    errorMessage = "Serialization/Processing error: ${e.message}"
                )
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}
