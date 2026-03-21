package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.CreateLiabilityRequest
import com.example.personalwealthmanager.data.remote.dto.LiabilitiesListDto
import com.example.personalwealthmanager.data.remote.dto.LiabilityDto
import com.example.personalwealthmanager.data.remote.dto.LiabilitySummaryDto
import com.example.personalwealthmanager.data.remote.dto.UpdateLiabilityRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface LiabilityApiService {

    @GET("api/liabilities")
    suspend fun getLiabilities(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<LiabilitiesListDto>>

    @GET("api/liabilities/summary")
    suspend fun getSummary(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<LiabilitySummaryDto>>

    @GET("api/liabilities/{id}")
    suspend fun getLiabilityById(
        @Header("x-session-token") sessionToken: String,
        @Path("id") id: String
    ): Response<ApiResponse<LiabilityDto>>

    @POST("api/liabilities")
    suspend fun createLiability(
        @Header("x-session-token") sessionToken: String,
        @Body request: CreateLiabilityRequest
    ): Response<ApiResponse<LiabilityDto>>

    @PUT("api/liabilities/{id}")
    suspend fun updateLiability(
        @Header("x-session-token") sessionToken: String,
        @Path("id") id: String,
        @Body request: UpdateLiabilityRequest
    ): Response<ApiResponse<LiabilityDto>>

    @DELETE("api/liabilities/{id}")
    suspend fun deleteLiability(
        @Header("x-session-token") sessionToken: String,
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}
