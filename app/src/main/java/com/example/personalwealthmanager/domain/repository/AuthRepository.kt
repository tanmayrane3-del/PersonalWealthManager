package com.pwm.personalwealthmanager.domain.repository

import com.pwm.personalwealthmanager.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, fullName: String, phone: String): Result<User>
    suspend fun logout(): Result<Unit>
}
