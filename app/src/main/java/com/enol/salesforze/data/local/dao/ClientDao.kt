package com.enol.salesforze.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enol.salesforze.data.local.entities.ClientEntity
import com.enol.salesforze.data.local.entities.LedgerEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientEntity>)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("SELECT * FROM clients ORDER BY business_name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    fun getClientById(id: String): Flow<ClientEntity?>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientByIdOnce(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE beat_id = :beatId ORDER BY business_name ASC")
    fun getClientsByBeat(beatId: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE district_id = :districtId ORDER BY business_name ASC")
    fun getClientsByDistrict(districtId: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE business_name LIKE '%' || :query || '%' OR owner_name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY business_name ASC")
    fun searchClients(query: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE credit_hold = 1 ORDER BY outstanding_balance DESC")
    fun getClientsOnCreditHold(): Flow<List<ClientEntity>>

    @Query("UPDATE clients SET outstanding_balance = :newBalance, credit_hold = :creditHold, updated_at = :updatedAt WHERE id = :clientId")
    suspend fun updateClientBalance(clientId: String, newBalance: Double, creditHold: Boolean, updatedAt: String)

    @Query("UPDATE clients SET approval_status = :status, updated_at = :updatedAt WHERE id = :clientId")
    suspend fun updateClientApprovalStatus(clientId: String, status: String, updatedAt: String)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClientById(id: String)

    @Query("DELETE FROM clients")
    suspend fun deleteAllClients()

    // --- Client Ledger Entries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<LedgerEntryEntity>)

    @Query("SELECT * FROM client_ledger_entries WHERE client_id = :clientId ORDER BY created_at DESC")
    fun getLedgerEntriesForClient(clientId: String): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM client_ledger_entries WHERE client_id = :clientId ORDER BY created_at DESC")
    suspend fun getLedgerEntriesForClientOnce(clientId: String): List<LedgerEntryEntity>

    @Query("DELETE FROM client_ledger_entries WHERE client_id = :clientId")
    suspend fun deleteLedgerEntriesForClient(clientId: String)
}
