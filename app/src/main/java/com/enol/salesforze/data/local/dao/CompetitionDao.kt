package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.CompetitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompetitionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetition(competition: CompetitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitions(competitions: List<CompetitionEntity>)

    @Update
    suspend fun updateCompetition(competition: CompetitionEntity)

    @Query("SELECT * FROM competitions ORDER BY start_date DESC")
    fun getAllCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions WHERE is_active = 1 ORDER BY start_date DESC")
    fun getActiveCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions WHERE id = :id LIMIT 1")
    fun getCompetitionById(id: String): Flow<CompetitionEntity?>

    @Query("SELECT * FROM competitions WHERE id = :id LIMIT 1")
    suspend fun getCompetitionByIdOnce(id: String): CompetitionEntity?

    @Delete
    suspend fun deleteCompetition(competition: CompetitionEntity)

    @Query("DELETE FROM competitions WHERE id = :id")
    suspend fun deleteCompetitionById(id: String)

    @Query("DELETE FROM competitions")
    suspend fun deleteAllCompetitions()
}
