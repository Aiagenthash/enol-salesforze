package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: VisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Query("SELECT * FROM visits ORDER BY check_in_time DESC")
    fun getAllVisits(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    fun getVisitById(id: String): Flow<VisitEntity?>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    suspend fun getVisitByIdOnce(id: String): VisitEntity?

    @Query("SELECT * FROM visits WHERE attendance_id = :attendanceId ORDER BY check_in_time ASC")
    fun getVisitsByAttendance(attendanceId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE attendance_id = :attendanceId ORDER BY check_in_time ASC")
    suspend fun getVisitsByAttendanceOnce(attendanceId: String): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE client_id = :clientId ORDER BY check_in_time DESC")
    fun getVisitsByClient(clientId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE sales_rep_id = :salesRepId ORDER BY check_in_time DESC")
    fun getVisitsByRep(salesRepId: String): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE sales_rep_id = :repId AND check_in_time LIKE :datePrefix || '%' ORDER BY check_in_time ASC")
    fun getVisitsByDate(repId: String, datePrefix: String): Flow<List<VisitEntity>>

    @Query("""
        UPDATE visits SET 
            check_out_time = :checkOutTime,
            check_out_latitude = :checkOutLat,
            check_out_longitude = :checkOutLng,
            outcome = :outcome,
            notes = :notes,
            recovery_slip_number = :slipNumber,
            recovery_amount = :recoveryAmount,
            order_id = :orderId,
            updated_at = :updatedAt
        WHERE id = :visitId
    """)
    suspend fun checkOutVisit(
        visitId: String,
        checkOutTime: String,
        checkOutLat: Double,
        checkOutLng: Double,
        outcome: String,
        notes: String?,
        slipNumber: String?,
        recoveryAmount: Double?,
        orderId: String?,
        updatedAt: String
    )

    @Delete
    suspend fun deleteVisit(visit: VisitEntity)

    @Query("DELETE FROM visits WHERE id = :id")
    suspend fun deleteVisitById(id: String)

    @Query("DELETE FROM visits")
    suspend fun deleteAllVisits()
}
