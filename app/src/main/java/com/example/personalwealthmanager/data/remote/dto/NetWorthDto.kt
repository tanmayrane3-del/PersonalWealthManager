package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NetWorthCurrentDto(
    @SerializedName("total_assets")      val totalAssets: Double,
    @SerializedName("total_liabilities") val totalLiabilities: Double,
    @SerializedName("net_worth")         val netWorth: Double
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
