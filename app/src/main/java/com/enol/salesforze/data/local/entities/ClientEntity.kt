package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["beat_id"]),
        Index(value = ["district_id"]),
        Index(value = ["credit_hold"]),
        Index(value = ["approval_status"]),
        Index(value = ["business_name"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = BeatEntity::class,
            parentColumns = ["id"],
            childColumns = ["beat_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        ),
        ForeignKey(
            entity = DistrictEntity::class,
            parentColumns = ["id"],
            childColumns = ["district_id"],
            onDelete = ForeignKey.NO_ACTION,
            deferred = true
        )
    ]
)
data class ClientEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "beat_id")
    val beatId: String? = null,
    @ColumnInfo(name = "district_id")
    val districtId: String,
    @ColumnInfo(name = "business_name")
    val businessName: String,
    @ColumnInfo(name = "owner_name")
    val ownerName: String,
    val phone: String,
    val cnic: String? = null,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "credit_limit")
    val creditLimit: Double = 100000.00,
    @ColumnInfo(name = "outstanding_balance")
    val outstandingBalance: Double = 0.00,
    @ColumnInfo(name = "credit_hold")
    val creditHold: Boolean = false,
    @ColumnInfo(name = "approval_status")
    val approvalStatus: String = "APPROVED",
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
