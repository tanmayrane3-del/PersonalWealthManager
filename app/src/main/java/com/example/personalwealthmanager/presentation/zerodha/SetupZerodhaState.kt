package com.example.personalwealthmanager.presentation.zerodha

sealed class SetupZerodhaState {
    object Idle : SetupZerodhaState()
    object Loading : SetupZerodhaState()
    object Success : SetupZerodhaState()
    data class CredentialsLoaded(val apiKey: String, val apiSecret: String) : SetupZerodhaState()
    data class Error(val message: String) : SetupZerodhaState()
}
