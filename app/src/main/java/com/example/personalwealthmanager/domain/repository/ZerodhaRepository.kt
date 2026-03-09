package com.example.personalwealthmanager.domain.repository

interface ZerodhaRepository {
    suspend fun saveCredentials(sessionToken: String, apiKey: String, apiSecret: String): Result<Unit>
    suspend fun getCredentials(sessionToken: String): Result<Pair<String, String>> // api_key to api_secret
    suspend fun getAuthUrl(sessionToken: String): Result<String>
    suspend fun exchangeToken(sessionToken: String, requestToken: String): Result<Unit>
}
