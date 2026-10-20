package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.TargetKpiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetKpiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetKpi(target: TargetKpiEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetKpis(targets: List<TargetKpiEntity>)

    @Update
    suspend fun updateTargetKpi(target: TargetKpiEntity)

    @Query("SELECT * FROM targets_kpis WHERE id = :id LIMIT 1")
    fun getTargetKpiById(id: String): Flow<TargetKpiEntity?>

    @Query("SELECT * FROM targets_kpis WHERE id = :id LIMIT 1")
    suspend fun getTargetKpiByIdOnce(id: String): TargetKpiEntity?

    @Query("SELECT * FROM targets_kpis WHERE user_id = :userId ORDER BY period_start DESC")
    fun getTargetsForUser(userId: String): Flow<List<TargetKpiEntity>>

    @Query("SELECT * FROM targets_kpis WHERE user_id = :userId ORDER BY period_start DESC")
    suspend fun getTargetsForUserOnce(userId: String): List<TargetKpiEntity>

    @Query("SELECT * FROM targets_kpis WHERE user_id = :userId AND period_type = :periodType AND :currentDate BETWEEN period_start AND period_end LIMIT 1")
    fun getCurrentTargetForUser(userId: String, periodType: String, currentDate: String): Flow<TargetKpiEntity?>

    @Query("SELECT * FROM targets_kpis WHERE user_id = :userId AND period_type = :periodType AND :currentDate BETWEEN period_start AND period_end LIMIT 1")
    suspend fun getCurrentTargetForUserOnce(userId: String, periodType: String, currentDate: String): TargetKpiEntity?

    @Delete
    suspend fun deleteTargetKpi(target: TargetKpiEntity)

    @Query("DELETE FROM targets_kpis WHERE id = :id")
    suspend fun deleteTargetKpiById(id: String)

    @Query("DELETE FROM targets_kpis")
    suspend fun deleteAllTargetKpis()
}
