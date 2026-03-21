package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.data.remote.dto.CreatePhysicalAssetRequest
import com.example.personalwealthmanager.data.remote.dto.UpdatePhysicalAssetRequest
import com.example.personalwealthmanager.domain.model.PhysicalAsset
import com.example.personalwealthmanager.domain.model.PhysicalAssetSummary

interface PhysicalAssetRepository {
    suspend fun getAssets(sessionToken: String): Result<List<PhysicalAsset>>
    suspend fun getSummary(sessionToken: String): Result<PhysicalAssetSummary>
    suspend fun createAsset(sessionToken: String, request: CreatePhysicalAssetRequest): Result<PhysicalAsset>
    suspend fun updateAsset(sessionToken: String, id: String, request: UpdatePhysicalAssetRequest): Result<PhysicalAsset>
    suspend fun deleteAsset(sessionToken: String, id: String): Result<Unit>
}
