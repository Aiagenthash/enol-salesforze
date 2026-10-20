package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.PendingSyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: PendingSyncQueueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItems(items: List<PendingSyncQueueEntity>)

    @Update
    suspend fun updateSyncItem(item: PendingSyncQueueEntity)

    @Query("SELECT * FROM pending_sync_queue ORDER BY created_at ASC")
    fun getPendingItems(): Flow<List<PendingSyncQueueEntity>>

    @Query("SELECT * FROM pending_sync_queue ORDER BY created_at ASC")
    suspend fun getPendingItemsOnce(): List<PendingSyncQueueEntity>

    @Query("SELECT * FROM pending_sync_queue ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingItemsBatch(limit: Int): List<PendingSyncQueueEntity>

    @Query("SELECT * FROM pending_sync_queue WHERE entity_type = :entityType ORDER BY created_at ASC")
    fun getPendingItemsByEntityType(entityType: String): Flow<List<PendingSyncQueueEntity>>

    @Query("SELECT * FROM pending_sync_queue WHERE idempotency_key = :key LIMIT 1")
    suspend fun getPendingItemByIdempotencyKey(key: String): PendingSyncQueueEntity?

    @Query("SELECT * FROM pending_sync_queue WHERE id = :id LIMIT 1")
    suspend fun getPendingItemById(id: String): PendingSyncQueueEntity?

    @Query("SELECT COUNT(*) FROM pending_sync_queue")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync_queue")
    suspend fun getPendingCountOnce(): Int

    @Query("UPDATE pending_sync_queue SET attempt_count = attempt_count + 1, last_attempt_at = :lastAttemptAt, error_message = :errorMessage WHERE id = :id")
    suspend fun incrementAttemptCount(id: String, lastAttemptAt: String, errorMessage: String?)

    @Delete
    suspend fun deleteSyncItem(item: PendingSyncQueueEntity)

    @Query("DELETE FROM pending_sync_queue WHERE id = :id")
    suspend fun deleteSyncItemById(id: String)

    @Query("DELETE FROM pending_sync_queue WHERE id IN (:ids)")
    suspend fun deleteSyncItemsByIds(ids: List<String>)

    @Query("DELETE FROM pending_sync_queue")
    suspend fun deleteAllSyncItems()
}
