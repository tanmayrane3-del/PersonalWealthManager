package com.example.personalwealthmanager.domain.repository

import com.example.personalwealthmanager.domain.model.Recipient

interface RecipientRepository {
    suspend fun getRecipients(): Result<List<Recipient>>

    suspend fun createRecipient(
        name: String,
        type: String?,
        description: String?,
        contactInfo: String?,
        isFavorite: Boolean,
        paymentIdentifier: String? = null,
        defaultCategoryId: String? = null
    ): Result<Recipient>

    suspend fun updateRecipient(
        recipientId: String,
        name: String?,
        type: String?,
        description: String?,
        contactInfo: String?,
        isFavorite: Boolean?,
        paymentIdentifier: String? = null,
        defaultCategoryId: String? = null
    ): Result<Recipient>

    suspend fun deleteRecipient(recipientId: String): Result<Unit>

    suspend fun lookupByPaymentIdentifier(identifier: String): Result<Recipient?>
}
