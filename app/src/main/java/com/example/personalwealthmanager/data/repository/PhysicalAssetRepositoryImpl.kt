package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.data.remote.api.PhysicalAssetApiService
import com.example.personalwealthmanager.data.remote.dto.CreatePhysicalAssetRequest
import com.example.personalwealthmanager.data.remote.dto.PhysicalAssetDto
import com.example.personalwealthmanager.data.remote.dto.UpdatePhysicalAssetRequest
import com.example.personalwealthmanager.domain.model.PhysicalAsset
import com.example.personalwealthmanager.domain.model.PhysicalAssetSummary
import com.example.personalwealthmanager.domain.repository.PhysicalAssetRepository
import org.json.JSONObject
import javax.inject.Inject

class PhysicalAssetRepositoryImpl @Inject constructor(
    private val api: PhysicalAssetApiService
) : PhysicalAssetRepository {

    override suspend fun getAssets(sessionToken: String): Result<List<PhysicalAsset>> {
        return try {
            val response = api.getAssets(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No asset data returned"))
                Result.success(data.assets.map { it.toDomain() })
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch assets")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummary(sessionToken: String): Result<PhysicalAssetSummary> {
        return try {
            val response = api.getSummary(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No summary data returned"))
                Result.success(PhysicalAssetSummary(
                    totalCurrentValue = data.totalCurrentValue,
                    proj1y = data.proj1y,
                    proj3y = data.proj3y,
                    proj5y = data.proj5y,
                    assets = data.assets.map { it.toDomain() }
                ))
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch summary")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createAsset(
        sessionToken: String,
        request: CreatePhysicalAssetRequest
    ): Result<PhysicalAsset> {
        return try {
            val response = api.createAsset(sessionToken, request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No asset data returned"))
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to create asset")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAsset(
        sessionToken: String,
        id: String,
        request: UpdatePhysicalAssetRequest
    ): Result<PhysicalAsset> {
        return try {
            val response = api.updateAsset(sessionToken, id, request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No asset data returned"))
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to update asset")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAsset(sessionToken: String, id: String): Result<Unit> {
        return try {
            val response = api.deleteAsset(sessionToken, id)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                val reason = parseReason(response.errorBody()?.string(), "Failed to delete asset")
                Result.failure(Exception(reason))
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

    private fun PhysicalAssetDto.toDomain() = PhysicalAsset(
        id = id,
        assetType = assetType,
        label = label,
        purchasePrice = purchasePrice,
        purchaseDate = purchaseDate,
        currentMarketValue = currentMarketValue,
        marketValueLastUpdated = marketValueLastUpdated,
        depreciationRatePct = depreciationRatePct,
        notes = notes,
        isActive = isActive,
        hasActiveLoan = hasActiveLoan,
        linkedLoanId = linkedLoanId
    )
}
