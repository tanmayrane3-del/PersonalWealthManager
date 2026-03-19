package com.example.personalwealthmanager.presentation.auth.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.databinding.ActivityLoginBinding
import com.example.personalwealthmanager.presentation.auth.register.RegisterActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeState()
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.login(email, password)
        }

        binding.tvRegister.text = android.text.SpannableString("New user? Register here").apply {
            val start = indexOf("Register here")
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, length, 0)
            setSpan(android.text.style.UnderlineSpan(), start, length, 0)
        }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
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
                binding.btnLogin.isEnabled = !state.isLoading

                // Success - navigate to dashboard
                if (state.isSuccess) {
                    navigateToDashboard()
                }

                // Show error
                state.error?.let { error ->
                    Toast.makeText(this@LoginActivity, error, Toast.LENGTH_LONG).show()
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