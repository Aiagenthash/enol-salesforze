package com.enol.salesforze.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class OrderWithItems(
    @Embedded
    val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "order_id"
    )
    val items: List<OrderItemEntity>
)
