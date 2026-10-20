package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "targets_kpis",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["period_start", "period_end"])
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
data class TargetKpiEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "period_type")
    val periodType: String,
    @ColumnInfo(name = "period_start")
    val periodStart: String,
    @ColumnInfo(name = "period_end")
    val periodEnd: String,
    @ColumnInfo(name = "target_volume_liters")
    val targetVolumeLiters: Double = 0.0,
    @ColumnInfo(name = "achieved_volume_liters")
    val achievedVolumeLiters: Double = 0.0,
    @ColumnInfo(name = "target_revenue")
    val targetRevenue: Double = 0.0,
    @ColumnInfo(name = "achieved_revenue")
    val achievedRevenue: Double = 0.0,
    @ColumnInfo(name = "target_visits")
    val targetVisits: Int = 0,
    @ColumnInfo(name = "achieved_visits")
    val achievedVisits: Int = 0,
    @ColumnInfo(name = "target_new_clients")
    val targetNewClients: Int = 0,
    @ColumnInfo(name = "achieved_new_clients")
    val achievedNewClients: Int = 0,
    @ColumnInfo(name = "calculated_kpi_score")
    val calculatedKpiScore: Double = 0.0,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
