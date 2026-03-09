package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StockHoldingDto(
    val id: String,
    @SerializedName("tradingsymbol") val tradingSymbol: String,
    val exchange: String,
    val quantity: Int,
    @SerializedName("average_price") val averagePrice: Double,
    @SerializedName("last_price") val lastPrice: Double,
    @SerializedName("current_value") val currentValue: Double,
    val pnl: Double,
    @SerializedName("pnl_percentage") val pnlPercentage: Double,
    @SerializedName("day_change") val dayChange: Double,
    @SerializedName("day_change_percentage") val dayChangePercentage: Double,
    @SerializedName("close_price") val closePrice: Double,
    @SerializedName("last_synced_at") val lastSyncedAt: String?
)

data class HoldingsSyncResponse(
    val synced: Int,
    val holdings: List<StockHoldingDto>
)

data class HoldingsListResponse(
    val holdings: List<StockHoldingDto>
)
