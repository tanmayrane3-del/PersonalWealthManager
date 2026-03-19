package com.example.personalwealthmanager.presentation.zerodha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.stocks.StocksActivity
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupZerodhaActivity : AppCompatActivity() {

    private val viewModel: SetupZerodhaViewModel by viewModels()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var etApiKey: EditText
    private lateinit var etApiSecret: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCheckHoldings: Button
    private lateinit var btnOpenKiteApps: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_zerodha)

        drawerLayout = findViewById(R.id.drawerLayout)
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

        setupNavigationDrawer()
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

    private fun setupNavigationDrawer() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)

        headerView.findViewById<TextView>(R.id.tvUserEmail)?.text =
            getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", "user@example.com")

        headerView.findViewById<Button>(R.id.btnDashboard)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        headerView.findViewById<Button>(R.id.btnTransactions)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, TransactionsActivity::class.java))
            finish()
        }

        setupExpandable(
            headerView,
            R.id.btnManagement, R.id.ivManagementExpand, R.id.managementChildItems
        )

        headerView.findViewById<Button>(R.id.btnCategoryManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, CategoryManagementActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnSourceManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SourceManagementActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnRecipientManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, RecipientManagementActivity::class.java))
        }

        setupExpandable(
            headerView,
            R.id.btnAssets, R.id.ivAssetsExpand, R.id.assetsChildItems
        )

        headerView.findViewById<Button>(R.id.btnStocks)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, StocksActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnMetals)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, com.example.personalwealthmanager.presentation.metals.MetalsActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnMutualFunds)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, com.example.personalwealthmanager.presentation.mutualfunds.MutualFundsActivity::class.java))
        }

        setupExpandable(
            headerView,
            R.id.btnSetupDemat, R.id.ivSetupDematExpand, R.id.setupDematChildItems
        )

        headerView.findViewById<Button>(R.id.btnConnectZerodha)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            // Already on this screen
        }

        headerView.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, com.example.personalwealthmanager.presentation.auth.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupExpandable(headerView: View, btnId: Int, iconId: Int, containerId: Int) {
        val btn = headerView.findViewById<Button>(btnId)
        val icon = headerView.findViewById<ImageView>(iconId)
        val container = headerView.findViewById<LinearLayout>(containerId)

        btn?.setOnClickListener {
            if (container?.visibility == View.VISIBLE) {
                container.visibility = View.GONE
                icon?.setImageResource(R.drawable.ic_expand_more)
            } else {
                container?.visibility = View.VISIBLE
                icon?.setImageResource(R.drawable.ic_expand_less)
            }
        }
        icon?.setOnClickListener { btn?.performClick() }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
