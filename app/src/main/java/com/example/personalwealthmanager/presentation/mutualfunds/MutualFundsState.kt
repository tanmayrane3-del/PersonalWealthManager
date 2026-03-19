package com.example.personalwealthmanager.presentation.mutualfunds

import com.example.personalwealthmanager.domain.model.MutualFundHolding
import com.example.personalwealthmanager.domain.model.MutualFundPortfolioSummary

sealed class MutualFundsUiState {
    object Idle    : MutualFundsUiState()
    object Loading : MutualFundsUiState()
    data class Success(
        val funds: List<MutualFundHolding>,
        val summary: MutualFundPortfolioSummary
    ) : MutualFundsUiState()
    data class Error(val message: String) : MutualFundsUiState()
}

sealed class MutualFundsCagrState {
    object Idle    : MutualFundsCagrState()
    object Syncing : MutualFundsCagrState()
    data class Available(
        val totalValue:  Double,
        val projected1y: Double,
        val projected3y: Double,
        val projected5y: Double
    ) : MutualFundsCagrState()
}
