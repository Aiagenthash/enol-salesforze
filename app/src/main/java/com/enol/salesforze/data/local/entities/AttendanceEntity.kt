package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_attendance",
    indices = [
        Index(value = ["user_id", "date"], unique = true),
        Index(value = ["user_id"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
data class AttendanceEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val date: String,
    @ColumnInfo(name = "shift_start")
    val shiftStart: String,
    @ColumnInfo(name = "shift_end")
    val shiftEnd: String? = null,
    @ColumnInfo(name = "start_latitude")
    val startLatitude: Double,
    @ColumnInfo(name = "start_longitude")
    val startLongitude: Double,
    @ColumnInfo(name = "start_selfie_url")
    val startSelfieUrl: String,
    @ColumnInfo(name = "start_odometer_reading")
    val startOdometerReading: Double,
    @ColumnInfo(name = "start_odometer_photo_url")
    val startOdometerPhotoUrl: String,
    @ColumnInfo(name = "end_latitude")
    val endLatitude: Double? = null,
    @ColumnInfo(name = "end_longitude")
    val endLongitude: Double? = null,
    @ColumnInfo(name = "end_selfie_url")
    val endSelfieUrl: String? = null,
    @ColumnInfo(name = "end_odometer_reading")
    val endOdometerReading: Double? = null,
    @ColumnInfo(name = "end_odometer_photo_url")
    val endOdometerPhotoUrl: String? = null,
    @ColumnInfo(name = "total_hours")
    val totalHours: Double? = null,
    val status: String = "ACTIVE",
    val notes: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
