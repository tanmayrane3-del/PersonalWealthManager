package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.MetalHoldingDto
import com.example.personalwealthmanager.data.remote.dto.MetalHoldingRequest
import com.example.personalwealthmanager.data.remote.dto.MetalHoldingsData
import com.example.personalwealthmanager.data.remote.dto.MetalRatesDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface MetalsApi {

    @GET("api/metals/rates")
    suspend fun getRates(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<MetalRatesDto>>

    @GET("api/metals/holdings")
    suspend fun getHoldings(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<MetalHoldingsData>>

    @POST("api/metals/holdings")
    suspend fun addHolding(
        @Header("x-session-token") sessionToken: String,
        @Body request: MetalHoldingRequest
    ): Response<ApiResponse<MetalHoldingDto>>

    @PUT("api/metals/holdings/{id}")
    suspend fun updateHolding(
        @Header("x-session-token") sessionToken: String,
        @Path("id") id: String,
        @Body request: MetalHoldingRequest
    ): Response<ApiResponse<MetalHoldingDto>>

    @DELETE("api/metals/holdings/{id}")
    suspend fun deleteHolding(
        @Header("x-session-token") sessionToken: String,
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}
