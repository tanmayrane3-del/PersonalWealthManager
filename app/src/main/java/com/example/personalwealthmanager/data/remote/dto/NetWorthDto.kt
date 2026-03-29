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
    @SerializedName("cagr_3y")             val cagr3y: Double = 0.0,
    @SerializedName("cagr_5y")             val cagr5y: Double = 0.0,
    @SerializedName("stocks_count")        val stocksCount: Int = 0,
    @SerializedName("stocks_value")        val stocksValue: Double = 0.0,
    @SerializedName("stocks_proj_1y")      val stocksProj1y: Double = 0.0,
    @SerializedName("stocks_proj_3y")      val stocksProj3y: Double = 0.0,
    @SerializedName("stocks_proj_5y")      val stocksProj5y: Double = 0.0,
    @SerializedName("mf_count")            val mfCount: Int = 0,
    @SerializedName("mf_value")            val mfValue: Double = 0.0,
    @SerializedName("mf_proj_1y")          val mfProj1y: Double = 0.0,
    @SerializedName("mf_proj_3y")          val mfProj3y: Double = 0.0,
    @SerializedName("mf_proj_5y")          val mfProj5y: Double = 0.0,
    @SerializedName("metals_value")        val metalsValue: Double = 0.0,
    @SerializedName("metals_proj_1y")      val metalsProj1y: Double = 0.0,
    @SerializedName("metals_proj_3y")      val metalsProj3y: Double = 0.0,
    @SerializedName("metals_proj_5y")      val metalsProj5y: Double = 0.0,
    @SerializedName("other_assets_count")  val otherAssetsCount: Int = 0,
    @SerializedName("other_value")         val otherValue: Double = 0.0,
    @SerializedName("other_proj_1y")       val otherProj1y: Double = 0.0,
    @SerializedName("other_proj_3y")       val otherProj3y: Double = 0.0,
    @SerializedName("other_proj_5y")       val otherProj5y: Double = 0.0,
    @SerializedName("liab_proj_1y")        val liabProj1y: Double = 0.0,
    @SerializedName("liab_proj_3y")        val liabProj3y: Double = 0.0,
    @SerializedName("liab_proj_5y")        val liabProj5y: Double = 0.0,
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
