package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.data.remote.api.HoldingsApi
import com.example.personalwealthmanager.data.remote.dto.StockHoldingDto
import com.example.personalwealthmanager.domain.model.StockHolding
import com.example.personalwealthmanager.domain.repository.HoldingsRepository
import org.json.JSONObject
import javax.inject.Inject

class HoldingsRepositoryImpl @Inject constructor(
    private val holdingsApi: HoldingsApi
) : HoldingsRepository {

    override suspend fun getHoldings(sessionToken: String): Result<List<StockHolding>> {
        return try {
            val response = holdingsApi.getHoldings(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val holdings = response.body()?.data?.holdings?.map { it.toDomain() } ?: emptyList()
                Result.success(holdings)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch holdings")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncHoldings(sessionToken: String): Result<List<StockHolding>> {
        return try {
            val response = holdingsApi.syncHoldings(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val holdings = response.body()?.data?.holdings?.map { it.toDomain() } ?: emptyList()
                Result.success(holdings)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to sync holdings")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Retrofit puts error bodies in errorBody(), not body(), for non-2xx responses.
    // Parse the "reason" field from the JSON error body.
    private fun parseReason(errorJson: String?, fallback: String): String {
        if (errorJson.isNullOrBlank()) return fallback
        return try {
            JSONObject(errorJson).optString("reason", fallback).ifBlank { fallback }
        } catch (e: Exception) {
            fallback
        }
    }

    private fun StockHoldingDto.toDomain() = StockHolding(
        id = id,
        tradingSymbol = tradingSymbol,
        exchange = exchange,
        quantity = quantity,
        averagePrice = averagePrice,
        lastPrice = lastPrice,
        currentValue = currentValue,
        pnl = pnl,
        pnlPercentage = pnlPercentage,
        dayChange = dayChange,
        dayChangePercentage = dayChangePercentage,
        closePrice = closePrice,
        lastSyncedAt = lastSyncedAt
    )
}
