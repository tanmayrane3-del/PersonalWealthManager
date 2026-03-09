package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.data.remote.api.ZerodhaApi
import com.example.personalwealthmanager.data.remote.dto.CredentialsRequest
import com.example.personalwealthmanager.data.remote.dto.ExchangeTokenRequest
import com.example.personalwealthmanager.domain.repository.ZerodhaRepository
import org.json.JSONObject
import javax.inject.Inject

class ZerodhaRepositoryImpl @Inject constructor(
    private val zerodhaApi: ZerodhaApi
) : ZerodhaRepository {

    override suspend fun saveCredentials(sessionToken: String, apiKey: String, apiSecret: String): Result<Unit> {
        return try {
            val response = zerodhaApi.saveCredentials(sessionToken, CredentialsRequest(apiKey, apiSecret))
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to save credentials")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCredentials(sessionToken: String): Result<Pair<String, String>> {
        return try {
            val response = zerodhaApi.getCredentials(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val data = response.body()?.data
                if (data != null) Result.success(Pair(data.apiKey, data.apiSecret))
                else Result.failure(Exception("Credentials not found"))
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Credentials not found")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAuthUrl(sessionToken: String): Result<String> {
        return try {
            val response = zerodhaApi.getAuthUrl(sessionToken)
            if (response.isSuccessful && response.body()?.status == "success") {
                val url = response.body()?.data?.authUrl
                if (url != null) Result.success(url)
                else Result.failure(Exception("Auth URL not found in response"))
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Failed to get auth URL")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exchangeToken(sessionToken: String, requestToken: String): Result<Unit> {
        return try {
            val response = zerodhaApi.exchangeToken(sessionToken, ExchangeTokenRequest(requestToken))
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseReason(response.errorBody()?.string(), "Token exchange failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Retrofit puts error bodies in errorBody(), not body(), for non-2xx responses.
    private fun parseReason(errorJson: String?, fallback: String): String {
        if (errorJson.isNullOrBlank()) return fallback
        return try {
            JSONObject(errorJson).optString("reason", fallback).ifBlank { fallback }
        } catch (e: Exception) {
            fallback
        }
    }
}
