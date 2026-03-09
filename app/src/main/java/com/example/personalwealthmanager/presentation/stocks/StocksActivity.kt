package com.example.personalwealthmanager.presentation.stocks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import com.example.personalwealthmanager.presentation.zerodha.SetupZerodhaActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StocksActivity : AppCompatActivity() {

    private val viewModel: StocksViewModel by viewModels()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvHoldings: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabSync: FloatingActionButton
    private lateinit var adapter: StocksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stocks)

        drawerLayout = findViewById(R.id.drawerLayout)
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

        setupNavigationDrawer()
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
            // Already on stocks screen
        }

        setupExpandable(
            headerView,
            R.id.btnSetupDemat, R.id.ivSetupDematExpand, R.id.setupDematChildItems
        )

        headerView.findViewById<Button>(R.id.btnConnectZerodha)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SetupZerodhaActivity::class.java))
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
