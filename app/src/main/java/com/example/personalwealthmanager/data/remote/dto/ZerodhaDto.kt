package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CredentialsRequest(
    @SerializedName("api_key") val apiKey: String,
    @SerializedName("api_secret") val apiSecret: String
)

data class AuthUrlResponse(
    @SerializedName("auth_url") val authUrl: String
)

data class CredentialsResponse(
    @SerializedName("api_key") val apiKey: String,
    @SerializedName("api_secret") val apiSecret: String
)

data class ExchangeTokenRequest(
    @SerializedName("request_token") val requestToken: String
)
