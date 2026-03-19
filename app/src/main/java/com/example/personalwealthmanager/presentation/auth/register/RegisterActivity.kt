package com.example.personalwealthmanager.presentation.auth.register

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.databinding.ActivityRegistrationBinding
import com.example.personalwealthmanager.presentation.auth.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeState()
    }

    private fun setupUI() {
        binding.btnSignUp.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            viewModel.register(email, password, confirmPassword)
        }

        binding.tvLogin.text = android.text.SpannableString("Already have an account? Login here").apply {
            val start = indexOf("Login here")
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, length, 0)
            setSpan(android.text.style.UnderlineSpan(), start, length, 0)
        }
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.tvPrivacyPolicy.text = android.text.Html.fromHtml("<u>Privacy Policy</u>", android.text.Html.FROM_HTML_MODE_LEGACY)
        binding.tvPrivacyPolicy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tanmayrane3-del.github.io/PersonalWealthManager-Privacy")))
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                // Show/hide progress bar
                binding.progressBar.visibility = if (state.isLoading) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Enable/disable button
                binding.btnSignUp.isEnabled = !state.isLoading

                // Success - navigate to dashboard
                if (state.isSuccess) {
                    navigateToDashboard()
                }

                // Show error
                state.error?.let { error ->
                    Toast.makeText(this@RegisterActivity, error, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun navigateToDashboard() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}