package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.DistrictEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistrictDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistrict(district: DistrictEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistricts(districts: List<DistrictEntity>)

    @Update
    suspend fun updateDistrict(district: DistrictEntity)

    @Query("SELECT * FROM districts ORDER BY name ASC")
    fun getAllDistricts(): Flow<List<DistrictEntity>>

    @Query("SELECT * FROM districts WHERE id = :id LIMIT 1")
    fun getDistrictById(id: String): Flow<DistrictEntity?>

    @Query("SELECT * FROM districts WHERE id = :id LIMIT 1")
    suspend fun getDistrictByIdOnce(id: String): DistrictEntity?

    @Query("SELECT * FROM districts WHERE code = :code LIMIT 1")
    suspend fun getDistrictByCode(code: String): DistrictEntity?

    @Query("SELECT * FROM districts WHERE province = :province AND is_active = 1 ORDER BY name ASC")
    fun getDistrictsByProvince(province: String): Flow<List<DistrictEntity>>

    @Query("SELECT * FROM districts WHERE assigned_asm_id = :asmId AND is_active = 1 ORDER BY name ASC")
    fun getDistrictsForAsm(asmId: String): Flow<List<DistrictEntity>>

    @Query("SELECT * FROM districts WHERE assigned_zsm_id = :zsmId AND is_active = 1 ORDER BY name ASC")
    fun getDistrictsForZsm(zsmId: String): Flow<List<DistrictEntity>>

    @Delete
    suspend fun deleteDistrict(district: DistrictEntity)

    @Query("DELETE FROM districts")
    suspend fun deleteAllDistricts()
}
