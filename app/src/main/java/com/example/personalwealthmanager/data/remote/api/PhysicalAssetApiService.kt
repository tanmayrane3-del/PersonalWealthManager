package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.CreatePhysicalAssetRequest
import com.example.personalwealthmanager.data.remote.dto.PhysicalAssetDto
import com.example.personalwealthmanager.data.remote.dto.PhysicalAssetsListDto
import com.example.personalwealthmanager.data.remote.dto.PhysicalAssetSummaryDto
import com.example.personalwealthmanager.data.remote.dto.UpdatePhysicalAssetRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PhysicalAssetApiService {

    @GET("api/physical-assets")
    suspend fun getAssets(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<PhysicalAssetsListDto>>

    @GET("api/physical-assets/summary")
    suspend fun getSummary(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<PhysicalAssetSummaryDto>>

    @POST("api/physical-assets")
    suspend fun createAsset(
        @Header("x-session-token") sessionToken: String,
        @Body request: CreatePhysicalAssetRequest
    ): Response<ApiResponse<PhysicalAssetDto>>

    @PUT("api/physical-assets/{id}")
    suspend fun updateAsset(
        @Header("x-session-token") sessionToken: String,
        @Path("id") id: String,
        @Body request: UpdatePhysicalAssetRequest
    ): Response<ApiResponse<PhysicalAssetDto>>

    @DELETE("api/physical-assets/{id}")
    suspend fun deleteAsset(
        @Header("x-session-token") sessionToken: String,
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>
}
