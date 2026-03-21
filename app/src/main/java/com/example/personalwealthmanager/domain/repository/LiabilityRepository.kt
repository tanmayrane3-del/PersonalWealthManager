package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.data.remote.dto.CreateLiabilityRequest
import com.example.personalwealthmanager.data.remote.dto.UpdateLiabilityRequest
import com.example.personalwealthmanager.domain.model.Liability
import com.example.personalwealthmanager.domain.model.LiabilitySummary

interface LiabilityRepository {
    suspend fun getLiabilities(sessionToken: String): Result<List<Liability>>
    suspend fun getSummary(sessionToken: String): Result<LiabilitySummary>
    suspend fun createLiability(sessionToken: String, request: CreateLiabilityRequest): Result<Liability>
    suspend fun updateLiability(sessionToken: String, id: String, request: UpdateLiabilityRequest): Result<Liability>
    suspend fun deleteLiability(sessionToken: String, id: String): Result<Unit>
}
