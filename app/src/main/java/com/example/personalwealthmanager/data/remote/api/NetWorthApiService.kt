package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.NetWorthCurrentDto
import com.example.personalwealthmanager.data.remote.dto.NetWorthSnapshotsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NetWorthApiService {

    @GET("api/net-worth/current")
    suspend fun getCurrent(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<NetWorthCurrentDto>>

    @GET("api/net-worth/snapshots")
    suspend fun getSnapshots(
        @Header("x-session-token") sessionToken: String,
        @Query("period") period: String
    ): Response<ApiResponse<NetWorthSnapshotsDto>>
}
