package com.example.personalwealthmanager.presentation.zerodha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.presentation.stocks.StocksActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupZerodhaActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getSelfButtonId() = R.id.btnConnectZerodha

    private val viewModel: SetupZerodhaViewModel by viewModels()

    private lateinit var etApiKey: EditText
    private lateinit var etApiSecret: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCheckHoldings: Button
    private lateinit var btnOpenKiteApps: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_zerodha)

        etApiKey = findViewById(R.id.etApiKey)
        etApiSecret = findViewById(R.id.etApiSecret)
        btnSave = findViewById(R.id.btnSave)
        btnCheckHoldings = findViewById(R.id.btnCheckHoldings)
        btnOpenKiteApps = findViewById(R.id.btnOpenKiteApps)

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        btnSave.setOnClickListener {
            val apiKey = etApiKey.text.toString().trim()
            val apiSecret = etApiSecret.text.toString().trim()
            when {
                apiKey.isEmpty() -> Toast.makeText(this, getString(R.string.api_key_required), Toast.LENGTH_SHORT).show()
                apiSecret.isEmpty() -> Toast.makeText(this, getString(R.string.api_secret_required), Toast.LENGTH_SHORT).show()
                else -> viewModel.saveCredentials(apiKey, apiSecret)
            }
        }

        btnCheckHoldings.setOnClickListener {
            startActivity(Intent(this, StocksActivity::class.java))
        }

        btnOpenKiteApps.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zerodha.com/products/api/"))
            startActivity(intent)
        }

        setupDrawerMenu()
        setupBottomNav()
        observeState()

        // Prefill existing credentials if available
        viewModel.loadCredentials()
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is SetupZerodhaState.Idle -> {
                        btnSave.isEnabled = true
                        btnSave.text = getString(R.string.save_credentials)
                    }
                    is SetupZerodhaState.CredentialsLoaded -> {
                        etApiKey.setText(state.apiKey)
                        etApiSecret.setText(state.apiSecret)
                        btnCheckHoldings.visibility = View.VISIBLE
                        viewModel.resetState()
                    }
                    is SetupZerodhaState.Loading -> {
                        btnSave.isEnabled = false
                        btnSave.text = getString(R.string.loading)
                    }
                    is SetupZerodhaState.Success -> {
                        btnSave.isEnabled = true
                        btnSave.text = getString(R.string.save_credentials)
                        Toast.makeText(this@SetupZerodhaActivity, getString(R.string.credentials_saved), Toast.LENGTH_SHORT).show()
                        btnCheckHoldings.visibility = View.VISIBLE
                        viewModel.resetState()
                    }
                    is SetupZerodhaState.Error -> {
                        btnSave.isEnabled = true
                        btnSave.text = getString(R.string.save_credentials)
                        Toast.makeText(this@SetupZerodhaActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }

}
