package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "client_ledger_entries",
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["created_at"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "client_id")
    val clientId: String,
    @ColumnInfo(name = "entry_type")
    val entryType: String,
    val amount: Double,
    @ColumnInfo(name = "balance_after")
    val balanceAfter: Double,
    @ColumnInfo(name = "reference_type")
    val referenceType: String = "ORDER",
    @ColumnInfo(name = "reference_id")
    val referenceId: String? = null,
    val description: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String
)
