package com.example.personalwealthmanager.presentation.stocks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.StockHolding
import com.example.personalwealthmanager.domain.model.StocksPortfolioSummary
import com.example.personalwealthmanager.presentation.zerodha.SetupZerodhaActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class StocksActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getActiveNavItem() = BottomNavItem.NETWORTH
    override fun getSelfButtonId() = R.id.btnStocks

    private val viewModel: StocksViewModel by viewModels()

    private lateinit var rvHoldings: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: StocksAdapter

    // Summary views
    private lateinit var tvPortfolioValue: TextView
    private lateinit var tvTrendBadge: TextView
    private lateinit var ivTrendIcon: ImageView
    private lateinit var tvTotalInvested: TextView
    private lateinit var tvTotalPnl: TextView
    private lateinit var tvDayChange: TextView
    private lateinit var tvPersonalCagr: TextView
    private lateinit var tvProjected1y: TextView
    private lateinit var tvProjected3y: TextView
    private lateinit var tvProjected5y: TextView
    private lateinit var tvCagr1yPct: TextView
    private lateinit var tvCagr3yPct: TextView
    private lateinit var tvCagr5yPct: TextView
    private lateinit var sparkline1y: SparklineView
    private lateinit var sparkline3y: SparklineView
    private lateinit var sparkline5y: SparklineView

    // Collapsible section views
    private lateinit var cardSecondaryStatsInner: LinearLayout
    private lateinit var cardProjectionsInner: LinearLayout
    private lateinit var headerSecondaryStats: LinearLayout
    private lateinit var contentSecondaryStats: LinearLayout
    private lateinit var ivExpandSecondary: ImageView
    private lateinit var headerProjections: LinearLayout
    private lateinit var contentProjections: LinearLayout
    private lateinit var ivExpandProjections: ImageView
    private lateinit var tvHoldingsTitle: TextView

    private val expandTransition by lazy {
        TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(Fade())
            ordering = TransitionSet.ORDERING_TOGETHER
            duration = 280
            interpolator = DecelerateInterpolator()
        }
    }

    private val currencyFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val currencyFormatNoDecimal = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stocks)

        bindViews()

        adapter = StocksAdapter(emptyList())
        rvHoldings.layoutManager = LinearLayoutManager(this)
        rvHoldings.adapter = adapter

        setupCollapsibleSections()
        setupDrawerMenu()
        setupBottomNav()
        observeState()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { onBackPressed() }
        findViewById<ImageButton>(R.id.btnSync).setOnClickListener { viewModel.syncHoldings() }

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

    private fun bindViews() {
        rvHoldings       = findViewById(R.id.rvHoldings)
        tvEmptyState      = findViewById(R.id.tvEmptyState)
        progressBar       = findViewById(R.id.progressBar)
        tvPortfolioValue  = findViewById(R.id.tvPortfolioValue)
        tvTrendBadge      = findViewById(R.id.tvTrendBadge)
        ivTrendIcon       = findViewById(R.id.ivTrendIcon)
        tvTotalInvested   = findViewById(R.id.tvTotalInvested)
        tvTotalPnl        = findViewById(R.id.tvTotalPnl)
        tvDayChange       = findViewById(R.id.tvDayChange)
        tvPersonalCagr    = findViewById(R.id.tvPersonalCagr)
        tvProjected1y     = findViewById(R.id.tvProjected1y)
        tvProjected3y     = findViewById(R.id.tvProjected3y)
        tvProjected5y     = findViewById(R.id.tvProjected5y)
        tvCagr1yPct       = findViewById(R.id.tvCagr1yPct)
        tvCagr3yPct       = findViewById(R.id.tvCagr3yPct)
        tvCagr5yPct       = findViewById(R.id.tvCagr5yPct)
        sparkline1y       = findViewById(R.id.sparkline1y)
        sparkline3y       = findViewById(R.id.sparkline3y)
        sparkline5y       = findViewById(R.id.sparkline5y)
        cardSecondaryStatsInner = findViewById(R.id.cardSecondaryStatsInner)
        cardProjectionsInner    = findViewById(R.id.cardProjectionsInner)
        headerSecondaryStats    = findViewById(R.id.headerSecondaryStats)
        contentSecondaryStats   = findViewById(R.id.contentSecondaryStats)
        ivExpandSecondary       = findViewById(R.id.ivExpandSecondary)
        headerProjections       = findViewById(R.id.headerProjections)
        contentProjections      = findViewById(R.id.contentProjections)
        ivExpandProjections     = findViewById(R.id.ivExpandProjections)
        tvHoldingsTitle         = findViewById(R.id.tvHoldingsTitle)
    }

    private fun setupCollapsibleSections() {
        headerSecondaryStats.setOnClickListener {
            val opening = contentSecondaryStats.visibility != View.VISIBLE
            TransitionManager.beginDelayedTransition(cardSecondaryStatsInner, expandTransition)
            contentSecondaryStats.visibility = if (opening) View.VISIBLE else View.GONE
            ivExpandSecondary.animate().rotation(if (opening) 180f else 0f).setDuration(280)
                .setInterpolator(DecelerateInterpolator()).start()
        }
        headerProjections.setOnClickListener {
            val opening = contentProjections.visibility != View.VISIBLE
            TransitionManager.beginDelayedTransition(cardProjectionsInner, expandTransition)
            contentProjections.visibility = if (opening) View.VISIBLE else View.GONE
            ivExpandProjections.animate().rotation(if (opening) 180f else 0f).setDuration(280)
                .setInterpolator(DecelerateInterpolator()).start()
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
            viewModel.summary.collect { summary ->
                if (summary != null) bindSummary(summary)
            }
        }

        lifecycleScope.launch {
            viewModel.authUrlState.collect { url ->
                if (url != null) {
                    viewModel.clearAuthUrl()
                    CustomTabsIntent.Builder().build()
                        .launchUrl(this@StocksActivity, Uri.parse(url))
                }
            }
        }
    }

    private fun bindSummary(s: StocksPortfolioSummary) {
        // Hero card
        tvPortfolioValue.text = "₹${currencyFormat.format(s.totalPortfolioValue)}"

        val dayPct = if (s.totalPortfolioValue != 0.0)
            (s.todayPnl / s.totalPortfolioValue) * 100 else 0.0
        val trendPos = dayPct >= 0
        tvTrendBadge.text = "${if (trendPos) "+" else ""}${String.format("%.2f", dayPct)}%"
        ivTrendIcon.setImageResource(if (trendPos) R.drawable.ic_trending_up else R.drawable.ic_trending_down)

        // Secondary stats: invested and P&L come from holdings list — updated in showHoldings()
        // Day change from summary
        val daySign = if (s.todayPnl >= 0) "+" else "-"
        tvDayChange.text = "$daySign₹${currencyFormat.format(abs(s.todayPnl))}"
        tvDayChange.setTextColor(getColor(if (s.todayPnl >= 0) R.color.amount_positive else R.color.amount_negative))

        // Personal CAGR (1Y blended)
        if (s.totalPortfolioValue > 0 && s.projected1y > 0) {
            val cagr1y = (s.projected1y / s.totalPortfolioValue - 1) * 100
            val sign1y = if (cagr1y >= 0) "+" else ""
            tvPersonalCagr.text = "$sign1y${String.format("%.2f", cagr1y)}%"
            tvPersonalCagr.setTextColor(getColor(if (cagr1y >= 0) R.color.teal_800 else R.color.amount_negative))
        }

        // Projection rows
        bindProjectionRow(tvProjected1y, tvCagr1yPct, sparkline1y,
            s.totalPortfolioValue, s.projected1y, 1)
        bindProjectionRow(tvProjected3y, tvCagr3yPct, sparkline3y,
            s.totalPortfolioValue, s.projected3y, 3)
        bindProjectionRow(tvProjected5y, tvCagr5yPct, sparkline5y,
            s.totalPortfolioValue, s.projected5y, 5)
    }

    private fun bindProjectionRow(
        tvValue: TextView,
        tvPct: TextView,
        sparkline: SparklineView,
        current: Double,
        projected: Double,
        years: Int
    ) {
        tvValue.text = "₹${currencyFormatNoDecimal.format(projected)}"

        val totalGrowthPct = if (current > 0) (projected / current - 1) * 100 else 0.0
        val pctSign = if (totalGrowthPct >= 0) "+" else ""
        tvPct.text = "$pctSign${String.format("%.1f", totalGrowthPct)}%"
        tvPct.setTextColor(getColor(if (totalGrowthPct >= 0) R.color.amount_positive else R.color.amount_negative))

        if (current > 0 && projected > 0) {
            val data = when (years) {
                1 -> SparklineView.monthly1Y(current, projected)
                3 -> SparklineView.quarterly3Y(current, projected)
                else -> SparklineView.semiAnnual5Y(current, projected)
            }
            sparkline.setData(data)
        }
    }

    private fun initiateZerodhaAuth() {
        hideLoading()
        viewModel.fetchAuthUrl()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        // If holdings are already displayed, keep them visible during refresh
        // so the user doesn't see a blank screen on every sync.
        if (adapter.itemCount == 0) {
            rvHoldings.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
        }
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showHoldings(holdings: List<StockHolding>) {
        hideLoading()
        if (holdings.isEmpty()) {
            showEmpty()
            return
        }
        rvHoldings.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        adapter.updateHoldings(holdings)

        // Update holdings count in header
        tvHoldingsTitle.text = "Holdings (${holdings.size})"

        // Compute invested + total P&L from holdings list and bind to secondary stats
        val totalInvested = holdings.sumOf { it.averagePrice * it.quantity }
        val totalPnl = holdings.sumOf { it.pnl }
        tvTotalInvested.text = "₹${currencyFormat.format(totalInvested)}"
        val pnlSign = if (totalPnl >= 0) "+" else "-"
        tvTotalPnl.text = "$pnlSign₹${currencyFormat.format(abs(totalPnl))}"
        tvTotalPnl.setTextColor(getColor(if (totalPnl >= 0) R.color.amount_positive else R.color.amount_negative))
    }

    private fun showEmpty() {
        rvHoldings.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }
}
