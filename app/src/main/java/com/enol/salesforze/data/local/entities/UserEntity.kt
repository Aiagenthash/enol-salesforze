package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["role"]),
        Index(value = ["manager_id"]),
        Index(value = ["hierarchy_path"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["manager_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        )
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    val phone: String,
    val role: String,
    @ColumnInfo(name = "manager_id")
    val managerId: String? = null,
    @ColumnInfo(name = "hierarchy_path")
    val hierarchyPath: String = "/",
    @ColumnInfo(name = "vehicle_type")
    val vehicleType: String = "MOTORCYCLE",
    @ColumnInfo(name = "vehicle_number")
    val vehicleNumber: String? = null,
    @ColumnInfo(name = "fuel_rate_per_km")
    val fuelRatePerKm: Double = 7.50,
    @ColumnInfo(name = "daily_allowance")
    val dailyAllowance: Double = 500.00,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
