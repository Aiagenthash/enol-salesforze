package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.TadaClaimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TadaClaimDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaim(claim: TadaClaimEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaims(claims: List<TadaClaimEntity>)

    @Update
    suspend fun updateClaim(claim: TadaClaimEntity)

    @Query("SELECT * FROM tada_claims WHERE id = :id LIMIT 1")
    fun getClaimById(id: String): Flow<TadaClaimEntity?>

    @Query("SELECT * FROM tada_claims WHERE id = :id LIMIT 1")
    suspend fun getClaimByIdOnce(id: String): TadaClaimEntity?

    @Query("SELECT * FROM tada_claims WHERE attendance_id = :attendanceId LIMIT 1")
    fun getClaimByAttendanceId(attendanceId: String): Flow<TadaClaimEntity?>

    @Query("SELECT * FROM tada_claims WHERE attendance_id = :attendanceId LIMIT 1")
    suspend fun getClaimByAttendanceIdOnce(attendanceId: String): TadaClaimEntity?

    @Query("SELECT * FROM tada_claims WHERE user_id = :userId ORDER BY claim_date DESC")
    fun getClaimsByUser(userId: String): Flow<List<TadaClaimEntity>>

    @Query("SELECT * FROM tada_claims WHERE status = :status ORDER BY claim_date DESC")
    fun getClaimsByStatus(status: String): Flow<List<TadaClaimEntity>>

    @Query("UPDATE tada_claims SET status = :status, rejection_reason = :rejectionReason, updated_at = :updatedAt WHERE id = :claimId")
    suspend fun updateClaimStatus(claimId: String, status: String, rejectionReason: String?, updatedAt: String)

    @Delete
    suspend fun deleteClaim(claim: TadaClaimEntity)

    @Query("DELETE FROM tada_claims WHERE id = :id")
    suspend fun deleteClaimById(id: String)

    @Query("DELETE FROM tada_claims")
    suspend fun deleteAllClaims()
}
