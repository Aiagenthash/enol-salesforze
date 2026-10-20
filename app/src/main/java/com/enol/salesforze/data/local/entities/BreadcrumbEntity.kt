package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "location_breadcrumbs",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["attendance_id"]),
        Index(value = ["recorded_at"]),
        Index(value = ["is_synced"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        ),
        ForeignKey(
            entity = AttendanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["attendance_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        )
    ]
)
data class BreadcrumbEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "attendance_id")
    val attendanceId: String? = null,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "accuracy_meters")
    val accuracyMeters: Double = 5.0,
    val speed: Double? = null,
    val bearing: Double? = null,
    @ColumnInfo(name = "battery_level")
    val batteryLevel: Double? = null,
    @ColumnInfo(name = "is_mock_gps")
    val isMockGps: Boolean = false,
    @ColumnInfo(name = "recorded_at")
    val recordedAt: String,
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)
