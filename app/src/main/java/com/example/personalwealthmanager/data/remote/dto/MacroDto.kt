package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MacroSignalDto(
    val month: String,
    @SerializedName("total_score")          val totalScore: Int,
    val signal: String,
    val confidence: String?,                // tier: "very_high","high","moderate","abstain"
    @SerializedName("confidence_pct")       val confidencePct: String?,
    @SerializedName("predicted_direction")  val predictedDirection: String,
    @SerializedName("predicted_return_pct") val predictedReturnPct: Double,
    @SerializedName("target_nifty")         val targetNifty: Int,
    @SerializedName("target_nifty_low")     val targetNiftyLow: Int,
    @SerializedName("target_nifty_high")    val targetNiftyHigh: Int,
    @SerializedName("accuracy_at_score")    val accuracyAtScore: Double?,
    @SerializedName("historical_months")    val historicalMonths: Int?,
    @SerializedName("pct_positive")         val pctPositive: Double?,
    @SerializedName("nifty_close")          val niftyClose: Double,
    @SerializedName("nasdaq_close")         val nasdaqClose: Double?,
    @SerializedName("usd_inr")              val usdInr: Double?,
    @SerializedName("oil_brent")            val oilBrent: Double?,
    @SerializedName("india_vix_high")       val indiaVixHigh: Double?,
    @SerializedName("fii_net_mtd")          val fiiNetMtd: Double?,
    @SerializedName("dii_net_mtd")          val diiNetMtd: Double?,
    @SerializedName("net_flow_mtd")         val netFlowMtd: Double?,
    @SerializedName("fed_rate")             val fedRate: Double?,
    @SerializedName("rbi_rate")             val rbiRate: Double?,
    val dxy: Double?,
    @SerializedName("hsi_close")            val hsiClose: Double?,
    @SerializedName("prev_nasdaq")          val prevNasdaq: Double?,
    @SerializedName("prev_nifty")           val prevNifty: Double?,
    @SerializedName("is_final")             val isFinal: Boolean,
    @SerializedName("final_score")          val finalScore: Int?,
    @SerializedName("final_signal")         val finalSignal: String?,
    @SerializedName("score_locked_at")      val scoreLockedAt: String?,
    @SerializedName("trading_day_n")        val tradingDayN: Int,
    @SerializedName("data_as_of")           val dataAsOf: String?,
    @SerializedName("score_net_flow")       val scoreNetFlow: Int?,
    @SerializedName("score_vix")            val scoreVix: Int?,
    @SerializedName("score_nasdaq")         val scoreNasdaq: Int?,
    @SerializedName("score_inr")            val scoreInr: Int?,
    @SerializedName("score_oil")            val scoreOil: Int?,
    @SerializedName("score_trend")          val scoreTrend: Int?
)

data class MacroHistoryItemDto(
    val month: String,
    @SerializedName("total_score")          val totalScore: Int,
    val signal: String,
    @SerializedName("nifty_close")          val niftyClose: Double,
    @SerializedName("fii_net_mtd")          val fiiNetMtd: Double?,
    @SerializedName("dii_net_mtd")          val diiNetMtd: Double?,
    @SerializedName("net_flow_mtd")         val netFlowMtd: Double?,
    val confidence: String?,
    @SerializedName("trading_day_n")        val tradingDayN: Int,
    @SerializedName("predicted_direction")  val predictedDirection: String?,
    @SerializedName("predicted_return_pct") val predictedReturnPct: Double?,
    @SerializedName("target_nifty")         val targetNifty: Int?,
    @SerializedName("accuracy_at_score")    val accuracyAtScore: Double?,
    @SerializedName("final_score")          val finalScore: Int?,
    @SerializedName("final_direction")      val finalDirection: String?,
    @SerializedName("is_final")             val isFinal: Boolean?,
    @SerializedName("actual_ret_1m")        val actualRet1m: Double?,
    @SerializedName("is_correct")           val isCorrect: Boolean?
)

data class MacroAccuracyDto(
    val score: Int,
    @SerializedName("accuracy_pct")  val accuracyPct: Double?,
    @SerializedName("avg_return")    val avgReturn: Double?,
    @SerializedName("total_months")  val totalMonths: Int,
    @SerializedName("pct_positive")  val pctPositive: Double?
)
