package com.example.personalwealthmanager

object ApiConfig {
    const val BASE_URL = "https://wealth-backend-demo.onrender.com"
    const val HEADER_SESSION_TOKEN = "x-session-token"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val CONTENT_TYPE_JSON = "application/json"

    object Endpoints {
        const val VALIDATE_LOGIN = "/api/users/validate-login"
        const val CREATE_USER = "/api/users"
        const val CREATE_SESSION = "/api/sessions/login"
        const val LOGOUT = "/api/sessions/logout"
        const val GET_INCOME_SUMMARY = "/api/income/summary"
        const val GET_EXPENSE_SUMMARY = "/api/expenses/summary"
        const val GET_INCOME = "/api/income"
        const val GET_EXPENSES = "/api/expenses"
        const val GET_INCOME_CATEGORIES = "/api/categories/income"
        const val GET_EXPENSE_CATEGORIES = "/api/categories/expense"
        const val GET_SOURCES = "/api/sources"
        const val GET_INCOME_SOURCES = "/api/sources"
        const val GET_RECIPIENTS = "/api/recipients"
        const val GET_TRANSACTIONS = "/api/transactions"
        const val ADD_INCOME = "/api/income"
        const val ADD_EXPENSE = "/api/expenses"
    }
}
