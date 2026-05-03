package com.pwm.personalwealthmanager.domain.repository

import com.pwm.personalwealthmanager.data.remote.dto.NetWorthCurrentDto
import com.pwm.personalwealthmanager.data.remote.dto.NetWorthSnapshotDto

interface NetWorthRepository {
    suspend fun getCurrent(sessionToken: String): Result<NetWorthCurrentDto>
    suspend fun getSnapshots(sessionToken: String, period: String): Result<List<NetWorthSnapshotDto>>
}
