package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.BeatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BeatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeat(beat: BeatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeats(beats: List<BeatEntity>)

    @Update
    suspend fun updateBeat(beat: BeatEntity)

    @Query("SELECT * FROM beats ORDER BY name ASC")
    fun getAllBeats(): Flow<List<BeatEntity>>

    @Query("SELECT * FROM beats WHERE id = :id LIMIT 1")
    fun getBeatById(id: String): Flow<BeatEntity?>

    @Query("SELECT * FROM beats WHERE id = :id LIMIT 1")
    suspend fun getBeatByIdOnce(id: String): BeatEntity?

    @Query("SELECT * FROM beats WHERE code = :code LIMIT 1")
    suspend fun getBeatByCode(code: String): BeatEntity?

    @Query("SELECT * FROM beats WHERE district_id = :districtId AND is_active = 1 ORDER BY name ASC")
    fun getBeatsByDistrict(districtId: String): Flow<List<BeatEntity>>

    @Query("SELECT * FROM beats WHERE assigned_rep_id = :repId AND is_active = 1 ORDER BY name ASC")
    fun getBeatsByRep(repId: String): Flow<List<BeatEntity>>

    @Query("SELECT * FROM beats WHERE day_of_week = :dayOfWeek AND is_active = 1 ORDER BY name ASC")
    fun getBeatsByDay(dayOfWeek: String): Flow<List<BeatEntity>>

    @Query("SELECT * FROM beats WHERE assigned_rep_id = :repId AND day_of_week = :dayOfWeek AND is_active = 1 ORDER BY name ASC")
    fun getBeatsForRepAndDay(repId: String, dayOfWeek: String): Flow<List<BeatEntity>>

    @Delete
    suspend fun deleteBeat(beat: BeatEntity)

    @Query("DELETE FROM beats WHERE id = :id")
    suspend fun deleteBeatById(id: String)

    @Query("DELETE FROM beats")
    suspend fun deleteAllBeats()
}
