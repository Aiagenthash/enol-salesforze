package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.MechanicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MechanicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMechanic(mechanic: MechanicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMechanics(mechanics: List<MechanicEntity>)

    @Update
    suspend fun updateMechanic(mechanic: MechanicEntity)

    @Query("SELECT * FROM mechanics ORDER BY name ASC")
    fun getAllMechanics(): Flow<List<MechanicEntity>>

    @Query("SELECT * FROM mechanics WHERE id = :id LIMIT 1")
    fun getMechanicById(id: String): Flow<MechanicEntity?>

    @Query("SELECT * FROM mechanics WHERE id = :id LIMIT 1")
    suspend fun getMechanicByIdOnce(id: String): MechanicEntity?

    @Query("SELECT * FROM mechanics WHERE qr_code_id = :qrCodeId LIMIT 1")
    fun getMechanicByQrCode(qrCodeId: String): Flow<MechanicEntity?>

    @Query("SELECT * FROM mechanics WHERE qr_code_id = :qrCodeId LIMIT 1")
    suspend fun getMechanicByQrCodeOnce(qrCodeId: String): MechanicEntity?

    @Query("SELECT * FROM mechanics WHERE phone = :phone LIMIT 1")
    suspend fun getMechanicByPhone(phone: String): MechanicEntity?

    @Query("SELECT * FROM mechanics WHERE client_id = :clientId ORDER BY name ASC")
    fun getMechanicsByClient(clientId: String): Flow<List<MechanicEntity>>

    @Query("UPDATE mechanics SET points_balance = points_balance + :pointsDelta, total_scans = total_scans + :scansDelta, updated_at = :updatedAt WHERE id = :id")
    suspend fun updatePoints(id: String, pointsDelta: Int, scansDelta: Int, updatedAt: String)

    @Query("SELECT * FROM mechanics WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMechanics(query: String): Flow<List<MechanicEntity>>

    @Delete
    suspend fun deleteMechanic(mechanic: MechanicEntity)

    @Query("DELETE FROM mechanics WHERE id = :id")
    suspend fun deleteMechanicById(id: String)

    @Query("DELETE FROM mechanics")
    suspend fun deleteAllMechanics()
}
