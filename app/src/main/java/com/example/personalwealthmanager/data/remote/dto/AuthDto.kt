package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

// Generic API Response wrapper
data class ApiResponse<T>(
    val status: String,
    val timestamp: String,
    val data: T?,
    val reason: String?,
    @SerializedName("request_id") val requestId: String?,
    @SerializedName("api_version") val apiVersion: String?
)

// Auth DTOs
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    @SerializedName("user_id") val userId: String,
    val email: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("is_email_verified") val isEmailVerified: Boolean
)

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name") val fullName: String? = null,
    val phone: String? = null
)

data class RegisterResponse(
    @SerializedName("user_id") val userId: String,
    val email: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("is_email_verified") val isEmailVerified: Boolean,
    val phone: String?,
    @SerializedName("is_active") val isActive: Boolean
)

// Session DTOs
data class CreateSessionRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("ip_address") val ipAddress: String,
    @SerializedName("user_agent") val userAgent: String
)

data class CreateSessionResponse(
    @SerializedName("session_token") val sessionToken: String
)