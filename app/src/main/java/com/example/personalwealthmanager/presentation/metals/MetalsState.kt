package com.example.personalwealthmanager.presentation.metals

import com.example.personalwealthmanager.domain.model.MetalHolding
import com.example.personalwealthmanager.domain.model.MetalRates

sealed class MetalsUiState {
    object Idle : MetalsUiState()
    object Loading : MetalsUiState()
    data class Success(
        val holdings: List<MetalHolding>,
        val rates: MetalRates,
        val totalValue: Double
    ) : MetalsUiState()
    data class Error(val message: String) : MetalsUiState()
}
