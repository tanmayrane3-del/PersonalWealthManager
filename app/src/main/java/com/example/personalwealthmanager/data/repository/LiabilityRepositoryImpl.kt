package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.data.remote.api.LiabilityApiService
import com.example.personalwealthmanager.data.remote.dto.CreateLiabilityRequest
import com.example.personalwealthmanager.data.remote.dto.LiabilityDto
import com.example.personalwealthmanager.data.remote.dto.UpdateLiabilityRequest
import com.example.personalwealthmanager.domain.model.Liability
import com.example.personalwealthmanager.domain.model.LiabilitySummary
import com.example.personalwealthmanager.domain.repository.LiabilityRepository
import org.json.JSONObject
import javax.inject.Inject

class LiabilityRepositoryImpl @Inject constructor(
    private val api: LiabilityApiService
) : LiabilityRepository {

    override suspend fun getLiabilities(sessionToken: String): Result<List<Liability>> {
        return try {
            val response = api.getLiabilities(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No data returned"))
                Result.success(data.liabilities.map { it.toDomain() })
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch liabilities")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummary(sessionToken: String): Result<LiabilitySummary> {
        return try {
            val response = api.getSummary(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No summary data returned"))
                Result.success(LiabilitySummary(
                    totalOutstanding = data.totalOutstanding,
                    totalEmi = data.totalEmi,
                    activeCount = data.activeCount,
                    liabilities = data.liabilities.map { it.toDomain() }
                ))
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch summary")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createLiability(
        sessionToken: String,
        request: CreateLiabilityRequest
    ): Result<Liability> {
        return try {
            val response = api.createLiability(sessionToken, request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No data returned"))
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to create liability")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLiability(
        sessionToken: String,
        id: String,
        request: UpdateLiabilityRequest
    ): Result<Liability> {
        return try {
            val response = api.updateLiability(sessionToken, id, request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No data returned"))
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to update liability")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLiability(sessionToken: String, id: String): Result<Unit> {
        return try {
            val response = api.deleteLiability(sessionToken, id)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to delete liability")))
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

    private fun LiabilityDto.toDomain() = Liability(
        id = id,
        loanType = loanType,
        lenderName = lenderName,
        loanAccountNumber = loanAccountNumber,
        interestType = interestType,
        interestRate = interestRate,
        originalAmount = originalAmount,
        outstandingPrincipal = outstandingPrincipal,
        emiAmount = emiAmount,
        emiDueDay = emiDueDay,
        startDate = startDate,
        tenureMonths = tenureMonths,
        physicalAssetId = physicalAssetId,
        assetLabel = assetLabel,
        assetType = assetType,
        status = status,
        notes = notes
    )
}
