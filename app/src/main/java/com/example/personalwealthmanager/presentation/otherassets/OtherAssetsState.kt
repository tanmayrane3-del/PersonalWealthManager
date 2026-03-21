package com.example.personalwealthmanager.presentation.otherassets

import com.example.personalwealthmanager.domain.model.PhysicalAssetSummary

sealed class OtherAssetsUiState {
    object Idle : OtherAssetsUiState()
    object Loading : OtherAssetsUiState()
    data class Success(val summary: PhysicalAssetSummary) : OtherAssetsUiState()
    data class Error(val message: String) : OtherAssetsUiState()
}

sealed class OtherAssetsActionState {
    object Idle : OtherAssetsActionState()
    object Saving : OtherAssetsActionState()
    object Saved : OtherAssetsActionState()
    object Deleting : OtherAssetsActionState()
    data class Error(val message: String) : OtherAssetsActionState()
}
