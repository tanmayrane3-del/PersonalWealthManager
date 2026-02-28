package com.example.personalwealthmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import com.example.personalwealthmanager.ApiConfig
import com.example.personalwealthmanager.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val errorDismissHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if already logged in
        val sessionToken = getSessionToken()
        if (sessionToken != null) {
            navigateToDashboard()
            return
        }

        setupClickListeners()
        setupErrorBanner()
    }

    private fun setupErrorBanner() {
        val errorBannerCard = binding.root.findViewById<CardView>(R.id.errorBanner)
        errorBannerCard.findViewById<ImageView>(R.id.btnCloseError)?.setOnClickListener {
            hideErrorBanner()
        }
    }

    private fun showErrorBanner(message: String) {
        val errorBannerCard = binding.root.findViewById<CardView>(R.id.errorBanner)
        errorBannerCard.visibility = View.VISIBLE
        errorBannerCard.findViewById<TextView>(R.id.tvErrorMessage)?.text = message

        errorDismissHandler.removeCallbacksAndMessages(null)
        errorDismissHandler.postDelayed({
            hideErrorBanner()
        }, 5000)
    }

    private fun hideErrorBanner() {
        val errorBannerCard = binding.root.findViewById<CardView>(R.id.errorBanner)
        errorBannerCard.visibility = View.GONE
        errorDismissHandler.removeCallbacksAndMessages(null)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            hideErrorBanner()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            showErrorBanner(getString(R.string.email_required))
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorBanner(getString(R.string.invalid_email))
            return false
        }
        if (password.isEmpty()) {
            showErrorBanner(getString(R.string.password_required))
            return false
        }
        return true
    }

    private fun loginUser(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.logging_in)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val validateUrl = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.VALIDATE_LOGIN)
                val validateConnection = validateUrl.openConnection() as HttpURLConnection
                validateConnection.requestMethod = "POST"
                validateConnection.setRequestProperty("Content-Type", "application/json")
                validateConnection.doOutput = true
                validateConnection.connectTimeout = 10000
                validateConnection.readTimeout = 10000

                val validateJson = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                validateConnection.outputStream.use { os ->
                    os.write(validateJson.toString().toByteArray())
                }

                val responseCode = validateConnection.responseCode
                val validateResponse = if (responseCode == HttpURLConnection.HTTP_OK) {
                    validateConnection.inputStream.bufferedReader().readText()
                } else {
                    validateConnection.errorStream?.bufferedReader()?.readText() ?: ""
                }

                val validateData = JSONObject(validateResponse)

                withContext(Dispatchers.Main) {
                    if (validateData.getString("status") == "success") {
                        val userData = validateData.getJSONObject("data")
                        val userId = userData.getString("user_id")
                        createSession(userId, email)
                    } else {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = getString(R.string.login_button)
                        showErrorBanner(getString(R.string.invalid_credentials))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login_button)
                    showErrorBanner(getString(R.string.login_failed, e.message))
                }
            }
        }
    }

    private fun createSession(userId: String, email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionUrl = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.CREATE_SESSION)
                val sessionConnection = sessionUrl.openConnection() as HttpURLConnection
                sessionConnection.requestMethod = "POST"
                sessionConnection.setRequestProperty("Content-Type", "application/json")
                sessionConnection.doOutput = true
                sessionConnection.connectTimeout = 10000
                sessionConnection.readTimeout = 10000

                val sessionJson = JSONObject().apply {
                    put("user_id", userId)
                    put("ip_address", "0.0.0.0")
                    put("user_agent", "Android App")
                }

                sessionConnection.outputStream.use { os ->
                    os.write(sessionJson.toString().toByteArray())
                }

                val sessionResponse = sessionConnection.inputStream.bufferedReader().readText()
                val sessionData = JSONObject(sessionResponse)

                withContext(Dispatchers.Main) {
                    if (sessionData.getString("status") == "success") {
                        val sessionToken = sessionData.getJSONObject("data").getString("session_token")
                        saveSessionToken(sessionToken)
                        saveUserEmail(email)
                        navigateToDashboard()
                    } else {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = getString(R.string.login_button)
                        showErrorBanner(getString(R.string.session_creation_failed))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login_button)
                    showErrorBanner(getString(R.string.session_error, e.message))
                }
            }
        }
    }

    private fun saveSessionToken(token: String) {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            putString("session_token", token)
        }
    }

    private fun saveUserEmail(email: String) {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            putString("user_email", email)
        }
    }

    private fun getSessionToken(): String? {
        return getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("session_token", null)
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        errorDismissHandler.removeCallbacksAndMessages(null)
    }
}