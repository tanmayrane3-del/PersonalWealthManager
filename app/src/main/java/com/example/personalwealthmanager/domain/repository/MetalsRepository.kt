package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.data.remote.dto.MetalHoldingRequest
import com.example.personalwealthmanager.domain.model.MetalHolding
import com.example.personalwealthmanager.domain.model.MetalRates

interface MetalsRepository {
    suspend fun getRates(sessionToken: String): Result<MetalRates>
    suspend fun getHoldings(sessionToken: String): Result<Triple<List<MetalHolding>, MetalRates, Double>>
    suspend fun addHolding(sessionToken: String, request: MetalHoldingRequest): Result<MetalHolding>
    suspend fun updateHolding(sessionToken: String, id: String, request: MetalHoldingRequest): Result<MetalHolding>
    suspend fun deleteHolding(sessionToken: String, id: String): Result<Unit>
}
