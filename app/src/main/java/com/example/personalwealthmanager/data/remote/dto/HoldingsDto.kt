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
    @SerializedName("last_synced_at") val lastSyncedAt: String?,
    @SerializedName("cagr_1y") val cagr1y: Double?,
    @SerializedName("cagr_3y") val cagr3y: Double?,
    @SerializedName("cagr_5y") val cagr5y: Double?
)

data class HoldingsSyncResponse(
    val synced: Int,
    val holdings: List<StockHoldingDto>
)

data class HoldingsListResponse(
    val holdings: List<StockHoldingDto>
)

data class StocksSummaryDto(
    @SerializedName("total_portfolio_value") val totalPortfolioValue: Double,
    @SerializedName("today_pnl") val todayPnl: Double,
    @SerializedName("projected_1y") val projected1y: Double,
    @SerializedName("projected_3y") val projected3y: Double,
    @SerializedName("projected_5y") val projected5y: Double
)
