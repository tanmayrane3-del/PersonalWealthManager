package com.pwm.personalwealthmanager.domain.repository

import com.pwm.personalwealthmanager.domain.model.StockHolding
import com.pwm.personalwealthmanager.domain.model.StocksPortfolioSummary

interface HoldingsRepository {
    suspend fun getHoldings(sessionToken: String): Result<List<StockHolding>>
    suspend fun syncHoldings(sessionToken: String): Result<List<StockHolding>>
    suspend fun getSummary(sessionToken: String): Result<StocksPortfolioSummary>
}
