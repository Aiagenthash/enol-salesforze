package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mechanics",
    indices = [
        Index(value = ["client_id"]),
        Index(value = ["phone"]),
        Index(value = ["qr_code_id"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        )
    ]
)
data class MechanicEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "client_id")
    val clientId: String? = null,
    val name: String,
    val phone: String,
    val cnic: String? = null,
    val specialty: String = "4-STROKE_MOTORCYCLE",
    @ColumnInfo(name = "wallet_provider")
    val walletProvider: String = "JAZZCASH",
    @ColumnInfo(name = "wallet_account_number")
    val walletAccountNumber: String,
    @ColumnInfo(name = "qr_code_id")
    val qrCodeId: String,
    @ColumnInfo(name = "points_balance")
    val pointsBalance: Int = 0,
    @ColumnInfo(name = "total_scans")
    val totalScans: Int = 0,
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
