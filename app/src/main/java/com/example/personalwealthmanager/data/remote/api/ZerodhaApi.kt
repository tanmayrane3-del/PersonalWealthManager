package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.AuthUrlResponse
import com.example.personalwealthmanager.data.remote.dto.CredentialsRequest
import com.example.personalwealthmanager.data.remote.dto.CredentialsResponse
import com.example.personalwealthmanager.data.remote.dto.ExchangeTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ZerodhaApi {

    @POST("api/zerodha/credentials")
    suspend fun saveCredentials(
        @Header("x-session-token") sessionToken: String,
        @Body request: CredentialsRequest
    ): Response<ApiResponse<Unit>>

    @GET("api/zerodha/credentials")
    suspend fun getCredentials(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<CredentialsResponse>>

    @GET("api/zerodha/auth-url")
    suspend fun getAuthUrl(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<AuthUrlResponse>>

    @POST("api/zerodha/callback")
    suspend fun exchangeToken(
        @Header("x-session-token") sessionToken: String,
        @Body request: ExchangeTokenRequest
    ): Response<ApiResponse<Unit>>
}
