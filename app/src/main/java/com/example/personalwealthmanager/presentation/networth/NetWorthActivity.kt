package com.example.personalwealthmanager.presentation.networth

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.data.remote.dto.NetWorthSnapshotDto
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.liabilities.LiabilitiesActivity
import com.example.personalwealthmanager.presentation.metals.MetalsActivity
import com.example.personalwealthmanager.presentation.mutualfunds.MutualFundsActivity
import com.example.personalwealthmanager.presentation.otherassets.OtherAssetsActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.settings.SettingsActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.stocks.StocksActivity
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import com.example.personalwealthmanager.presentation.zerodha.SetupZerodhaActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import androidx.lifecycle.Lifecycle

@AndroidEntryPoint
class NetWorthActivity : AppCompatActivity() {

    private val viewModel: NetWorthViewModel by viewModels()

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvNetWorthValue: TextView
    private lateinit var tvTotalAssets: TextView
    private lateinit var tvTotalLiabilities: TextView
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var chartProgressBar: ProgressBar
    private lateinit var lineChart: LineChart
    private lateinit var tvNoData: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var ivRefresh: ImageView

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private var snapshotDates: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_net_worth)

        drawerLayout = findViewById(R.id.drawerLayout)
        swipeRefresh      = findViewById(R.id.swipeRefresh)
        tvNetWorthValue   = findViewById(R.id.tvNetWorthValue)
        tvTotalAssets     = findViewById(R.id.tvTotalAssets)
        tvTotalLiabilities = findViewById(R.id.tvTotalLiabilities)
        chipGroupPeriod   = findViewById(R.id.chipGroupPeriod)
        chartProgressBar  = findViewById(R.id.chartProgressBar)
        lineChart         = findViewById(R.id.lineChart)
        tvNoData          = findViewById(R.id.tvNoData)
        tvLastUpdated     = findViewById(R.id.tvLastUpdated)
        ivRefresh         = findViewById(R.id.ivRefresh)

        setupChart()
        setupChipGroup()

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        ivRefresh.setOnClickListener { viewModel.fetchCurrent() }

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        setupNavigationDrawer()
        observeViewModel()
    }

    // ── Chart Setup ────────────────────────────────────────────────────────
    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled      = false
            setTouchEnabled(true)
            isDragEnabled         = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)

            xAxis.apply {
                textColor       = Color.parseColor("#B3FFFFFF")
                textSize        = 10f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity     = 1f
            }
            axisLeft.apply {
                textColor       = Color.parseColor("#B3FFFFFF")
                textSize        = 10f
                setDrawGridLines(true)
                gridColor       = Color.parseColor("#1AFFFFFF")
                setDrawAxisLine(false)
                valueFormatter  = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float) = formatCompact(value.toDouble())
                }
            }
            axisRight.isEnabled = false

            marker = NetWorthMarkerView(this@NetWorthActivity)
        }
    }

    // ── Chip Group ────────────────────────────────────────────────────────
    private fun setupChipGroup() {
        chipGroupPeriod.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val period = when (checkedIds.first()) {
                R.id.chip1M  -> "1m"
                R.id.chip3M  -> "3m"
                R.id.chip6M  -> "6m"
                R.id.chip1Y  -> "1y"
                R.id.chipAll -> "all"
                else         -> "1m"
            }
            viewModel.fetchSnapshots(period)
        }
    }

    // ── Observe ────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentState.collect   { handleCurrentState(it)   } }
                launch { viewModel.snapshotsState.collect { handleSnapshotsState(it) } }
            }
        }
    }

    private fun handleCurrentState(state: NetWorthCurrentState) {
        when (state) {
            is NetWorthCurrentState.Loading -> {
                tvNetWorthValue.text   = getString(R.string.loading)
                tvTotalAssets.text     = "--"
                tvTotalLiabilities.text = "--"
                swipeRefresh.isRefreshing = true
            }
            is NetWorthCurrentState.Success -> {
                swipeRefresh.isRefreshing = false
                val nw = state.data.netWorth
                tvNetWorthValue.text = formatCompact(nw)
                tvNetWorthValue.setTextColor(
                    if (nw >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                )
                tvTotalAssets.text     = formatCompact(state.data.totalAssets)
                tvTotalLiabilities.text = formatCompact(state.data.totalLiabilities)
                tvLastUpdated.text = "Last updated: ${SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date())}"
            }
            is NetWorthCurrentState.Error -> {
                swipeRefresh.isRefreshing = false
                tvNetWorthValue.text = "Error — tap refresh"
                tvNetWorthValue.setTextColor(Color.parseColor("#F44336"))
            }
            is NetWorthCurrentState.Idle -> { /* no-op */ }
        }
    }

    private fun handleSnapshotsState(state: NetWorthSnapshotsState) {
        when (state) {
            is NetWorthSnapshotsState.Loading -> {
                chartProgressBar.visibility = View.VISIBLE
                lineChart.visibility        = View.GONE
                tvNoData.visibility         = View.GONE
            }
            is NetWorthSnapshotsState.Success -> {
                chartProgressBar.visibility = View.GONE
                if (state.snapshots.size < 2) {
                    lineChart.visibility = View.GONE
                    tvNoData.visibility  = View.VISIBLE
                } else {
                    tvNoData.visibility  = View.GONE
                    lineChart.visibility = View.VISIBLE
                    updateChart(state.snapshots)
                }
            }
            is NetWorthSnapshotsState.Error -> {
                chartProgressBar.visibility = View.GONE
                lineChart.visibility        = View.GONE
                tvNoData.visibility         = View.VISIBLE
                tvNoData.text               = "Could not load chart data"
            }
            is NetWorthSnapshotsState.Idle -> { /* no-op */ }
        }
    }

    // ── Chart Update ──────────────────────────────────────────────────────
    private fun updateChart(snapshots: List<NetWorthSnapshotDto>) {
        snapshotDates = snapshots.map { it.snapshotDate }

        val entries = snapshots.mapIndexed { index, snapshot ->
            Entry(index.toFloat(), snapshot.netWorth.toFloat())
        }

        val latestNw = snapshots.last().netWorth
        val lineColor = if (latestNw >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")

        val dataSet = LineDataSet(entries, "Net Worth").apply {
            color       = lineColor
            lineWidth   = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode        = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor   = lineColor
            fillAlpha   = 30
            highLightColor = Color.WHITE
        }

        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
        val labels = snapshots.map { snapshot ->
            try {
                LocalDate.parse(snapshot.snapshotDate.take(10))
                    .format(dateFormatter)
            } catch (e: Exception) {
                snapshot.snapshotDate.take(6)
            }
        }

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.labelCount = minOf(labels.size, 6)
        lineChart.data = LineData(dataSet)
        lineChart.invalidate()
    }

    // ── MarkerView ────────────────────────────────────────────────────────
    inner class NetWorthMarkerView(context: Context) : MarkerView(context, R.layout.marker_net_worth) {
        private val tvDate: TextView  = findViewById(R.id.tvMarkerDate)
        private val tvValue: TextView = findViewById(R.id.tvMarkerValue)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            if (e != null) {
                val index = e.x.toInt()
                tvDate.text  = if (index in snapshotDates.indices) snapshotDates[index].take(10) else ""
                tvValue.text = formatCompact(e.y.toDouble())
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2f), -height.toFloat() - 8f)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private fun formatCompact(amount: Double): String = when {
        amount >= 1_00_00_000 -> "₹${String.format("%.2f", amount / 1_00_00_000)}Cr"
        amount >= 1_00_000    -> "₹${String.format("%.2f", amount / 1_00_000)}L"
        else                  -> currencyFormat.format(amount)
    }

    // ── Navigation Drawer ─────────────────────────────────────────────────
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

        setupExpandable(headerView, R.id.btnManagement, R.id.ivManagementExpand, R.id.managementChildItems)

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

        headerView.findViewById<Button>(R.id.btnNetWorth)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            // Already on this screen
        }

        setupExpandable(headerView, R.id.btnAssets, R.id.ivAssetsExpand, R.id.assetsChildItems)

        headerView.findViewById<Button>(R.id.btnStocks)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, StocksActivity::class.java))
            finish()
        }
        headerView.findViewById<Button>(R.id.btnMetals)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, MetalsActivity::class.java))
            finish()
        }
        headerView.findViewById<Button>(R.id.btnMutualFunds)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, MutualFundsActivity::class.java))
            finish()
        }
        headerView.findViewById<Button>(R.id.btnOtherAssets)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, OtherAssetsActivity::class.java))
            finish()
        }
        headerView.findViewById<Button>(R.id.btnLiabilities)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, LiabilitiesActivity::class.java))
            finish()
        }

        setupExpandable(headerView, R.id.btnSetupDemat, R.id.ivSetupDematExpand, R.id.setupDematChildItems)

        headerView.findViewById<Button>(R.id.btnConnectZerodha)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SetupZerodhaActivity::class.java))
        }
        headerView.findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SettingsActivity::class.java))
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
        val btn       = headerView.findViewById<Button>(btnId)
        val icon      = headerView.findViewById<ImageView>(iconId)
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
