package com.example.personalwealthmanager

data class Transaction(
    val transactionId: String,
    val transactionType: String, // "income" or "expense"
    val date: String,
    val time: String,
    val amount: Double,
    val currency: String,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?,
    val sourceName: String?,
    val recipientName: String?,
    val paymentMethod: String?,
    val notes: String?,
    val tags: List<String>?
)