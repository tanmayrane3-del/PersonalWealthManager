package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.api.MetadataApi
import com.example.personalwealthmanager.data.remote.dto.CreateSourceRequest
import com.example.personalwealthmanager.data.remote.dto.SourceDto
import com.example.personalwealthmanager.data.remote.dto.UpdateSourceRequest
import com.example.personalwealthmanager.domain.model.Source
import com.example.personalwealthmanager.domain.repository.SourceRepository
import javax.inject.Inject

class SourceRepositoryImpl @Inject constructor(
    private val metadataApi: MetadataApi,
    private val sessionManager: SessionManager
) : SourceRepository {

    override suspend fun getSources(): Result<List<Source>> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = metadataApi.getSources(sessionToken)

            if (response.isSuccessful && response.body()?.status == "success") {
                val sources = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                Result.success(sources)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to fetch income sources"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createSource(
        name: String,
        description: String?,
        type: String?,
        contactInfo: String?,
        sourceIdentifier: String?,
        defaultCategoryId: String?
    ): Result<Source> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val request = CreateSourceRequest(
                name = name,
                description = description,
                type = type,
                contactInfo = contactInfo,
                sourceIdentifier = sourceIdentifier,
                defaultCategoryId = defaultCategoryId
            )

            val response = metadataApi.createSource(sessionToken, request)

            if (response.isSuccessful && response.body()?.status == "success") {
                val sourceDto = response.body()?.data
                if (sourceDto != null) {
                    Result.success(sourceDto.toDomain())
                } else {
                    Result.failure(Exception("Failed to create income source"))
                }
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to create income source"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSource(
        sourceId: String,
        name: String?,
        description: String?,
        type: String?,
        contactInfo: String?,
        sourceIdentifier: String?,
        defaultCategoryId: String?
    ): Result<Source> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val request = UpdateSourceRequest(
                name = name,
                description = description,
                type = type,
                contactInfo = contactInfo,
                sourceIdentifier = sourceIdentifier,
                defaultCategoryId = defaultCategoryId
            )

            val response = metadataApi.updateSource(sessionToken, sourceId, request)

            if (response.isSuccessful && response.body()?.status == "success") {
                val sourceDto = response.body()?.data
                if (sourceDto != null) {
                    Result.success(sourceDto.toDomain())
                } else {
                    Result.failure(Exception("Failed to update income source"))
                }
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to update income source"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSource(sourceId: String): Result<Unit> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = metadataApi.deleteSource(sessionToken, sourceId)

            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to delete income source"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SourceDto.toDomain(): Source {
        return Source(
            id = this.id,
            name = this.name,
            description = this.description,
            type = this.type,
            contactInfo = this.contactInfo,
            sourceIdentifier = this.sourceIdentifier,
            defaultCategoryId = this.defaultCategoryId,
            isGlobal = this.isGlobal,
            isUserSpecific = this.isUserSpecific,
            transactionCount = this.transactionCount
        )
    }
}
