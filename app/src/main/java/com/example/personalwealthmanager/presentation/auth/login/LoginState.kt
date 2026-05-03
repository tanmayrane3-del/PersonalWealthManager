package com.pwm.personalwealthmanager.presentation.auth.login

data class LoginState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)