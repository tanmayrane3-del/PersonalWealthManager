package com.example.personalwealthmanager.presentation.networth

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
