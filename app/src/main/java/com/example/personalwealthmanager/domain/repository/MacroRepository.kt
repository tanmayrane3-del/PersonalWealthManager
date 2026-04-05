package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.data.remote.dto.MacroAccuracyDto
import com.example.personalwealthmanager.data.remote.dto.MacroHistoryItemDto
import com.example.personalwealthmanager.data.remote.dto.MacroSignalDto

interface MacroRepository {
    suspend fun getSignal(sessionToken: String): Result<MacroSignalDto?>
    suspend fun getHistory(sessionToken: String): Result<List<MacroHistoryItemDto>>
    suspend fun getAccuracy(sessionToken: String): Result<List<MacroAccuracyDto>>
}
