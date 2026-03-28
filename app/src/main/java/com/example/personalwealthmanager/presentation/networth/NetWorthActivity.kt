package com.example.personalwealthmanager.presentation.networth

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.data.remote.dto.NetWorthSnapshotDto
import android.content.Context
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
class NetWorthActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getActiveNavItem() = BottomNavItem.NETWORTH


    override fun getSelfButtonId() = R.id.btnNetWorth

    private val viewModel: NetWorthViewModel by viewModels()

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

    // Asset portfolio views
    private lateinit var stocksLoadingBar: ProgressBar
    private lateinit var stocksValueContainer: android.view.View
    private lateinit var tvStocksValue: TextView
    private lateinit var tvStocksPnl: TextView

    private lateinit var metalsLoadingBar: ProgressBar
    private lateinit var metalsValueContainer: android.view.View
    private lateinit var tvMetalsValue: TextView
    private lateinit var tvMetalsDayPnl: TextView
    private lateinit var metalsCagrSection: android.view.View
    private lateinit var tvMetalsCagr1y: TextView
    private lateinit var tvMetalsCagr3y: TextView
    private lateinit var tvMetalsCagr5y: TextView

    private lateinit var mfLoadingBar: ProgressBar
    private lateinit var mfValueContainer: android.view.View
    private lateinit var tvMfValue: TextView
    private lateinit var tvMfDayPnl: TextView
    private lateinit var mfCagrSection: android.view.View
    private lateinit var tvMfCagr1y: TextView
    private lateinit var tvMfCagr3y: TextView
    private lateinit var tvMfCagr5y: TextView

    private lateinit var otherLoadingBar: ProgressBar
    private lateinit var otherValueContainer: android.view.View
    private lateinit var tvOtherValue: TextView

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private var snapshotDates: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_net_worth)

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

        // Asset portfolio views
        stocksLoadingBar      = findViewById(R.id.stocksLoadingBar)
        stocksValueContainer  = findViewById(R.id.stocksValueContainer)
        tvStocksValue         = findViewById(R.id.tvStocksValue)
        tvStocksPnl           = findViewById(R.id.tvStocksPnl)

        metalsLoadingBar      = findViewById(R.id.metalsLoadingBar)
        metalsValueContainer  = findViewById(R.id.metalsValueContainer)
        tvMetalsValue         = findViewById(R.id.tvMetalsValue)
        tvMetalsDayPnl        = findViewById(R.id.tvMetalsDayPnl)
        metalsCagrSection     = findViewById(R.id.metalsCagrSection)
        tvMetalsCagr1y        = findViewById(R.id.tvMetalsCagr1y)
        tvMetalsCagr3y        = findViewById(R.id.tvMetalsCagr3y)
        tvMetalsCagr5y        = findViewById(R.id.tvMetalsCagr5y)

        mfLoadingBar          = findViewById(R.id.mfLoadingBar)
        mfValueContainer      = findViewById(R.id.mfValueContainer)
        tvMfValue             = findViewById(R.id.tvMfValue)
        tvMfDayPnl            = findViewById(R.id.tvMfDayPnl)
        mfCagrSection         = findViewById(R.id.mfCagrSection)
        tvMfCagr1y            = findViewById(R.id.tvMfCagr1y)
        tvMfCagr3y            = findViewById(R.id.tvMfCagr3y)
        tvMfCagr5y            = findViewById(R.id.tvMfCagr5y)

        otherLoadingBar       = findViewById(R.id.otherLoadingBar)
        otherValueContainer   = findViewById(R.id.otherValueContainer)
        tvOtherValue          = findViewById(R.id.tvOtherValue)

        setupChart()
        setupChipGroup()

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        ivRefresh.setOnClickListener { viewModel.refresh() }

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        setupDrawerMenu()
        setupBottomNav()
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
                launch { viewModel.currentState.collect    { handleCurrentState(it)    } }
                launch { viewModel.snapshotsState.collect  { handleSnapshotsState(it)  } }
                launch { viewModel.stocksState.collect     { handleStocksState(it)     } }
                launch { viewModel.metalsState.collect     { handleMetalsState(it)     } }
                launch { viewModel.mfState.collect         { handleMfState(it)         } }
                launch { viewModel.otherAssetsState.collect{ handleOtherAssetsState(it)} }
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

    private fun handleStocksState(state: StocksWidgetState) {
        when (state) {
            is StocksWidgetState.Loading -> {
                stocksLoadingBar.visibility     = View.VISIBLE
                stocksValueContainer.visibility = View.GONE
            }
            is StocksWidgetState.Success -> {
                stocksLoadingBar.visibility     = View.GONE
                stocksValueContainer.visibility = View.VISIBLE
                tvStocksValue.text = if (state.totalValue == 0.0) "No holdings" else formatCompact(state.totalValue)
                val pnl = state.todayPnl
                tvStocksPnl.text = if (pnl >= 0) "+${formatCompact(pnl)}" else formatCompact(pnl)
                tvStocksPnl.setTextColor(if (pnl >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            }
            is StocksWidgetState.Error -> {
                stocksLoadingBar.visibility     = View.GONE
                stocksValueContainer.visibility = View.VISIBLE
                tvStocksValue.text = "--"
                tvStocksPnl.text   = ""
            }
            is StocksWidgetState.Idle -> { /* no-op */ }
        }
    }

    private fun handleMetalsState(state: MetalsWidgetState) {
        when (state) {
            is MetalsWidgetState.Loading -> {
                metalsLoadingBar.visibility     = View.VISIBLE
                metalsValueContainer.visibility = View.GONE
                metalsCagrSection.visibility    = View.GONE
            }
            is MetalsWidgetState.Success -> {
                metalsLoadingBar.visibility     = View.GONE
                metalsValueContainer.visibility = View.VISIBLE
                tvMetalsValue.text = formatCompact(state.data.totalValue)
                val pnl = state.data.totalDayPnl
                tvMetalsDayPnl.text = if (pnl >= 0) "+${formatCompact(pnl)}" else formatCompact(pnl)
                tvMetalsDayPnl.setTextColor(if (pnl >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
                if (state.data.hasCagr) {
                    metalsCagrSection.visibility = View.VISIBLE
                    tvMetalsCagr1y.text = "1Y: ${formatCompact(state.data.projected1y)}"
                    tvMetalsCagr3y.text = "3Y: ${formatCompact(state.data.projected3y)}"
                    tvMetalsCagr5y.text = "5Y: ${formatCompact(state.data.projected5y)}"
                }
            }
            is MetalsWidgetState.Error -> {
                metalsLoadingBar.visibility     = View.GONE
                metalsValueContainer.visibility = View.VISIBLE
                tvMetalsValue.text  = "--"
                tvMetalsDayPnl.text = ""
            }
            is MetalsWidgetState.Idle -> { /* no-op */ }
        }
    }

    private fun handleMfState(state: MfWidgetState) {
        when (state) {
            is MfWidgetState.Loading -> {
                mfLoadingBar.visibility     = View.VISIBLE
                mfValueContainer.visibility = View.GONE
                mfCagrSection.visibility    = View.GONE
            }
            is MfWidgetState.Success -> {
                mfLoadingBar.visibility     = View.GONE
                mfValueContainer.visibility = View.VISIBLE
                tvMfValue.text = formatCompact(state.data.currentValue)
                val pnl = state.data.totalDayPnl
                tvMfDayPnl.text = if (pnl >= 0) "+${formatCompact(pnl)}" else formatCompact(pnl)
                tvMfDayPnl.setTextColor(if (pnl >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
                if (state.data.hasCagr) {
                    mfCagrSection.visibility = View.VISIBLE
                    tvMfCagr1y.text = "1Y: ${formatCompact(state.data.projected1y)}"
                    tvMfCagr3y.text = "3Y: ${formatCompact(state.data.projected3y)}"
                    tvMfCagr5y.text = "5Y: ${formatCompact(state.data.projected5y)}"
                }
            }
            is MfWidgetState.Error -> {
                mfLoadingBar.visibility     = View.GONE
                mfValueContainer.visibility = View.VISIBLE
                tvMfValue.text  = "--"
                tvMfDayPnl.text = ""
            }
            is MfWidgetState.Idle -> { /* no-op */ }
        }
    }

    private fun handleOtherAssetsState(state: OtherAssetsWidgetState) {
        when (state) {
            is OtherAssetsWidgetState.Loading -> {
                otherLoadingBar.visibility     = View.VISIBLE
                otherValueContainer.visibility = View.GONE
            }
            is OtherAssetsWidgetState.Success -> {
                otherLoadingBar.visibility     = View.GONE
                otherValueContainer.visibility = View.VISIBLE
                tvOtherValue.text = formatCompact(state.totalValue)
            }
            is OtherAssetsWidgetState.Error -> {
                otherLoadingBar.visibility     = View.GONE
                otherValueContainer.visibility = View.VISIBLE
                tvOtherValue.text = "--"
            }
            is OtherAssetsWidgetState.Idle -> { /* no-op */ }
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
}
