package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM daily_attendance WHERE id = :id LIMIT 1")
    fun getAttendanceById(id: String): Flow<AttendanceEntity?>

    @Query("SELECT * FROM daily_attendance WHERE id = :id LIMIT 1")
    suspend fun getAttendanceByIdOnce(id: String): AttendanceEntity?

    @Query("SELECT * FROM daily_attendance WHERE user_id = :userId AND date = :date LIMIT 1")
    fun getAttendanceByUserAndDate(userId: String, date: String): Flow<AttendanceEntity?>

    @Query("SELECT * FROM daily_attendance WHERE user_id = :userId AND date = :date LIMIT 1")
    suspend fun getAttendanceByUserAndDateOnce(userId: String, date: String): AttendanceEntity?

    @Query("SELECT * FROM daily_attendance WHERE user_id = :userId AND status = 'ACTIVE' ORDER BY shift_start DESC LIMIT 1")
    fun getActiveAttendanceForUser(userId: String): Flow<AttendanceEntity?>

    @Query("SELECT * FROM daily_attendance WHERE user_id = :userId AND status = 'ACTIVE' ORDER BY shift_start DESC LIMIT 1")
    suspend fun getActiveAttendanceForUserOnce(userId: String): AttendanceEntity?

    @Query("SELECT * FROM daily_attendance WHERE user_id = :userId ORDER BY date DESC")
    fun getAttendanceHistoryForUser(userId: String): Flow<List<AttendanceEntity>>

    @Query("""
        UPDATE daily_attendance SET 
            shift_end = :shiftEnd,
            end_latitude = :endLatitude,
            end_longitude = :endLongitude,
            end_selfie_url = :endSelfieUrl,
            end_odometer_reading = :endOdometer,
            end_odometer_photo_url = :endOdometerPhotoUrl,
            total_hours = :totalHours,
            status = 'COMPLETED',
            updated_at = :updatedAt
        WHERE id = :attendanceId
    """)
    suspend fun endShift(
        attendanceId: String,
        shiftEnd: String,
        endLatitude: Double,
        endLongitude: Double,
        endSelfieUrl: String,
        endOdometer: Double,
        endOdometerPhotoUrl: String,
        totalHours: Double,
        updatedAt: String
    )

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM daily_attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: String)

    @Query("DELETE FROM daily_attendance")
    suspend fun deleteAllAttendance()
}
