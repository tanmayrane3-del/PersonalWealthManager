package com.example.personalwealthmanager.presentation.zerodha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.domain.repository.ZerodhaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupZerodhaViewModel @Inject constructor(
    private val zerodhaRepository: ZerodhaRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<SetupZerodhaState>(SetupZerodhaState.Idle)
    val state: StateFlow<SetupZerodhaState> = _state

    fun loadCredentials() {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            val result = zerodhaRepository.getCredentials(token)
            result.fold(
                onSuccess = { (key, secret) ->
                    _state.value = SetupZerodhaState.CredentialsLoaded(key, secret)
                },
                onFailure = { /* No credentials yet — stay Idle, don't show error */ }
            )
        }
    }

    fun saveCredentials(apiKey: String, apiSecret: String) {
        val token = sessionManager.getSessionToken() ?: return
        viewModelScope.launch {
            _state.value = SetupZerodhaState.Loading
            val result = zerodhaRepository.saveCredentials(token, apiKey, apiSecret)
            result.fold(
                onSuccess = { _state.value = SetupZerodhaState.Success },
                onFailure = { e -> _state.value = SetupZerodhaState.Error(e.message ?: "Failed to save credentials") }
            )
        }
    }

    fun resetState() {
        _state.value = SetupZerodhaState.Idle
    }
}
