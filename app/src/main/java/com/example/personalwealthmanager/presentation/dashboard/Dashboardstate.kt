package com.example.personalwealthmanager.presentation.dashboard

import com.example.personalwealthmanager.domain.model.Transaction

data class DashboardState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: String = "0.00",
    val totalExpenses: String = "0.00",
    val balance: String = "0.00",
    val error: String? = null
)