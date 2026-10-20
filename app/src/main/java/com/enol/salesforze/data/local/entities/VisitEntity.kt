package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visits",
    indices = [
        Index(value = ["sales_rep_id"]),
        Index(value = ["client_id"]),
        Index(value = ["attendance_id"]),
        Index(value = ["check_in_time"]),
        Index(value = ["order_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AttendanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["attendance_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        ),
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["client_id"],
            onDelete = ForeignKey.NO_ACTION,
            deferred = true
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["sales_rep_id"],
            onDelete = ForeignKey.NO_ACTION,
            deferred = true
        ),
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        )
    ]
)
data class VisitEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "attendance_id")
    val attendanceId: String? = null,
    @ColumnInfo(name = "client_id")
    val clientId: String,
    @ColumnInfo(name = "sales_rep_id")
    val salesRepId: String,
    @ColumnInfo(name = "scheduled_at")
    val scheduledAt: String? = null,
    @ColumnInfo(name = "check_in_time")
    val checkInTime: String,
    @ColumnInfo(name = "check_out_time")
    val checkOutTime: String? = null,
    @ColumnInfo(name = "check_in_latitude")
    val checkInLatitude: Double,
    @ColumnInfo(name = "check_in_longitude")
    val checkInLongitude: Double,
    @ColumnInfo(name = "check_out_latitude")
    val checkOutLatitude: Double? = null,
    @ColumnInfo(name = "check_out_longitude")
    val checkOutLongitude: Double? = null,
    @ColumnInfo(name = "geofence_distance_meters")
    val geofenceDistanceMeters: Double? = null,
    @ColumnInfo(name = "is_mock_gps")
    val isMockGps: Boolean = false,
    val outcome: String,
    val notes: String? = null,
    @ColumnInfo(name = "recovery_slip_number")
    val recoverySlipNumber: String? = null,
    @ColumnInfo(name = "recovery_amount")
    val recoveryAmount: Double? = 0.0,
    @ColumnInfo(name = "order_id")
    val orderId: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
