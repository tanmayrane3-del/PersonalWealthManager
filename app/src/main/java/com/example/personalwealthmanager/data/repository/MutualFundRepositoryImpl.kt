package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.data.remote.api.MutualFundApi
import com.example.personalwealthmanager.data.remote.dto.AddLotRequest
import com.example.personalwealthmanager.data.remote.dto.CasPreviewData
import com.example.personalwealthmanager.data.remote.dto.ConfirmImportRequest
import com.example.personalwealthmanager.data.remote.dto.ImportResultDto
import com.example.personalwealthmanager.data.remote.dto.MfCagrSummaryDto
import com.example.personalwealthmanager.data.remote.dto.MutualFundHoldingDto
import com.example.personalwealthmanager.data.remote.dto.MutualFundLotDto
import com.example.personalwealthmanager.domain.model.MutualFundHolding
import com.example.personalwealthmanager.domain.model.MutualFundLot
import com.example.personalwealthmanager.domain.model.MutualFundPortfolioSummary
import com.example.personalwealthmanager.domain.model.SchemeLookupResult
import com.example.personalwealthmanager.domain.repository.MutualFundRepository
import okhttp3.MultipartBody
import org.json.JSONObject
import javax.inject.Inject

class MutualFundRepositoryImpl @Inject constructor(
    private val mutualFundApi: MutualFundApi
) : MutualFundRepository {

    override suspend fun lookupScheme(sessionToken: String, isin: String): Result<SchemeLookupResult> {
        return try {
            val response = mutualFundApi.lookupScheme(sessionToken, isin)
            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                    ?: return Result.failure(Exception("No data returned"))
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Scheme not found")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadCas(sessionToken: String, pdf: MultipartBody.Part): Result<CasPreviewData> {
        return try {
            val response = mutualFundApi.uploadCas(sessionToken, pdf)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No preview data returned"))
                Result.success(data)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to parse CAS PDF")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmCasImport(sessionToken: String, request: ConfirmImportRequest): Result<ImportResultDto> {
        return try {
            val response = mutualFundApi.confirmCasImport(sessionToken, request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No result returned"))
                Result.success(data)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Import failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHoldings(sessionToken: String): Result<Pair<List<MutualFundHolding>, MutualFundPortfolioSummary>> {
        return try {
            val response = mutualFundApi.getHoldings(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No holdings data returned"))
                val holdings = data.funds.map { it.toDomain() }
                val summary = data.summary.let {
                    MutualFundPortfolioSummary(
                        totalInvested     = it.totalInvested,
                        currentValue      = it.currentValue,
                        absoluteReturn    = it.absoluteReturn,
                        absoluteReturnPct = it.absoluteReturnPct
                    )
                }
                Result.success(Pair(holdings, summary))
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch holdings")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSummary(sessionToken: String): Result<MfCagrSummaryDto> {
        return try {
            val response = mutualFundApi.getSummary(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                    ?: return Result.failure(Exception("No summary data returned"))
                Result.success(data)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to fetch summary")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addLot(sessionToken: String, request: AddLotRequest): Result<Unit> {
        return try {
            val response = mutualFundApi.addLot(sessionToken, request)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to add holding")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLot(sessionToken: String, id: String): Result<Unit> {
        return try {
            val response = mutualFundApi.deleteLot(sessionToken, id)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to delete holding")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncNav(sessionToken: String): Result<Unit> {
        return try {
            val response = mutualFundApi.syncNav(sessionToken)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to sync NAV")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncCagr(sessionToken: String): Result<Unit> {
        return try {
            val response = mutualFundApi.syncCagr(sessionToken)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to sync CAGR")))
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

    private fun MutualFundLotDto.toDomain() = MutualFundLot(
        id             = id,
        purchaseDate   = purchaseDate,
        units          = units,
        purchaseNav    = purchaseNav,
        amountInvested = amountInvested
    )

    private fun MutualFundHoldingDto.toDomain() = MutualFundHolding(
        isin              = isin,
        schemeCode        = schemeCode,
        schemeName        = schemeName,
        amcName           = amcName,
        totalUnits        = totalUnits,
        avgNav            = avgNav,
        latestNav         = latestNav,
        latestNavDate     = latestNavDate,
        totalInvested     = totalInvested,
        currentValue      = currentValue,
        absoluteReturn    = absoluteReturn,
        absoluteReturnPct = absoluteReturnPct,
        xirr              = xirr,
        lots              = lots.map { it.toDomain() }
    )

    private fun com.example.personalwealthmanager.data.remote.dto.SchemeLookupDto.toDomain() = SchemeLookupResult(
        schemeCode = schemeCode,
        schemeName = schemeName,
        amcName    = amcName
    )
}
