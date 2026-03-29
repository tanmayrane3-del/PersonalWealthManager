package com.example.personalwealthmanager.presentation.networth

import com.example.personalwealthmanager.data.remote.dto.MetalsSummaryDto
import com.example.personalwealthmanager.data.remote.dto.MfCagrSummaryDto
import com.example.personalwealthmanager.data.remote.dto.NetWorthCurrentDto
import com.example.personalwealthmanager.data.remote.dto.NetWorthSnapshotDto

sealed class NetWorthCurrentState {
    object Idle    : NetWorthCurrentState()
    object Loading : NetWorthCurrentState()
    data class Success(val data: NetWorthCurrentDto)   : NetWorthCurrentState()
    data class Error(val message: String)              : NetWorthCurrentState()
}

sealed class NetWorthSnapshotsState {
    object Idle    : NetWorthSnapshotsState()
    object Loading : NetWorthSnapshotsState()
    data class Success(val snapshots: List<NetWorthSnapshotDto>) : NetWorthSnapshotsState()
    data class Error(val message: String)                       : NetWorthSnapshotsState()
}

sealed class StocksWidgetState {
    object Idle    : StocksWidgetState()
    object Loading : StocksWidgetState()
    data class Success(val totalValue: Double, val todayPnl: Double) : StocksWidgetState()
    data class Error(val message: String) : StocksWidgetState()
}

sealed class MetalsWidgetState {
    object Idle    : MetalsWidgetState()
    object Loading : MetalsWidgetState()
    data class Success(val data: MetalsSummaryDto) : MetalsWidgetState()
    data class Error(val message: String) : MetalsWidgetState()
}

sealed class MfWidgetState {
    object Idle    : MfWidgetState()
    object Loading : MfWidgetState()
    data class Success(val data: MfCagrSummaryDto) : MfWidgetState()
    data class Error(val message: String) : MfWidgetState()
}

sealed class OtherAssetsWidgetState {
    object Idle    : OtherAssetsWidgetState()
    object Loading : OtherAssetsWidgetState()
    data class Success(
        val totalValue: Double,
        val proj1y: Double = 0.0,
        val proj3y: Double = 0.0,
        val proj5y: Double = 0.0,
    ) : OtherAssetsWidgetState()
    data class Error(val message: String) : OtherAssetsWidgetState()
}
