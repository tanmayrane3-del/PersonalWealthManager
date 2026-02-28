package com.example.personalwealthmanager.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("income_id") val incomeId: String?,
    @SerializedName("expense_id") val expenseId: String?,
    val amount: String,
    val date: String,
    val time: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("category_icon") val categoryIcon: String?,
    @SerializedName("source_name") val sourceName: String?,
    @SerializedName("recipient_name") val recipientName: String?,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("transaction_reference") val transactionReference: String?,
    val tags: List<String>?,
    val notes: String?
)

data class CreateTransactionRequest(
    val date: String,
    val time: String,
    val amount: String,
    val currency: String = "INR",
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("source_id") val sourceId: String?,
    @SerializedName("recipient_id") val recipientId: String?,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("transaction_reference") val transactionReference: String?,
    val tags: List<String>?,
    val notes: String?
)

data class UpdateTransactionRequest(
    val date: String,
    val time: String,
    val amount: String,
    val currency: String = "INR",
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("source_id") val sourceId: String?,
    @SerializedName("recipient_id") val recipientId: String?,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("transaction_reference") val transactionReference: String?,
    val tags: List<String>?,
    val notes: String?
)

// Response from create/update only contains the ID
data class TransactionIdResponse(
    @SerializedName("income_id") val incomeId: String?,
    @SerializedName("expense_id") val expenseId: String?
)

data class CategoryDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val type: String? = null,  // Type is determined by which endpoint is called
    @SerializedName("is_global") val isGlobal: Boolean = false,
    @SerializedName("is_user_specific") val isUserSpecific: Boolean = false,
    @SerializedName("transaction_count") val transactionCount: Int = 0
)

data class CreateCategoryRequest(
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String = "#FF5722",
    @SerializedName("display_order") val displayOrder: Int = 0
)

data class UpdateCategoryRequest(
    val name: String? = null,
    val description: String? = null,
    val icon: String? = null
)

data class SourceDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val type: String? = null,
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("source_identifier") val sourceIdentifier: String? = null,
    @SerializedName("default_category_id") val defaultCategoryId: String? = null,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("is_global") val isGlobal: Boolean = false,
    @SerializedName("is_user_specific") val isUserSpecific: Boolean = false,
    @SerializedName("transaction_count") val transactionCount: Int = 0
)

data class RecipientDto(
    val id: String,
    val name: String,
    val type: String? = null,
    val description: String? = null,
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("is_global") val isGlobal: Boolean = false,
    @SerializedName("is_user_specific") val isUserSpecific: Boolean = false,
    @SerializedName("transaction_count") val transactionCount: Int = 0,
    @SerializedName("payment_identifier") val paymentIdentifier: String? = null,
    @SerializedName("category_id") val defaultCategoryId: String? = null
)

data class CreateSourceRequest(
    val name: String,
    val description: String? = null,
    val type: String? = null,
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("source_identifier") val sourceIdentifier: String? = null,
    @SerializedName("default_category_id") val defaultCategoryId: String? = null
)

data class UpdateSourceRequest(
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("source_identifier") val sourceIdentifier: String? = null,
    @SerializedName("default_category_id") val defaultCategoryId: String? = null
)

data class CreateRecipientRequest(
    val name: String,
    val type: String? = null,
    val description: String? = null,
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    @SerializedName("payment_identifier") val paymentIdentifier: String? = null,
    @SerializedName("category_id") val defaultCategoryId: String? = null
)

data class UpdateRecipientRequest(
    val name: String? = null,
    val type: String? = null,
    val description: String? = null,
    @SerializedName("contact_info") val contactInfo: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean? = null,
    @SerializedName("payment_identifier") val paymentIdentifier: String? = null,
    @SerializedName("category_id") val defaultCategoryId: String? = null
)