package com.enol.salesforze.data.local.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductScheme(
    @SerialName("type")
    val type: String = "",
    @SerialName("buy_cartons")
    val buyCartons: Double? = null,
    @SerialName("free_cartons")
    val freeCartons: Double? = null,
    @SerialName("free_units")
    val freeUnits: Int? = null,
    @SerialName("min_cartons")
    val minCartons: Double? = null,
    @SerialName("discount_pct")
    val discountPct: Double? = null,
    @SerialName("per_carton_pkr")
    val perCartonPkr: Double? = null,
    @SerialName("description")
    val description: String? = null
)
