package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["order_number"], unique = true),
        Index(value = ["client_id"]),
        Index(value = ["sales_rep_id"]),
        Index(value = ["beat_id"]),
        Index(value = ["status"]),
        Index(value = ["idempotency_key"])
    ],
    foreignKeys = [
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
            entity = BeatEntity::class,
            parentColumns = ["id"],
            childColumns = ["beat_id"],
            onDelete = ForeignKey.SET_NULL,
            deferred = true
        )
    ]
)
data class OrderEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "order_number")
    val orderNumber: String,
    @ColumnInfo(name = "client_id")
    val clientId: String,
    @ColumnInfo(name = "sales_rep_id")
    val salesRepId: String,
    @ColumnInfo(name = "beat_id")
    val beatId: String? = null,
    @ColumnInfo(name = "total_cartons")
    val totalCartons: Double = 0.0,
    @ColumnInfo(name = "total_liters")
    val totalLiters: Double = 0.0,
    @ColumnInfo(name = "gross_amount")
    val grossAmount: Double = 0.0,
    @ColumnInfo(name = "discount_amount")
    val discountAmount: Double = 0.0,
    @ColumnInfo(name = "net_amount")
    val netAmount: Double = 0.0,
    @ColumnInfo(name = "payment_terms")
    val paymentTerms: String = "CREDIT_15_DAYS",
    @ColumnInfo(name = "credit_hold_flag")
    val creditHoldFlag: Boolean = false,
    val status: String = "PENDING",
    @ColumnInfo(name = "external_oms_reference")
    val externalOmsReference: String? = null,
    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
