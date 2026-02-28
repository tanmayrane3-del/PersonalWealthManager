package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SmsParseRequest(
    val sender: String,
    val body: String,
    @SerializedName("timestamp_ms") val timestampMs: Long
)

data class SmsParseResult(
    val recorded: Boolean,
    val reason: String?,
    val type: String?,
    @SerializedName("expense_id") val expenseId: String?,
    @SerializedName("income_id") val incomeId: String?,
    val amount: String?,
    val recipient: String?,
    val source: String?,
    val bank: String?,
    @SerializedName("is_unmatched") val isUnmatched: Boolean?
)
