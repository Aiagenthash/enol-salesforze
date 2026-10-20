package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "competitions",
    indices = [
        Index(value = ["is_active"])
    ]
)
data class CompetitionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "start_date")
    val startDate: String,
    @ColumnInfo(name = "end_date")
    val endDate: String,
    @ColumnInfo(name = "target_metric")
    val targetMetric: String,
    @ColumnInfo(name = "reward_details_json")
    val rewardDetailsJson: String? = "{}",
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
