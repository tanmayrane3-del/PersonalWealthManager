package com.example.personalwealthmanager.core.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "AppPrefs"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_USER_EMAIL = "user_email"
    }
    
    fun saveSession(token: String, email: String) {
        prefs.edit().apply {
            putString(KEY_SESSION_TOKEN, token)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }
    
    fun getSessionToken(): String? {
        return prefs.getString(KEY_SESSION_TOKEN, null)
    }
    
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    fun isLoggedIn(): Boolean {
        return getSessionToken() != null
    }
    
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
