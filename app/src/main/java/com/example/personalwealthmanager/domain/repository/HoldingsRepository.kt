package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.domain.model.StockHolding
import com.example.personalwealthmanager.domain.model.StocksPortfolioSummary

interface HoldingsRepository {
    suspend fun getHoldings(sessionToken: String): Result<List<StockHolding>>
    suspend fun syncHoldings(sessionToken: String): Result<List<StockHolding>>
    suspend fun getSummary(sessionToken: String): Result<StocksPortfolioSummary>
}
