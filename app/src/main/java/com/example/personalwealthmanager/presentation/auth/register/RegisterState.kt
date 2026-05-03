package com.pwm.personalwealthmanager.presentation.auth.register

data class RegisterState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)