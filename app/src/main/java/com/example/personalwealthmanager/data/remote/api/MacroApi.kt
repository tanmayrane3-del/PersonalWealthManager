package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.MacroAccuracyDto
import com.example.personalwealthmanager.data.remote.dto.MacroHistoryItemDto
import com.example.personalwealthmanager.data.remote.dto.MacroSignalDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MacroApi {

    @GET("api/macro/signal")
    suspend fun getSignal(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<MacroSignalDto>>

    @GET("api/macro/history")
    suspend fun getHistory(
        @Header("x-session-token") sessionToken: String,
        @Query("months") months: Int = 12
    ): Response<ApiResponse<List<MacroHistoryItemDto>>>

    @GET("api/macro/accuracy")
    suspend fun getAccuracy(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<List<MacroAccuracyDto>>>
}
