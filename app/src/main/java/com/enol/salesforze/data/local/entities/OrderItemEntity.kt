package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    indices = [
        Index(value = ["order_id"]),
        Index(value = ["product_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["order_id"],
            onDelete = ForeignKey.CASCADE,
            deferred = true
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.NO_ACTION,
            deferred = true
        )
    ]
)
data class OrderItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "order_id")
    val orderId: String,
    @ColumnInfo(name = "product_id")
    val productId: String,
    val cartons: Double = 0.0,
    val units: Int = 0,
    @ColumnInfo(name = "pack_size_liters")
    val packSizeLiters: Double,
    @ColumnInfo(name = "total_liters")
    val totalLiters: Double,
    @ColumnInfo(name = "unit_price")
    val unitPrice: Double,
    @ColumnInfo(name = "line_total")
    val lineTotal: Double,
    @ColumnInfo(name = "scheme_discount")
    val schemeDiscount: Double = 0.0,
    @ColumnInfo(name = "net_line_total")
    val netLineTotal: Double
)
