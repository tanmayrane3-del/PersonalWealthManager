package com.example.personalwealthmanager.presentation.stocks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.presentation.zerodha.SetupZerodhaActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.widget.ImageView

@AndroidEntryPoint
class StocksActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getSelfButtonId() = R.id.btnStocks

    private val viewModel: StocksViewModel by viewModels()

    private lateinit var rvHoldings: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabSync: FloatingActionButton
    private lateinit var adapter: StocksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stocks)

        rvHoldings = findViewById(R.id.rvHoldings)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        progressBar = findViewById(R.id.progressBar)
        fabSync = findViewById(R.id.fabSync)

        adapter = StocksAdapter(emptyList())
        rvHoldings.layoutManager = LinearLayoutManager(this)
        rvHoldings.adapter = adapter

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        fabSync.setOnClickListener {
            viewModel.syncHoldings()
        }

        setupDrawerMenu()
        observeState()

        viewModel.loadHoldings()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }

    // Called when wealthapp://auth/success lands after Zerodha login.
    // Chrome Custom Tab intercepts the custom scheme, closes itself, and
    // delivers the intent here (activity is singleTop so it isn't recreated).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val data = intent.data
        if (data?.scheme == "wealthapp" && data.host == "auth" && data.path == "/callback") {
            val requestToken = data.getQueryParameter("request_token")
            if (!requestToken.isNullOrBlank()) {
                viewModel.exchangeAndSync(requestToken)
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is StocksState.Idle -> Unit
                    is StocksState.Loading -> showLoading()
                    is StocksState.Success -> showHoldings(state.holdings)
                    is StocksState.Error -> {
                        hideLoading()
                        showEmpty()
                        Toast.makeText(this@StocksActivity, getString(R.string.sync_failed, state.message), Toast.LENGTH_LONG).show()
                    }
                    is StocksState.NotAuthenticated -> initiateZerodhaAuth()
                    is StocksState.CredentialsNotFound -> {
                        hideLoading()
                        Toast.makeText(this@StocksActivity, getString(R.string.zerodha_not_connected), Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@StocksActivity, SetupZerodhaActivity::class.java))
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.authUrlState.collect { url ->
                if (url != null) {
                    viewModel.clearAuthUrl()
                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.launchUrl(this@StocksActivity, Uri.parse(url))
                }
            }
        }
    }

    private fun initiateZerodhaAuth() {
        hideLoading()
        viewModel.fetchAuthUrl()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        rvHoldings.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showHoldings(holdings: List<com.example.personalwealthmanager.domain.model.StockHolding>) {
        hideLoading()
        if (holdings.isEmpty()) {
            showEmpty()
        } else {
            rvHoldings.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            adapter.updateHoldings(holdings)
        }
    }

    private fun showEmpty() {
        rvHoldings.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }
}
