package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.data.remote.api.NetWorthApiService
import com.example.personalwealthmanager.data.remote.dto.NetWorthCurrentDto
import com.example.personalwealthmanager.data.remote.dto.NetWorthSnapshotDto
import com.example.personalwealthmanager.domain.repository.NetWorthRepository
import org.json.JSONObject
import javax.inject.Inject

class NetWorthRepositoryImpl @Inject constructor(
    private val api: NetWorthApiService
) : NetWorthRepository {

    override suspend fun getCurrent(sessionToken: String): Result<NetWorthCurrentDto> {
        return try {
            val response = api.getCurrent(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No data returned"))
                Result.success(data)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch net worth")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSnapshots(
        sessionToken: String,
        period: String
    ): Result<List<NetWorthSnapshotDto>> {
        return try {
            val response = api.getSnapshots(sessionToken, period)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No data returned"))
                Result.success(data.snapshots)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch snapshots")))
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
