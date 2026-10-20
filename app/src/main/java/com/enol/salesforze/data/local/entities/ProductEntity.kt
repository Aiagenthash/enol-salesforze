package com.enol.salesforze.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["category"]),
        Index(value = ["viscosity_grade"])
    ],
    foreignKeys = []
)
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val sku: String,
    val name: String,
    val description: String? = null,
    val category: String,
    @ColumnInfo(name = "viscosity_grade")
    val viscosityGrade: String,
    @ColumnInfo(name = "pack_size_liters")
    val packSizeLiters: Double,
    @ColumnInfo(name = "units_per_carton")
    val unitsPerCarton: Int = 1,
    @ColumnInfo(name = "trade_price")
    val tradePrice: Double,
    @ColumnInfo(name = "retail_price")
    val retailPrice: Double,
    @ColumnInfo(name = "active_schemes_json")
    val activeSchemesJson: String? = "[]",
    @ColumnInfo(name = "stock_quantity")
    val stockQuantity: Int = 0,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
