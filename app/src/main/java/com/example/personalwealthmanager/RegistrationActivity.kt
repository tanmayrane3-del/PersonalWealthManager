package com.example.personalwealthmanager

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.personalwealthmanager.ApiConfig
import com.example.personalwealthmanager.databinding.ActivityRegistrationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private val warningDismissHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupWarningBanner()
    }

    private fun setupWarningBanner() {
        val warningBannerCard = binding.root.findViewById<CardView>(R.id.warningBanner)
        warningBannerCard.findViewById<ImageView>(R.id.btnCloseWarning)?.setOnClickListener {
            hideWarningBanner()
        }
    }

    private fun showWarningBanner(title: String, message: String) {
        val warningBannerCard = binding.root.findViewById<CardView>(R.id.warningBanner)
        warningBannerCard.visibility = View.VISIBLE
        warningBannerCard.findViewById<TextView>(R.id.tvWarningTitle)?.text = title
        warningBannerCard.findViewById<TextView>(R.id.tvWarningMessage)?.text = message

        warningDismissHandler.removeCallbacksAndMessages(null)
        warningDismissHandler.postDelayed({
            hideWarningBanner()
        }, 5000)
    }

    private fun hideWarningBanner() {
        val warningBannerCard = binding.root.findViewById<CardView>(R.id.warningBanner)
        warningBannerCard.visibility = View.GONE
        warningDismissHandler.removeCallbacksAndMessages(null)
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            hideWarningBanner()
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(name, email, phone, password)) {
                registerUser(name, email, phone, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(name: String, email: String, phone: String, password: String): Boolean {
        if (name.isEmpty()) {
            showWarningBanner(
                getString(R.string.missing_information),
                getString(R.string.name_required)
            )
            return false
        }
        if (email.isEmpty()) {
            showWarningBanner(
                getString(R.string.missing_information),
                getString(R.string.email_required)
            )
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showWarningBanner(
                getString(R.string.invalid_email_title),
                getString(R.string.invalid_email_message)
            )
            return false
        }
        if (phone.isEmpty()) {
            showWarningBanner(
                getString(R.string.missing_information),
                getString(R.string.phone_required)
            )
            return false
        }
        if (phone.length < 10) {
            showWarningBanner(
                getString(R.string.invalid_email_title),
                getString(R.string.invalid_phone)
            )
            return false
        }
        if (password.isEmpty()) {
            showWarningBanner(
                getString(R.string.missing_information),
                getString(R.string.password_required)
            )
            return false
        }
        if (password.length < 6) {
            showWarningBanner(
                getString(R.string.weak_password),
                getString(R.string.password_min_length)
            )
            return false
        }
        return true
    }

    private fun registerUser(name: String, email: String, phone: String, password: String) {
        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = getString(R.string.creating_account)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.CREATE_USER)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("full_name", name)
                    put("phone", phone)
                }

                connection.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_CREATED ||
                    responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                }

                val responseData = JSONObject(response)

                withContext(Dispatchers.Main) {
                    if (responseData.getString("status") == "success") {
                        showWarningBanner(
                            getString(R.string.registration_success_title),
                            getString(R.string.registration_success_message)
                        )
                        Handler(Looper.getMainLooper()).postDelayed({
                            finish()
                        }, 2000)
                    } else {
                        binding.btnSignUp.isEnabled = true
                        binding.btnSignUp.text = getString(R.string.sign_up_button)
                        val reason = responseData.optString("reason", "Registration failed")
                        if (reason.contains("already exists", ignoreCase = true) ||
                            reason.contains("duplicate", ignoreCase = true)) {
                            showWarningBanner(
                                getString(R.string.warning_title),
                                getString(R.string.warning_message)
                            )
                        } else {
                            showWarningBanner(
                                getString(R.string.registration_failed_title),
                                reason
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSignUp.isEnabled = true
                    binding.btnSignUp.text = getString(R.string.sign_up_button)
                    showWarningBanner(
                        getString(R.string.error_message),
                        getString(R.string.registration_failed_message, e.message)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        warningDismissHandler.removeCallbacksAndMessages(null)
    }
}