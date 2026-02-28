package com.example.personalwealthmanager.core.network

object ApiConfig {
    const val BASE_URL = "https://wealth-backend-demo.onrender.com/"
    
    // Endpoints
    object Auth {
        const val LOGIN = "api/auth/login"
        const val REGISTER = "api/auth/register"
        const val LOGOUT = "api/auth/logout"
    }
    
    object Transactions {
        const val INCOME = "api/income"
        const val EXPENSES = "api/expenses"
    }
    
    object Metadata {
        const val CATEGORIES_INCOME = "api/categories/income"
        const val CATEGORIES_EXPENSE = "api/categories/expense"
        const val SOURCES = "api/sources"
        const val RECIPIENTS = "api/recipients"
    }

    object Sms {
        const val PARSE = "api/sms/parse"
    }
}
