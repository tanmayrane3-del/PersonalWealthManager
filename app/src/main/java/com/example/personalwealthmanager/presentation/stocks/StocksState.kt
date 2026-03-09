package com.example.personalwealthmanager.presentation.stocks

import com.example.personalwealthmanager.domain.model.StockHolding

sealed class StocksState {
    object Idle : StocksState()
    object Loading : StocksState()
    data class Success(val holdings: List<StockHolding>) : StocksState()
    data class Error(val message: String) : StocksState()
    object NotAuthenticated : StocksState()
    object CredentialsNotFound : StocksState()
}
