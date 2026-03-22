package com.example.personalwealthmanager.presentation.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.domain.repository.NetWorthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val repository: NetWorthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _currentState = MutableStateFlow<NetWorthCurrentState>(NetWorthCurrentState.Loading)
    val currentState: StateFlow<NetWorthCurrentState> = _currentState

    private val _snapshotsState = MutableStateFlow<NetWorthSnapshotsState>(NetWorthSnapshotsState.Loading)
    val snapshotsState: StateFlow<NetWorthSnapshotsState> = _snapshotsState

    var selectedPeriod: String = "1m"
        private set

    init {
        refresh()
    }

    fun fetchCurrent() {
        viewModelScope.launch {
            _currentState.value = NetWorthCurrentState.Loading
            val token = sessionManager.getSessionToken() ?: run {
                _currentState.value = NetWorthCurrentState.Error("Not logged in")
                return@launch
            }
            repository.getCurrent(token).fold(
                onSuccess = { dto -> _currentState.value = NetWorthCurrentState.Success(dto) },
                onFailure = { e  -> _currentState.value = NetWorthCurrentState.Error(e.message ?: "Failed to load net worth") }
            )
        }
    }

    fun fetchSnapshots(period: String) {
        selectedPeriod = period
        viewModelScope.launch {
            _snapshotsState.value = NetWorthSnapshotsState.Loading
            val token = sessionManager.getSessionToken() ?: run {
                _snapshotsState.value = NetWorthSnapshotsState.Error("Not logged in")
                return@launch
            }
            repository.getSnapshots(token, period).fold(
                onSuccess = { list -> _snapshotsState.value = NetWorthSnapshotsState.Success(list) },
                onFailure = { e   -> _snapshotsState.value = NetWorthSnapshotsState.Error(e.message ?: "Failed to load snapshots") }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val token = sessionManager.getSessionToken() ?: run {
                _currentState.value   = NetWorthCurrentState.Error("Not logged in")
                _snapshotsState.value = NetWorthSnapshotsState.Error("Not logged in")
                return@launch
            }
            _currentState.value   = NetWorthCurrentState.Loading
            _snapshotsState.value = NetWorthSnapshotsState.Loading

            val currentDeferred   = async { repository.getCurrent(token) }
            val snapshotsDeferred = async { repository.getSnapshots(token, selectedPeriod) }

            currentDeferred.await().fold(
                onSuccess = { dto -> _currentState.value = NetWorthCurrentState.Success(dto) },
                onFailure = { e  -> _currentState.value = NetWorthCurrentState.Error(e.message ?: "Failed to load net worth") }
            )
            snapshotsDeferred.await().fold(
                onSuccess = { list -> _snapshotsState.value = NetWorthSnapshotsState.Success(list) },
                onFailure = { e   -> _snapshotsState.value = NetWorthSnapshotsState.Error(e.message ?: "Failed to load snapshots") }
            )
        }
    }
}
