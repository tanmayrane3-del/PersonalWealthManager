package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MetalHoldingDto(
    val id: String,
    @SerializedName("metal_type") val metalType: String,
    @SerializedName("sub_type") val subType: String?,
    val label: String,
    @SerializedName("quantity_grams") val quantityGrams: Double,
    val purity: String,
    val notes: String?,
    @SerializedName("current_value") val currentValue: Double,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class MetalRatesDto(
    @SerializedName("gold_22k_per_gram") val gold22kPerGram: Double,
    @SerializedName("gold_24k_per_gram") val gold24kPerGram: Double,
    @SerializedName("fetched_at") val fetchedAt: String
)

data class MetalHoldingsData(
    val holdings: List<MetalHoldingDto>,
    @SerializedName("total_value") val totalValue: Double,
    val rates: MetalRatesDto
)

data class MetalHoldingRequest(
    @SerializedName("metal_type") val metalType: String,
    @SerializedName("sub_type") val subType: String?,
    val label: String,
    @SerializedName("quantity_grams") val quantityGrams: Double,
    val purity: String,
    val notes: String?
)

data class MetalsSummaryDto(
    @SerializedName("total_value")   val totalValue: Double,
    @SerializedName("total_day_pnl") val totalDayPnl: Double,
    @SerializedName("projected_1y")  val projected1y: Double,
    @SerializedName("projected_3y")  val projected3y: Double,
    @SerializedName("projected_5y")  val projected5y: Double,
    @SerializedName("has_cagr")      val hasCagr: Boolean
)
