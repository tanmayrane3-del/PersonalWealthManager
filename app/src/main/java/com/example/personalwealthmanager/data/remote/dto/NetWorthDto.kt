package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NetWorthCurrentDto(
    @SerializedName("total_assets")        val totalAssets: Double,
    @SerializedName("total_liabilities")   val totalLiabilities: Double,
    @SerializedName("net_worth")           val netWorth: Double,
    @SerializedName("day_change")          val dayChange: Double = 0.0,
    @SerializedName("day_change_pct")      val dayChangePct: Double = 0.0,
    @SerializedName("projected_1y")        val projected1y: Double = 0.0,
    @SerializedName("projected_3y")        val projected3y: Double = 0.0,
    @SerializedName("projected_5y")        val projected5y: Double = 0.0,
    @SerializedName("cagr_1y")             val cagr1y: Double = 0.0,
    @SerializedName("stocks_count")        val stocksCount: Int = 0,
    @SerializedName("stocks_proj_1y")      val stocksProj1y: Double = 0.0,
    @SerializedName("stocks_proj_3y")      val stocksProj3y: Double = 0.0,
    @SerializedName("stocks_proj_5y")      val stocksProj5y: Double = 0.0,
    @SerializedName("mf_count")            val mfCount: Int = 0,
    @SerializedName("other_assets_count")  val otherAssetsCount: Int = 0,
)

data class NetWorthSnapshotDto(
    @SerializedName("snapshot_date")     val snapshotDate: String,
    @SerializedName("total_assets")      val totalAssets: Double,
    @SerializedName("total_liabilities") val totalLiabilities: Double,
    @SerializedName("net_worth")         val netWorth: Double
)

data class NetWorthSnapshotsDto(
    @SerializedName("snapshots") val snapshots: List<NetWorthSnapshotDto>
)
