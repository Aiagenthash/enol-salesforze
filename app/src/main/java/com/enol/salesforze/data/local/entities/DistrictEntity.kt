package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "districts",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["province"]),
        Index(value = ["assigned_zsm_id"]),
        Index(value = ["assigned_asm_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["assigned_zsm_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["assigned_asm_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        )
    ]
)
data class DistrictEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val code: String,
    val province: String,
    @ColumnInfo(name = "polygon_geojson")
    val polygonGeoJson: String? = null,
    @ColumnInfo(name = "assigned_zsm_id")
    val assignedZsmId: String? = null,
    @ColumnInfo(name = "assigned_asm_id")
    val assignedAsmId: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
