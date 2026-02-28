package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.api.AuthApi
import com.example.personalwealthmanager.data.remote.dto.CreateSessionRequest
import com.example.personalwealthmanager.data.remote.dto.LoginRequest
import com.example.personalwealthmanager.data.remote.dto.RegisterRequest
import com.example.personalwealthmanager.domain.model.User
import com.example.personalwealthmanager.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            // Step 1: Validate credentials
            val validateResponse = authApi.validateLogin(LoginRequest(email, password))

            if (!validateResponse.isSuccessful || validateResponse.body()?.status != "success") {
                val errorMessage = validateResponse.body()?.reason ?: "Login failed"
                return Result.failure(Exception(errorMessage))
            }

            val userData = validateResponse.body()?.data
                ?: return Result.failure(Exception("Invalid response data"))

            // Step 2: Create session
            val sessionRequest = CreateSessionRequest(
                userId = userData.userId,
                ipAddress = "0.0.0.0", // You can get actual IP if needed
                userAgent = "Android App"
            )

            val sessionResponse = authApi.createSession(sessionRequest)

            if (!sessionResponse.isSuccessful || sessionResponse.body()?.status != "success") {
                val errorMessage = sessionResponse.body()?.reason ?: "Session creation failed"
                return Result.failure(Exception(errorMessage))
            }

            val sessionData = sessionResponse.body()?.data
                ?: return Result.failure(Exception("Invalid session response"))

            // Save session
            sessionManager.saveSession(sessionData.sessionToken, userData.email)

            Result.success(
                User(
                    email = userData.email,
                    sessionToken = sessionData.sessionToken
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String): Result<User> {
        return try {
            // Create user
            val registerResponse = authApi.register(RegisterRequest(email, password))

            if (!registerResponse.isSuccessful || registerResponse.body()?.status != "success") {
                val errorMessage = registerResponse.body()?.reason ?: "Registration failed"
                return Result.failure(Exception(errorMessage))
            }

            val userData = registerResponse.body()?.data
                ?: return Result.failure(Exception("Invalid response data"))

            // Create session
            val sessionRequest = CreateSessionRequest(
                userId = userData.userId,
                ipAddress = "0.0.0.0",
                userAgent = "Android App"
            )

            val sessionResponse = authApi.createSession(sessionRequest)

            if (!sessionResponse.isSuccessful || sessionResponse.body()?.status != "success") {
                val errorMessage = sessionResponse.body()?.reason ?: "Session creation failed"
                return Result.failure(Exception(errorMessage))
            }

            val sessionData = sessionResponse.body()?.data
                ?: return Result.failure(Exception("Invalid session response"))

            // Save session
            sessionManager.saveSession(sessionData.sessionToken, userData.email)

            Result.success(
                User(
                    email = userData.email,
                    sessionToken = sessionData.sessionToken
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val sessionToken = sessionManager.getSessionToken()

            if (sessionToken != null) {
                val response = authApi.logout(sessionToken)

                // Clear session regardless of response
                sessionManager.clearSession()

                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Logout failed"))
                }
            } else {
                // No session to logout
                sessionManager.clearSession()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Clear session even on error
            sessionManager.clearSession()
            Result.failure(e)
        }
    }
}