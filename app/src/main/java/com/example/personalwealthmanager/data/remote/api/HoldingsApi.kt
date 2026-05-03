package com.pwm.personalwealthmanager.data.remote.api

import com.pwm.personalwealthmanager.data.remote.dto.ApiResponse
import com.pwm.personalwealthmanager.data.remote.dto.HoldingsListResponse
import com.pwm.personalwealthmanager.data.remote.dto.HoldingsSyncResponse
import com.pwm.personalwealthmanager.data.remote.dto.StocksSummaryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface HoldingsApi {

    @GET("api/holdings/sync")
    suspend fun syncHoldings(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<HoldingsSyncResponse>>

    @GET("api/holdings")
    suspend fun getHoldings(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<HoldingsListResponse>>

    @GET("api/holdings/summary")
    suspend fun getSummary(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<StocksSummaryDto>>
}
