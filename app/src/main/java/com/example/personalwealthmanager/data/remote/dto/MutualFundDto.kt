package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MutualFundLotDto(
    val id: String,
    @SerializedName("purchase_date")   val purchaseDate: String,
    val units: Double,
    @SerializedName("purchase_nav")    val purchaseNav: Double,
    @SerializedName("amount_invested") val amountInvested: Double
)

data class MutualFundHoldingDto(
    val isin: String,
    @SerializedName("scheme_code")          val schemeCode: String?,
    @SerializedName("scheme_name")          val schemeName: String,
    @SerializedName("amc_name")             val amcName: String?,
    @SerializedName("total_units")          val totalUnits: Double,
    @SerializedName("avg_nav")              val avgNav: Double,
    @SerializedName("latest_nav")           val latestNav: Double?,
    @SerializedName("latest_nav_date")      val latestNavDate: String?,
    @SerializedName("total_invested")       val totalInvested: Double,
    @SerializedName("current_value")        val currentValue: Double,
    @SerializedName("absolute_return")      val absoluteReturn: Double,
    @SerializedName("absolute_return_pct")  val absoluteReturnPct: Double,
    val xirr: Double?,
    val lots: List<MutualFundLotDto>
)

data class MfPortfolioSummaryDto(
    @SerializedName("total_invested")       val totalInvested: Double,
    @SerializedName("current_value")        val currentValue: Double,
    @SerializedName("absolute_return")      val absoluteReturn: Double,
    @SerializedName("absolute_return_pct")  val absoluteReturnPct: Double
)

data class MfHoldingsData(
    val funds: List<MutualFundHoldingDto>,
    val summary: MfPortfolioSummaryDto
)

data class MfCagrSummaryDto(
    @SerializedName("total_invested")       val totalInvested: Double,
    @SerializedName("current_value")        val currentValue: Double,
    @SerializedName("absolute_return")      val absoluteReturn: Double,
    @SerializedName("absolute_return_pct")  val absoluteReturnPct: Double,
    @SerializedName("projected_1y")         val projected1y: Double,
    @SerializedName("projected_3y")         val projected3y: Double,
    @SerializedName("projected_5y")         val projected5y: Double,
    @SerializedName("has_cagr")             val hasCagr: Boolean
)

data class SchemeLookupDto(
    @SerializedName("scheme_code") val schemeCode: String,
    @SerializedName("scheme_name") val schemeName: String?,
    @SerializedName("amc_name")    val amcName: String?
)

data class CasPreviewLotDto(
    val date: String,
    val units: Double,
    val nav: Double,
    val amount: Double
)

data class CasPreviewFundDto(
    val isin: String,
    @SerializedName("scheme_code")    val schemeCode: String?,
    @SerializedName("scheme_name")    val schemeName: String,
    @SerializedName("amc_name")       val amcName: String?,
    @SerializedName("folio_number")   val folioNumber: String,
    @SerializedName("closing_units")  val closingUnits: Double,
    @SerializedName("amount_invested") val amountInvested: Double,
    @SerializedName("current_value")  val currentValue: Double?,
    @SerializedName("lookup_failed")  val lookupFailed: Boolean,
    val lots: List<CasPreviewLotDto>
)

data class CasPreviewData(
    val funds: List<CasPreviewFundDto>,
    @SerializedName("total_funds") val totalFunds: Int
)

data class ConfirmImportRequest(
    val funds: List<CasPreviewFundDto>
)

data class ImportResultDto(
    val message: String,
    val inserted: Int,
    val deleted: Int
)

data class AddLotRequest(
    val isin: String,
    @SerializedName("scheme_code")    val schemeCode: String?,
    @SerializedName("scheme_name")    val schemeName: String?,
    @SerializedName("amc_name")       val amcName: String?,
    @SerializedName("folio_number")   val folioNumber: String?,
    val units: Double,
    @SerializedName("purchase_nav")   val purchaseNav: Double,
    @SerializedName("purchase_date")  val purchaseDate: String,
    val notes: String?
)
