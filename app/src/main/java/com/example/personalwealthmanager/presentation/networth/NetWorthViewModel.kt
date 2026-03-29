package com.example.personalwealthmanager.presentation.networth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.domain.repository.HoldingsRepository
import com.example.personalwealthmanager.domain.repository.MetalsRepository
import com.example.personalwealthmanager.domain.repository.MutualFundRepository
import com.example.personalwealthmanager.domain.repository.NetWorthRepository
import com.example.personalwealthmanager.domain.repository.PhysicalAssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetWorthViewModel @Inject constructor(
    private val repository: NetWorthRepository,
    private val sessionManager: SessionManager,
    private val holdingsRepository: HoldingsRepository,
    private val metalsRepository: MetalsRepository,
    private val mfRepository: MutualFundRepository,
    private val otherAssetsRepository: PhysicalAssetRepository
) : ViewModel() {

    private val _currentState = MutableStateFlow<NetWorthCurrentState>(NetWorthCurrentState.Loading)
    val currentState: StateFlow<NetWorthCurrentState> = _currentState

    private val _snapshotsState = MutableStateFlow<NetWorthSnapshotsState>(NetWorthSnapshotsState.Loading)
    val snapshotsState: StateFlow<NetWorthSnapshotsState> = _snapshotsState

    private val _stocksState = MutableStateFlow<StocksWidgetState>(StocksWidgetState.Idle)
    val stocksState: StateFlow<StocksWidgetState> = _stocksState

    private val _metalsState = MutableStateFlow<MetalsWidgetState>(MetalsWidgetState.Idle)
    val metalsState: StateFlow<MetalsWidgetState> = _metalsState

    private val _mfState = MutableStateFlow<MfWidgetState>(MfWidgetState.Idle)
    val mfState: StateFlow<MfWidgetState> = _mfState

    private val _otherAssetsState = MutableStateFlow<OtherAssetsWidgetState>(OtherAssetsWidgetState.Idle)
    val otherAssetsState: StateFlow<OtherAssetsWidgetState> = _otherAssetsState

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
                _currentState.value      = NetWorthCurrentState.Error("Not logged in")
                _snapshotsState.value    = NetWorthSnapshotsState.Error("Not logged in")
                _stocksState.value       = StocksWidgetState.Error("Not logged in")
                _metalsState.value       = MetalsWidgetState.Error("Not logged in")
                _mfState.value           = MfWidgetState.Error("Not logged in")
                _otherAssetsState.value  = OtherAssetsWidgetState.Error("Not logged in")
                return@launch
            }

            // Set all to loading immediately
            _currentState.value     = NetWorthCurrentState.Loading
            _snapshotsState.value   = NetWorthSnapshotsState.Loading
            _stocksState.value      = StocksWidgetState.Loading
            _metalsState.value      = MetalsWidgetState.Loading
            _mfState.value          = MfWidgetState.Loading
            _otherAssetsState.value = OtherAssetsWidgetState.Loading

            // Each asset updates independently as its response arrives
            launch {
                repository.getCurrent(token).fold(
                    onSuccess = { dto -> _currentState.value = NetWorthCurrentState.Success(dto) },
                    onFailure = { e  -> _currentState.value = NetWorthCurrentState.Error(e.message ?: "Failed to load net worth") }
                )
            }
            launch {
                repository.getSnapshots(token, selectedPeriod).fold(
                    onSuccess = { list -> _snapshotsState.value = NetWorthSnapshotsState.Success(list) },
                    onFailure = { e   -> _snapshotsState.value = NetWorthSnapshotsState.Error(e.message ?: "Failed to load snapshots") }
                )
            }
            launch {
                holdingsRepository.getHoldings(token).fold(
                    onSuccess = { holdings ->
                        val total = holdings.sumOf { it.currentValue }
                        val pnl   = holdings.sumOf { it.dayChange }
                        _stocksState.value = StocksWidgetState.Success(total, pnl)
                    },
                    onFailure = { e -> _stocksState.value = StocksWidgetState.Error(e.message ?: "Failed to load stocks") }
                )
            }
            launch {
                metalsRepository.getSummary(token).fold(
                    onSuccess = { dto -> _metalsState.value = MetalsWidgetState.Success(dto) },
                    onFailure = { e  -> _metalsState.value = MetalsWidgetState.Error(e.message ?: "Failed to load metals") }
                )
            }
            launch {
                mfRepository.getSummary(token).fold(
                    onSuccess = { dto -> _mfState.value = MfWidgetState.Success(dto) },
                    onFailure = { e  -> _mfState.value = MfWidgetState.Error(e.message ?: "Failed to load mutual funds") }
                )
            }
            launch {
                otherAssetsRepository.getSummary(token).fold(
                    onSuccess = { summary ->
                        _otherAssetsState.value = OtherAssetsWidgetState.Success(
                            totalValue = summary.totalCurrentValue,
                            proj1y     = summary.proj1y,
                            proj3y     = summary.proj3y,
                            proj5y     = summary.proj5y,
                        )
                    },
                    onFailure = { e -> _otherAssetsState.value = OtherAssetsWidgetState.Error(e.message ?: "Failed to load other assets") }
                )
            }
        }
    }
}
