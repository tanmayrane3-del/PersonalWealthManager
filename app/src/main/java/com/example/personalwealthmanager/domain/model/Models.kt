package com.example.personalwealthmanager.domain.model

data class User(
    val email: String,
    val sessionToken: String
)

data class Transaction(
    val transactionId: String,
    val amount: String,
    val date: String,
    val time: String,
    val type: String, // "income" or "expense"
    val categoryName: String,
    val categoryIcon: String?,
    val sourceName: String?,
    val recipientName: String?,
    val paymentMethod: String,
    val transactionReference: String?,
    val tags: List<String>,
    val notes: String?
)

data class Category(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val type: String? = null,  // Type is determined by which endpoint is called, not from response
    val isGlobal: Boolean = false,
    val isUserSpecific: Boolean = false,
    val transactionCount: Int = 0
)

data class Source(
    val id: String,
    val name: String,
    val description: String? = null,
    val type: String? = null,
    val contactInfo: String? = null,
    val sourceIdentifier: String? = null,
    val defaultCategoryId: String? = null,
    val isGlobal: Boolean = false,
    val isUserSpecific: Boolean = false,
    val transactionCount: Int = 0
)

data class Recipient(
    val id: String,
    val name: String,
    val type: String? = null,
    val description: String? = null,
    val contactInfo: String? = null,
    val isFavorite: Boolean = false,
    val isGlobal: Boolean = false,
    val isUserSpecific: Boolean = false,
    val transactionCount: Int = 0,
    val paymentIdentifiers: List<String> = emptyList(),
    val defaultCategoryId: String? = null
)

data class StockHolding(
    val id: String,
    val tradingSymbol: String,
    val exchange: String,
    val quantity: Int,
    val averagePrice: Double,
    val lastPrice: Double,
    val currentValue: Double,
    val pnl: Double,
    val pnlPercentage: Double,
    val dayChange: Double,
    val dayChangePercentage: Double,
    val closePrice: Double,
    val lastSyncedAt: String?
)

data class TransactionMetadata(
    val incomeCategories: List<Category>,
    val expenseCategories: List<Category>,
    val sources: List<Source>,
    val recipients: List<Recipient>
)

data class MetalHolding(
    val id: String,
    val metalType: String,
    val subType: String?,
    val label: String,
    val quantityGrams: Double,
    val purity: String,
    val notes: String?,
    val currentValue: Double
)

data class MetalRates(
    val gold22kPerGram: Double,
    val gold24kPerGram: Double,
    val fetchedAt: String
)
