package com.example.personalwealthmanager.presentation.liabilities

import com.example.personalwealthmanager.domain.model.LiabilitySummary

sealed class LiabilitiesUiState {
    object Idle : LiabilitiesUiState()
    object Loading : LiabilitiesUiState()
    data class Success(val summary: LiabilitySummary) : LiabilitiesUiState()
    data class Error(val message: String) : LiabilitiesUiState()
}

sealed class LiabilitiesActionState {
    object Idle : LiabilitiesActionState()
    object Saving : LiabilitiesActionState()
    object Saved : LiabilitiesActionState()
    object Deleting : LiabilitiesActionState()
    data class Error(val message: String) : LiabilitiesActionState()
}
