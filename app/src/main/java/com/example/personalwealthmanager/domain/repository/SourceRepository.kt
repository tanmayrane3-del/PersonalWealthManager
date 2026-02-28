package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.domain.model.Source

interface SourceRepository {
    suspend fun getSources(): Result<List<Source>>

    suspend fun createSource(
        name: String,
        description: String?,
        type: String?,
        contactInfo: String?,
        sourceIdentifier: String?,
        defaultCategoryId: String? = null
    ): Result<Source>

    suspend fun updateSource(
        sourceId: String,
        name: String?,
        description: String?,
        type: String?,
        contactInfo: String?,
        sourceIdentifier: String?,
        defaultCategoryId: String? = null
    ): Result<Source>

    suspend fun deleteSource(sourceId: String): Result<Unit>
}
