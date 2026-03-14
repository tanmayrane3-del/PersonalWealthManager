package com.example.personalwealthmanager.data.repository

import com.example.personalwealthmanager.core.utils.SessionManager
import com.example.personalwealthmanager.data.remote.api.MetadataApi
import com.example.personalwealthmanager.data.remote.dto.CreateRecipientRequest
import com.example.personalwealthmanager.data.remote.dto.RecipientDto
import com.example.personalwealthmanager.data.remote.dto.UpdateRecipientRequest
import com.example.personalwealthmanager.domain.model.Recipient
import com.example.personalwealthmanager.domain.repository.RecipientRepository
import javax.inject.Inject

class RecipientRepositoryImpl @Inject constructor(
    private val metadataApi: MetadataApi,
    private val sessionManager: SessionManager
) : RecipientRepository {

    override suspend fun getRecipients(): Result<List<Recipient>> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = metadataApi.getRecipients(sessionToken)

            if (response.isSuccessful && response.body()?.status == "success") {
                val recipients = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                Result.success(recipients)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to fetch recipients"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createRecipient(
        name: String,
        type: String?,
        description: String?,
        contactInfo: String?,
        isFavorite: Boolean,
        paymentIdentifiers: List<String>,
        defaultCategoryId: String?
    ): Result<Recipient> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val request = CreateRecipientRequest(
                name = name,
                type = type,
                description = description,
                contactInfo = contactInfo,
                isFavorite = isFavorite,
                paymentIdentifiers = paymentIdentifiers,
                defaultCategoryId = defaultCategoryId
            )

            val response = metadataApi.createRecipient(sessionToken, request)

            if (response.isSuccessful && response.body()?.status == "success") {
                val recipientDto = response.body()?.data
                if (recipientDto != null) {
                    Result.success(recipientDto.toDomain())
                } else {
                    Result.failure(Exception("Failed to create recipient"))
                }
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to create recipient"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecipient(
        recipientId: String,
        name: String?,
        type: String?,
        description: String?,
        contactInfo: String?,
        isFavorite: Boolean?,
        paymentIdentifiers: List<String>,
        defaultCategoryId: String?
    ): Result<Recipient> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val request = UpdateRecipientRequest(
                name = name,
                type = type,
                description = description,
                contactInfo = contactInfo,
                isFavorite = isFavorite,
                paymentIdentifiers = paymentIdentifiers,
                defaultCategoryId = defaultCategoryId
            )

            val response = metadataApi.updateRecipient(sessionToken, recipientId, request)

            if (response.isSuccessful && response.body()?.status == "success") {
                val recipientDto = response.body()?.data
                if (recipientDto != null) {
                    Result.success(recipientDto.toDomain())
                } else {
                    Result.failure(Exception("Failed to update recipient"))
                }
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to update recipient"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecipient(recipientId: String): Result<Unit> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = metadataApi.deleteRecipient(sessionToken, recipientId)

            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(Unit)
            } else {
                val errorMessage = response.body()?.reason ?: "Failed to delete recipient"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun lookupByPaymentIdentifier(identifier: String): Result<Recipient?> {
        return try {
            val sessionToken = sessionManager.getSessionToken()
                ?: return Result.failure(Exception("No session token"))

            val response = metadataApi.lookupRecipientByPaymentIdentifier(sessionToken, identifier)

            if (response.isSuccessful && response.body()?.status == "success") {
                val dto = response.body()?.data
                Result.success(dto?.toDomain())
            } else {
                val errorMessage = response.body()?.reason ?: "Lookup failed"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun RecipientDto.toDomain(): Recipient {
        return Recipient(
            id = this.id,
            name = this.name,
            type = this.type,
            description = this.description,
            contactInfo = this.contactInfo,
            isFavorite = this.isFavorite,
            isGlobal = this.isGlobal,
            isUserSpecific = this.isUserSpecific,
            transactionCount = this.transactionCount,
            paymentIdentifiers = this.paymentIdentifier
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList(),
            defaultCategoryId = this.defaultCategoryId
        )
    }
}
