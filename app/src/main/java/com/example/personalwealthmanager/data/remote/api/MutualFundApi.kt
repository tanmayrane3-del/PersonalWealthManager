package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.AddLotRequest
import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.CasPreviewData
import com.example.personalwealthmanager.data.remote.dto.ConfirmImportRequest
import com.example.personalwealthmanager.data.remote.dto.ImportResultDto
import com.example.personalwealthmanager.data.remote.dto.MfCagrSummaryDto
import com.example.personalwealthmanager.data.remote.dto.MfHoldingsData
import com.example.personalwealthmanager.data.remote.dto.SchemeLookupDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MutualFundApi {

    @GET("api/mutual-funds/lookup")
    suspend fun lookupScheme(
        @Header("x-session-token") sessionToken: String,
        @Query("isin") isin: String
    ): Response<ApiResponse<SchemeLookupDto>>

    @Multipart
    @POST("api/mutual-funds/cas/upload")
    suspend fun uploadCas(
        @Header("x-session-token") sessionToken: String,
        @Part pdf: MultipartBody.Part
    ): Response<ApiResponse<CasPreviewData>>

    @POST("api/mutual-funds/cas/confirm")
    suspend fun confirmCasImport(
        @Header("x-session-token") sessionToken: String,
        @Body request: ConfirmImportRequest
    ): Response<ApiResponse<ImportResultDto>>

    @GET("api/mutual-funds/holdings")
    suspend fun getHoldings(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<MfHoldingsData>>

    @GET("api/mutual-funds/summary")
    suspend fun getSummary(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<MfCagrSummaryDto>>

    @POST("api/mutual-funds/holdings")
    suspend fun addLot(
        @Header("x-session-token") sessionToken: String,
        @Body request: AddLotRequest
    ): Response<ApiResponse<Any>>

    @DELETE("api/mutual-funds/holdings/{id}")
    suspend fun deleteLot(
        @Header("x-session-token") sessionToken: String,
        @Path("id") id: String
    ): Response<ApiResponse<Any>>

    @POST("api/mutual-funds/sync-cagr")
    suspend fun syncCagr(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<Any>>
}
