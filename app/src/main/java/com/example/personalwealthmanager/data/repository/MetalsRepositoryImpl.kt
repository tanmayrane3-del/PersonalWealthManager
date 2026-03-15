package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.data.remote.api.MetalsApi
import com.example.personalwealthmanager.data.remote.dto.MetalHoldingDto
import com.example.personalwealthmanager.data.remote.dto.MetalHoldingRequest
import com.example.personalwealthmanager.data.remote.dto.MetalRatesDto
import com.example.personalwealthmanager.data.remote.dto.MetalsSummaryDto
import com.example.personalwealthmanager.domain.model.MetalHolding
import com.example.personalwealthmanager.domain.model.MetalRates
import com.example.personalwealthmanager.domain.repository.MetalsRepository
import org.json.JSONObject
import javax.inject.Inject

class MetalsRepositoryImpl @Inject constructor(
    private val metalsApi: MetalsApi
) : MetalsRepository {

    override suspend fun getRates(sessionToken: String): Result<MetalRates> {
        return try {
            val response = metalsApi.getRates(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No rate data returned"))
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch rates")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHoldings(sessionToken: String): Result<Triple<List<MetalHolding>, MetalRates, Double>> {
        return try {
            val response = metalsApi.getHoldings(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No holdings data returned"))
                val holdings = data.holdings.map { it.toDomain() }
                val rates = data.rates.toDomain()
                Result.success(Triple(holdings, rates, data.totalValue))
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch holdings")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummary(sessionToken: String): Result<MetalsSummaryDto> {
        return try {
            val response = metalsApi.getSummary(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No summary data returned"))
                Result.success(dto)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch summary")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncCagr(sessionToken: String): Result<Unit> {
        return try {
            val response = metalsApi.syncCagr(sessionToken)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to sync CAGR")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addHolding(sessionToken: String, request: MetalHoldingRequest): Result<MetalHolding> {
        return try {
            val response = metalsApi.addHolding(sessionToken, request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No holding data returned"))
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to add holding")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHolding(sessionToken: String, id: String, request: MetalHoldingRequest): Result<MetalHolding> {
        return try {
            val response = metalsApi.updateHolding(sessionToken, id, request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No holding data returned"))
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to update holding")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHolding(sessionToken: String, id: String): Result<Unit> {
        return try {
            val response = metalsApi.deleteHolding(sessionToken, id)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to delete holding")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseReason(errorJson: String?, fallback: String): String {
        if (errorJson.isNullOrBlank()) return fallback
        return try {
            JSONObject(errorJson).optString("reason", fallback).ifBlank { fallback }
        } catch (e: Exception) {
            fallback
        }
    }

    private fun MetalHoldingDto.toDomain() = MetalHolding(
        id = id,
        metalType = metalType,
        subType = subType,
        label = label,
        quantityGrams = quantityGrams,
        purity = purity,
        notes = notes,
        currentValue = currentValue
    )

    private fun MetalRatesDto.toDomain() = MetalRates(
        gold22kPerGram = gold22kPerGram,
        gold24kPerGram = gold24kPerGram,
        fetchedAt = fetchedAt
    )
}
