package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enol.salesforze.data.local.entities.BreadcrumbEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BreadcrumbDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreadcrumb(breadcrumb: BreadcrumbEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreadcrumbs(breadcrumbs: List<BreadcrumbEntity>)

    @Query("SELECT * FROM location_breadcrumbs WHERE is_synced = 0 ORDER BY recorded_at ASC")
    fun getUnsyncedBreadcrumbs(): Flow<List<BreadcrumbEntity>>

    @Query("SELECT * FROM location_breadcrumbs WHERE is_synced = 0 ORDER BY recorded_at ASC LIMIT :limit")
    suspend fun getUnsyncedBreadcrumbsOnce(limit: Int = 100): List<BreadcrumbEntity>

    @Query("UPDATE location_breadcrumbs SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("SELECT * FROM location_breadcrumbs WHERE attendance_id = :attendanceId ORDER BY recorded_at ASC")
    fun getBreadcrumbsByAttendance(attendanceId: String): Flow<List<BreadcrumbEntity>>

    @Query("SELECT * FROM location_breadcrumbs WHERE attendance_id = :attendanceId ORDER BY recorded_at ASC")
    suspend fun getBreadcrumbsByAttendanceOnce(attendanceId: String): List<BreadcrumbEntity>

    @Query("SELECT * FROM location_breadcrumbs WHERE user_id = :userId ORDER BY recorded_at DESC")
    fun getBreadcrumbsByUser(userId: String): Flow<List<BreadcrumbEntity>>

    @Query("DELETE FROM location_breadcrumbs WHERE is_synced = 1 AND recorded_at < :olderThanTimestamp")
    suspend fun deleteSyncedBreadcrumbsOlderThan(olderThanTimestamp: String)

    @Delete
    suspend fun deleteBreadcrumb(breadcrumb: BreadcrumbEntity)

    @Query("DELETE FROM location_breadcrumbs")
    suspend fun deleteAllBreadcrumbs()
}
