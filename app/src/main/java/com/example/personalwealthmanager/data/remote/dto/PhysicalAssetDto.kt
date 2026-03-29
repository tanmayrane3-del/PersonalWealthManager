package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PhysicalAssetDto(
    val id: String,
    @SerializedName("asset_type") val assetType: String,
    val label: String,
    @SerializedName("purchase_price") val purchasePrice: Double,
    @SerializedName("purchase_date") val purchaseDate: String,
    @SerializedName("current_market_value") val currentMarketValue: Double?,
    @SerializedName("market_value_last_updated") val marketValueLastUpdated: String?,
    @SerializedName("depreciation_rate_pct") val depreciationRatePct: Double?,
    val notes: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("has_active_loan") val hasActiveLoan: Boolean = false,
    @SerializedName("linked_loan_id") val linkedLoanId: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class PhysicalAssetSummaryDto(
    @SerializedName("total_current_value") val totalCurrentValue: Double,
    @SerializedName("proj_1y")             val proj1y: Double = 0.0,
    @SerializedName("proj_3y")             val proj3y: Double = 0.0,
    @SerializedName("proj_5y")             val proj5y: Double = 0.0,
    val assets: List<PhysicalAssetDto>
)

data class PhysicalAssetsListDto(
    val assets: List<PhysicalAssetDto>
)

data class CreatePhysicalAssetRequest(
    @SerializedName("asset_type") val assetType: String,
    val label: String,
    @SerializedName("purchase_price") val purchasePrice: Double,
    @SerializedName("purchase_date") val purchaseDate: String,
    @SerializedName("current_market_value") val currentMarketValue: Double?,
    @SerializedName("depreciation_rate_pct") val depreciationRatePct: Double?,
    val notes: String?
)

data class UpdatePhysicalAssetRequest(
    @SerializedName("asset_type") val assetType: String?,
    val label: String?,
    @SerializedName("purchase_price") val purchasePrice: Double?,
    @SerializedName("purchase_date") val purchaseDate: String?,
    @SerializedName("current_market_value") val currentMarketValue: Double?,
    @SerializedName("depreciation_rate_pct") val depreciationRatePct: Double?,
    val notes: String?
)
