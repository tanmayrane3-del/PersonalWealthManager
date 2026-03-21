package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LiabilityDto(
    val id: String,
    @SerializedName("loan_type") val loanType: String,
    @SerializedName("lender_name") val lenderName: String,
    @SerializedName("loan_account_number") val loanAccountNumber: String?,
    @SerializedName("interest_type") val interestType: String,
    @SerializedName("interest_rate") val interestRate: Double,
    @SerializedName("original_amount") val originalAmount: Double,
    @SerializedName("outstanding_principal") val outstandingPrincipal: Double,
    @SerializedName("emi_amount") val emiAmount: Double,
    @SerializedName("emi_due_day") val emiDueDay: Int?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("tenure_months") val tenureMonths: Int,
    @SerializedName("physical_asset_id") val physicalAssetId: String?,
    @SerializedName("asset_label") val assetLabel: String?,
    @SerializedName("asset_type") val assetType: String?,
    val status: String,
    val notes: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class LiabilitiesListDto(
    val liabilities: List<LiabilityDto>
)

data class LiabilitySummaryDto(
    @SerializedName("total_outstanding") val totalOutstanding: Double,
    @SerializedName("total_emi") val totalEmi: Double,
    @SerializedName("active_count") val activeCount: Int,
    val liabilities: List<LiabilityDto>
)

data class CreateLiabilityRequest(
    @SerializedName("loan_type") val loanType: String,
    @SerializedName("lender_name") val lenderName: String,
    @SerializedName("loan_account_number") val loanAccountNumber: String?,
    @SerializedName("interest_type") val interestType: String,
    @SerializedName("interest_rate") val interestRate: Double,
    @SerializedName("original_amount") val originalAmount: Double,
    @SerializedName("outstanding_principal") val outstandingPrincipal: Double,
    @SerializedName("emi_amount") val emiAmount: Double,
    @SerializedName("emi_due_day") val emiDueDay: Int?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("tenure_months") val tenureMonths: Int,
    @SerializedName("physical_asset_id") val physicalAssetId: String?,
    val notes: String?
)

data class UpdateLiabilityRequest(
    @SerializedName("loan_type") val loanType: String?,
    @SerializedName("lender_name") val lenderName: String?,
    @SerializedName("loan_account_number") val loanAccountNumber: String?,
    @SerializedName("interest_type") val interestType: String?,
    @SerializedName("interest_rate") val interestRate: Double?,
    @SerializedName("original_amount") val originalAmount: Double?,
    @SerializedName("outstanding_principal") val outstandingPrincipal: Double?,
    @SerializedName("emi_amount") val emiAmount: Double?,
    @SerializedName("emi_due_day") val emiDueDay: Int?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("tenure_months") val tenureMonths: Int?,
    @SerializedName("physical_asset_id") val physicalAssetId: String?,
    val status: String?,
    val notes: String?
)
