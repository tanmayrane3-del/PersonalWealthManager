package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.data.remote.dto.AddLotRequest
import com.example.personalwealthmanager.data.remote.dto.CasPreviewData
import com.example.personalwealthmanager.data.remote.dto.ConfirmImportRequest
import com.example.personalwealthmanager.data.remote.dto.ImportResultDto
import com.example.personalwealthmanager.data.remote.dto.MfCagrSummaryDto
import com.example.personalwealthmanager.domain.model.MutualFundHolding
import com.example.personalwealthmanager.domain.model.MutualFundPortfolioSummary
import com.example.personalwealthmanager.domain.model.SchemeLookupResult
import okhttp3.MultipartBody

interface MutualFundRepository {
    suspend fun lookupScheme(sessionToken: String, isin: String): Result<SchemeLookupResult>
    suspend fun uploadCas(sessionToken: String, pdf: MultipartBody.Part): Result<CasPreviewData>
    suspend fun confirmCasImport(sessionToken: String, request: ConfirmImportRequest): Result<ImportResultDto>
    suspend fun getHoldings(sessionToken: String): Result<Pair<List<MutualFundHolding>, MutualFundPortfolioSummary>>
    suspend fun getSummary(sessionToken: String): Result<MfCagrSummaryDto>
    suspend fun addLot(sessionToken: String, request: AddLotRequest): Result<Unit>
    suspend fun deleteLot(sessionToken: String, id: String): Result<Unit>
    suspend fun syncNav(sessionToken: String): Result<Unit>
    suspend fun syncCagr(sessionToken: String): Result<Unit>
}
