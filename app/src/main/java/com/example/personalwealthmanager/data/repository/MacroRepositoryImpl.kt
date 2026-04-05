package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.data.remote.api.MacroApi
import com.example.personalwealthmanager.data.remote.dto.MacroAccuracyDto
import com.example.personalwealthmanager.data.remote.dto.MacroBacktestResponseDto
import com.example.personalwealthmanager.data.remote.dto.MacroHistoryItemDto
import com.example.personalwealthmanager.data.remote.dto.MacroSignalDto
import com.example.personalwealthmanager.domain.repository.MacroRepository
import org.json.JSONObject
import javax.inject.Inject

class MacroRepositoryImpl @Inject constructor(
    private val macroApi: MacroApi
) : MacroRepository {

    override suspend fun getSignal(sessionToken: String): Result<MacroSignalDto?> {
        return try {
            val response = macroApi.getSignal(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(response.body()?.data)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch signal")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistory(sessionToken: String): Result<List<MacroHistoryItemDto>> {
        return try {
            val response = macroApi.getHistory(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch history")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAccuracy(sessionToken: String): Result<List<MacroAccuracyDto>> {
        return try {
            val response = macroApi.getAccuracy(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch accuracy")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBacktest(sessionToken: String): Result<MacroBacktestResponseDto> {
        return try {
            val response = macroApi.getBacktest(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("Empty backtest response"))
                Result.success(data)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch backtest")))
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
}
