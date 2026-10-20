package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "beats",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["district_id"]),
        Index(value = ["assigned_rep_id"]),
        Index(value = ["day_of_week"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DistrictEntity::class,
            parentColumns = ["id"],
            childColumns = ["district_id"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["assigned_rep_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        )
    ]
)
data class BeatEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "district_id")
    val districtId: String,
    val name: String,
    val code: String,
    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: String,
    @ColumnInfo(name = "assigned_rep_id")
    val assignedRepId: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
