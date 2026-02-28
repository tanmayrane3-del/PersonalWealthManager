package com.example.personalwealthmanager.data.remote.api

import com.example.personalwealthmanager.data.remote.dto.ApiResponse
import com.example.personalwealthmanager.data.remote.dto.CreateSessionRequest
import com.example.personalwealthmanager.data.remote.dto.CreateSessionResponse
import com.example.personalwealthmanager.data.remote.dto.LoginRequest
import com.example.personalwealthmanager.data.remote.dto.LoginResponse
import com.example.personalwealthmanager.data.remote.dto.RegisterRequest
import com.example.personalwealthmanager.data.remote.dto.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    // Step 1: Validate login credentials
    @POST("api/users/validate-login")
    suspend fun validateLogin(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    // Step 2: Create session (after validation)
    @POST("api/sessions/login")
    suspend fun createSession(
        @Body request: CreateSessionRequest
    ): Response<ApiResponse<CreateSessionResponse>>

    // Register new user
    @POST("api/users")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<RegisterResponse>>

    // Logout - end session
    @POST("api/sessions/logout")
    suspend fun logout(
        @Header("x-session-token") sessionToken: String
    ): Response<ApiResponse<Unit>>
}