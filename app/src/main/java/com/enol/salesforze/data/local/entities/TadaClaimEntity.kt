package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tada_claims",
    indices = [
        Index(value = ["attendance_id"]),
        Index(value = ["user_id"]),
        Index(value = ["claim_date"]),
        Index(value = ["status"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AttendanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["attendance_id"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        )
    ]
)
data class TadaClaimEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "attendance_id")
    val attendanceId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "claim_date")
    val claimDate: String,
    @ColumnInfo(name = "start_odometer")
    val startOdometer: Double,
    @ColumnInfo(name = "end_odometer")
    val endOdometer: Double,
    @ColumnInfo(name = "odometer_delta_km")
    val odometerDeltaKm: Double,
    @ColumnInfo(name = "gps_distance_km")
    val gpsDistanceKm: Double,
    @ColumnInfo(name = "variance_percentage")
    val variancePercentage: Double,
    @ColumnInfo(name = "fuel_rate_per_km")
    val fuelRatePerKm: Double,
    @ColumnInfo(name = "fuel_claim_amount")
    val fuelClaimAmount: Double,
    @ColumnInfo(name = "daily_allowance_amount")
    val dailyAllowanceAmount: Double,
    @ColumnInfo(name = "toll_parking_amount")
    val tollParkingAmount: Double = 0.0,
    @ColumnInfo(name = "total_claim_amount")
    val totalClaimAmount: Double,
    val status: String = "SUBMITTED",
    @ColumnInfo(name = "rejection_reason")
    val rejectionReason: String? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
