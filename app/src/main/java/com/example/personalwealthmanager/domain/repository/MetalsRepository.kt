package com.pwm.personalwealthmanager.domain.repository

import com.pwm.personalwealthmanager.data.remote.dto.MetalHoldingRequest
import com.pwm.personalwealthmanager.data.remote.dto.MetalsSummaryDto
import com.pwm.personalwealthmanager.domain.model.MetalHolding
import com.pwm.personalwealthmanager.domain.model.MetalRates

interface MetalsRepository {
    suspend fun getRates(sessionToken: String): Result<MetalRates>
    suspend fun getHoldings(sessionToken: String): Result<Triple<List<MetalHolding>, MetalRates, Double>>
    suspend fun getSummary(sessionToken: String): Result<MetalsSummaryDto>
    suspend fun syncCagr(sessionToken: String): Result<Unit>
    suspend fun addHolding(sessionToken: String, request: MetalHoldingRequest): Result<MetalHolding>
    suspend fun updateHolding(sessionToken: String, id: String, request: MetalHoldingRequest): Result<MetalHolding>
    suspend fun deleteHolding(sessionToken: String, id: String): Result<Unit>
}
