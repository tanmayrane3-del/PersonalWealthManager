package com.example.personalwealthmanager.presentation.networth

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.data.remote.dto.NetWorthCurrentDto
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

    // Latest net worth DTO — cached so asset handlers can read counts
    private var latestNetWorthDto: NetWorthCurrentDto? = null

    // ── Hero views ────────────────────────────────────────────────────────────
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvNetWorthValue: TextView
    private lateinit var tvTotalAssets: TextView
    private lateinit var tvTotalLiabilities: TextView
    private lateinit var tvDayChangeBadge: TextView
    private lateinit var tvCagrBadge: TextView

    // ── Projection row ────────────────────────────────────────────────────────
    private lateinit var tvProj1y: TextView
    private lateinit var tvProj1yPct: TextView
    private lateinit var tvProj3y: TextView
    private lateinit var tvProj3yPct: TextView
    private lateinit var tvProj5y: TextView
    private lateinit var tvProj5yPct: TextView

    // ── Chart views ───────────────────────────────────────────────────────────
    private lateinit var chipGroupPeriod: ChipGroup
    private lateinit var chartProgressBar: ProgressBar
    private lateinit var lineChart: LineChart
    private lateinit var tvNoData: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var ivRefresh: ImageButton

    // ── Stocks card ───────────────────────────────────────────────────────────
    private lateinit var stocksLoadingBar: ProgressBar
    private lateinit var stocksValueContainer: View
    private lateinit var tvStocksValue: TextView
    private lateinit var tvStocksPnl: TextView
    private lateinit var tvStocksChangeBadge: TextView
    private lateinit var stocksCagrSection: View
    private lateinit var dividerStocksCagr: View
    private lateinit var tvStocksCagr1y: TextView
    private lateinit var tvStocksCagr3y: TextView
    private lateinit var tvStocksCagr5y: TextView

    // ── Gold card ─────────────────────────────────────────────────────────────
    private lateinit var metalsLoadingBar: ProgressBar
    private lateinit var metalsValueContainer: View
    private lateinit var tvMetalsValue: TextView
    private lateinit var tvMetalsDayPnl: TextView
    private lateinit var tvMetalsChangeBadge: TextView
    private lateinit var metalsCagrSection: View
    private lateinit var dividerMetalsCagr: View
    private lateinit var tvMetalsCagr1y: TextView
    private lateinit var tvMetalsCagr3y: TextView
    private lateinit var tvMetalsCagr5y: TextView

    // ── MF card ───────────────────────────────────────────────────────────────
    private lateinit var mfLoadingBar: ProgressBar
    private lateinit var mfValueContainer: View
    private lateinit var tvMfValue: TextView
    private lateinit var tvMfDayPnl: TextView
    private lateinit var tvMfChangeBadge: TextView
    private lateinit var mfCagrSection: View
    private lateinit var dividerMfCagr: View
    private lateinit var tvMfCagr1y: TextView
    private lateinit var tvMfCagr3y: TextView
    private lateinit var tvMfCagr5y: TextView

    // ── Other assets card ─────────────────────────────────────────────────────
    private lateinit var otherLoadingBar: ProgressBar
    private lateinit var otherValueContainer: View
    private lateinit var tvOtherValue: TextView
    private lateinit var tvOtherCount: TextView
    private lateinit var otherCagrSection: View
    private lateinit var dividerOtherCagr: View
    private lateinit var tvOtherCagr1y: TextView
    private lateinit var tvOtherCagr3y: TextView
    private lateinit var tvOtherCagr5y: TextView

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private var snapshotDates: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_net_worth)

        // Hero
        swipeRefresh        = findViewById(R.id.swipeRefresh)
        tvNetWorthValue     = findViewById(R.id.tvNetWorthValue)
        tvTotalAssets       = findViewById(R.id.tvTotalAssets)
        tvTotalLiabilities  = findViewById(R.id.tvTotalLiabilities)
        tvDayChangeBadge    = findViewById(R.id.tvDayChangeBadge)
        tvCagrBadge         = findViewById(R.id.tvCagrBadge)

        // Projection row
        tvProj1y    = findViewById(R.id.tvProj1y)
        tvProj1yPct = findViewById(R.id.tvProj1yPct)
        tvProj3y    = findViewById(R.id.tvProj3y)
        tvProj3yPct = findViewById(R.id.tvProj3yPct)
        tvProj5y    = findViewById(R.id.tvProj5y)
        tvProj5yPct = findViewById(R.id.tvProj5yPct)

        // Chart
        chipGroupPeriod  = findViewById(R.id.chipGroupPeriod)
        chartProgressBar = findViewById(R.id.chartProgressBar)
        lineChart        = findViewById(R.id.lineChart)
        tvNoData         = findViewById(R.id.tvNoData)
        tvLastUpdated    = findViewById(R.id.tvLastUpdated)
        ivRefresh        = findViewById(R.id.ivRefresh)

        // Stocks card
        stocksLoadingBar    = findViewById(R.id.stocksLoadingBar)
        stocksValueContainer = findViewById(R.id.stocksValueContainer)
        tvStocksValue       = findViewById(R.id.tvStocksValue)
        tvStocksPnl         = findViewById(R.id.tvStocksPnl)
        tvStocksChangeBadge = findViewById(R.id.tvStocksChangeBadge)
        stocksCagrSection   = findViewById(R.id.stocksCagrSection)
        dividerStocksCagr   = findViewById(R.id.dividerStocksCagr)
        tvStocksCagr1y      = findViewById(R.id.tvStocksCagr1y)
        tvStocksCagr3y      = findViewById(R.id.tvStocksCagr3y)
        tvStocksCagr5y      = findViewById(R.id.tvStocksCagr5y)

        // Gold card
        metalsLoadingBar    = findViewById(R.id.metalsLoadingBar)
        metalsValueContainer = findViewById(R.id.metalsValueContainer)
        tvMetalsValue       = findViewById(R.id.tvMetalsValue)
        tvMetalsDayPnl      = findViewById(R.id.tvMetalsDayPnl)
        tvMetalsChangeBadge = findViewById(R.id.tvMetalsChangeBadge)
        metalsCagrSection   = findViewById(R.id.metalsCagrSection)
        dividerMetalsCagr   = findViewById(R.id.dividerMetalsCagr)
        tvMetalsCagr1y      = findViewById(R.id.tvMetalsCagr1y)
        tvMetalsCagr3y      = findViewById(R.id.tvMetalsCagr3y)
        tvMetalsCagr5y      = findViewById(R.id.tvMetalsCagr5y)

        // MF card
        mfLoadingBar    = findViewById(R.id.mfLoadingBar)
        mfValueContainer = findViewById(R.id.mfValueContainer)
        tvMfValue       = findViewById(R.id.tvMfValue)
        tvMfDayPnl      = findViewById(R.id.tvMfDayPnl)
        tvMfChangeBadge = findViewById(R.id.tvMfChangeBadge)
        mfCagrSection   = findViewById(R.id.mfCagrSection)
        dividerMfCagr   = findViewById(R.id.dividerMfCagr)
        tvMfCagr1y      = findViewById(R.id.tvMfCagr1y)
        tvMfCagr3y      = findViewById(R.id.tvMfCagr3y)
        tvMfCagr5y      = findViewById(R.id.tvMfCagr5y)

        // Other assets card
        otherLoadingBar    = findViewById(R.id.otherLoadingBar)
        otherValueContainer = findViewById(R.id.otherValueContainer)
        tvOtherValue       = findViewById(R.id.tvOtherValue)
        tvOtherCount       = findViewById(R.id.tvOtherCount)
        otherCagrSection   = findViewById(R.id.otherCagrSection)
        dividerOtherCagr   = findViewById(R.id.dividerOtherCagr)
        tvOtherCagr1y      = findViewById(R.id.tvOtherCagr1y)
        tvOtherCagr3y      = findViewById(R.id.tvOtherCagr3y)
        tvOtherCagr5y      = findViewById(R.id.tvOtherCagr5y)

        setupChart()
        setupChipGroup()

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        ivRefresh.setOnClickListener { viewModel.refresh() }

        // Back button — mirrors Stocks page behaviour
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { onBackPressed() }

        setupDrawerMenu()
        setupBottomNav()
        observeViewModel()
    }

    // ── Chart Setup ────────────────────────────────────────────────────────────
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
                textColor       = Color.parseColor("#757575")
                textSize        = 10f
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity     = 1f
            }
            axisLeft.apply {
                textColor       = Color.parseColor("#757575")
                textSize        = 10f
                setDrawGridLines(true)
                gridColor       = Color.parseColor("#1A000000")
                setDrawAxisLine(false)
                valueFormatter  = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float) = formatCompact(value.toDouble())
                }
            }
            axisRight.isEnabled = false

            marker = NetWorthMarkerView(this@NetWorthActivity)
        }
    }

    // ── Chip Group ──────────────────────────────────────────────────────────────
    private fun setupChipGroup() {
        chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
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

    // ── Observe ────────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentState.collect    { handleCurrentState(it)     } }
                launch { viewModel.snapshotsState.collect  { handleSnapshotsState(it)   } }
                launch { viewModel.stocksState.collect     { handleStocksState(it)      } }
                launch { viewModel.metalsState.collect     { handleMetalsState(it)      } }
                launch { viewModel.mfState.collect         { handleMfState(it)          } }
                launch { viewModel.otherAssetsState.collect{ handleOtherAssetsState(it) } }
            }
        }
    }

    // ── Current net worth (hero + projections) ──────────────────────────────────
    private fun handleCurrentState(state: NetWorthCurrentState) {
        when (state) {
            is NetWorthCurrentState.Loading -> {
                tvNetWorthValue.text = getString(R.string.loading)
                tvTotalAssets.text   = "--"
                tvTotalLiabilities.text = "--"
                swipeRefresh.isRefreshing = true
            }
            is NetWorthCurrentState.Success -> {
                swipeRefresh.isRefreshing = false
                latestNetWorthDto = state.data
                val dto = state.data
                val nw  = dto.netWorth

                tvNetWorthValue.text = formatCompact(nw)
                tvTotalAssets.text   = formatCompact(dto.totalAssets)
                tvTotalLiabilities.text = formatCompact(dto.totalLiabilities)
                tvLastUpdated.text   = "Last updated: ${SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date())}"

                // Day change badge
                val dc = dto.dayChange
                if (dc != 0.0) {
                    val sign = if (dc >= 0) "+" else ""
                    tvDayChangeBadge.text = "$sign${formatCompact(dc)} (${String.format("%.1f", dto.dayChangePct)}%)"
                    tvDayChangeBadge.visibility = View.VISIBLE
                }

                // CAGR badge
                if (dto.cagr1y != 0.0) {
                    tvCagrBadge.text = "${String.format("%.1f", dto.cagr1y)}% CAGR"
                    tvCagrBadge.visibility = View.VISIBLE
                }

                // Projection row
                if (dto.projected1y > 0 && nw > 0) {
                    tvProj1y.text    = formatCompact(dto.projected1y)
                    tvProj1yPct.text = projPct(dto.projected1y, nw)
                    tvProj3y.text    = formatCompact(dto.projected3y)
                    tvProj3yPct.text = projPct(dto.projected3y, nw)
                    tvProj5y.text    = formatCompact(dto.projected5y)
                    tvProj5yPct.text = projPct(dto.projected5y, nw)
                }

                // Stocks CAGR (projections live in the current DTO)
                if (dto.stocksProj1y > 0) {
                    tvStocksCagr1y.text = formatCompact(dto.stocksProj1y)
                    tvStocksCagr3y.text = formatCompact(dto.stocksProj3y)
                    tvStocksCagr5y.text = formatCompact(dto.stocksProj5y)
                    stocksCagrSection.visibility = View.VISIBLE
                    dividerStocksCagr.visibility = View.VISIBLE
                }

                // Refresh position count labels if asset states already loaded
                refreshStocksFooter()
                refreshMfFooter()
                refreshOtherFooter()
            }
            is NetWorthCurrentState.Error -> {
                swipeRefresh.isRefreshing = false
                tvNetWorthValue.text = "Error — tap refresh"
            }
            is NetWorthCurrentState.Idle -> { /* no-op */ }
        }
    }

    // ── Stocks ─────────────────────────────────────────────────────────────────
    private fun handleStocksState(state: StocksWidgetState) {
        when (state) {
            is StocksWidgetState.Loading -> {
                stocksLoadingBar.visibility     = View.VISIBLE
                stocksValueContainer.visibility = View.GONE
                tvStocksChangeBadge.visibility  = View.GONE
            }
            is StocksWidgetState.Success -> {
                stocksLoadingBar.visibility = View.GONE
                tvStocksValue.text = if (state.totalValue == 0.0) "No holdings"
                                     else formatCompact(state.totalValue)

                // Day change % badge
                val pnl = state.todayPnl
                val prev = state.totalValue - pnl
                if (state.totalValue > 0 && prev != 0.0) {
                    val pct = (pnl / Math.abs(prev)) * 100
                    val sign = if (pct >= 0) "+" else ""
                    tvStocksChangeBadge.text = "$sign${String.format("%.1f", pct)}%"
                    tvStocksChangeBadge.setTextColor(
                        if (pct >= 0) Color.parseColor("#1B5E20") else Color.parseColor("#B71C1C")
                    )
                    tvStocksChangeBadge.visibility = View.VISIBLE
                }

                stocksValueContainer.visibility = View.VISIBLE
                refreshStocksFooter()
            }
            is StocksWidgetState.Error -> {
                stocksLoadingBar.visibility    = View.GONE
                tvStocksValue.text             = "--"
                stocksValueContainer.visibility = View.VISIBLE
                tvStocksPnl.text               = ""
            }
            is StocksWidgetState.Idle -> { /* no-op */ }
        }
    }

    // ── Metals ─────────────────────────────────────────────────────────────────
    private fun handleMetalsState(state: MetalsWidgetState) {
        when (state) {
            is MetalsWidgetState.Loading -> {
                metalsLoadingBar.visibility     = View.VISIBLE
                metalsValueContainer.visibility = View.GONE
                metalsCagrSection.visibility    = View.GONE
                dividerMetalsCagr.visibility    = View.GONE
                tvMetalsChangeBadge.visibility  = View.GONE
            }
            is MetalsWidgetState.Success -> {
                metalsLoadingBar.visibility = View.GONE
                tvMetalsValue.text = formatCompact(state.data.totalValue)

                // Day change % badge
                val pnl = state.data.totalDayPnl
                val prev = state.data.totalValue - pnl
                if (state.data.totalValue > 0 && prev != 0.0) {
                    val pct = (pnl / Math.abs(prev)) * 100
                    val sign = if (pct >= 0) "+" else ""
                    tvMetalsChangeBadge.text = "$sign${String.format("%.1f", pct)}%"
                    tvMetalsChangeBadge.setTextColor(
                        if (pct >= 0) Color.parseColor("#1B5E20") else Color.parseColor("#B71C1C")
                    )
                    tvMetalsChangeBadge.visibility = View.VISIBLE
                }

                if (state.data.hasCagr) {
                    tvMetalsCagr1y.text = formatCompact(state.data.projected1y)
                    tvMetalsCagr3y.text = formatCompact(state.data.projected3y)
                    tvMetalsCagr5y.text = formatCompact(state.data.projected5y)
                    metalsCagrSection.visibility = View.VISIBLE
                    dividerMetalsCagr.visibility = View.VISIBLE
                }

                tvMetalsDayPnl.text             = "Digital & Physical"
                metalsValueContainer.visibility = View.VISIBLE
            }
            is MetalsWidgetState.Error -> {
                metalsLoadingBar.visibility     = View.GONE
                tvMetalsValue.text              = "--"
                metalsValueContainer.visibility = View.VISIBLE
                tvMetalsDayPnl.text             = "Digital & Physical"
            }
            is MetalsWidgetState.Idle -> { /* no-op */ }
        }
    }

    // ── Mutual Funds ───────────────────────────────────────────────────────────
    private fun handleMfState(state: MfWidgetState) {
        when (state) {
            is MfWidgetState.Loading -> {
                mfLoadingBar.visibility     = View.VISIBLE
                mfValueContainer.visibility = View.GONE
                mfCagrSection.visibility    = View.GONE
                dividerMfCagr.visibility    = View.GONE
                tvMfChangeBadge.visibility  = View.GONE
            }
            is MfWidgetState.Success -> {
                mfLoadingBar.visibility = View.GONE
                tvMfValue.text = formatCompact(state.data.currentValue)

                // Day change % badge
                val pnl = state.data.totalDayPnl
                val prev = state.data.currentValue - pnl
                if (state.data.currentValue > 0 && prev != 0.0) {
                    val pct = (pnl / Math.abs(prev)) * 100
                    val sign = if (pct >= 0) "+" else ""
                    tvMfChangeBadge.text = "$sign${String.format("%.1f", pct)}%"
                    tvMfChangeBadge.setTextColor(
                        if (pct >= 0) Color.parseColor("#1B5E20") else Color.parseColor("#B71C1C")
                    )
                    tvMfChangeBadge.visibility = View.VISIBLE
                }

                if (state.data.hasCagr) {
                    tvMfCagr1y.text = formatCompact(state.data.projected1y)
                    tvMfCagr3y.text = formatCompact(state.data.projected3y)
                    tvMfCagr5y.text = formatCompact(state.data.projected5y)
                    mfCagrSection.visibility = View.VISIBLE
                    dividerMfCagr.visibility = View.VISIBLE
                }

                mfValueContainer.visibility = View.VISIBLE
                refreshMfFooter()
            }
            is MfWidgetState.Error -> {
                mfLoadingBar.visibility     = View.GONE
                tvMfValue.text              = "--"
                mfValueContainer.visibility = View.VISIBLE
                tvMfDayPnl.text             = ""
            }
            is MfWidgetState.Idle -> { /* no-op */ }
        }
    }

    // ── Other Assets ───────────────────────────────────────────────────────────
    private fun handleOtherAssetsState(state: OtherAssetsWidgetState) {
        when (state) {
            is OtherAssetsWidgetState.Loading -> {
                otherLoadingBar.visibility     = View.VISIBLE
                otherValueContainer.visibility = View.GONE
            }
            is OtherAssetsWidgetState.Success -> {
                otherLoadingBar.visibility = View.GONE
                tvOtherValue.text = if (state.totalValue == 0.0) "No assets"
                                    else formatCompact(state.totalValue)
                if (state.proj1y > 0) {
                    tvOtherCagr1y.text = formatCompact(state.proj1y)
                    tvOtherCagr3y.text = formatCompact(state.proj3y)
                    tvOtherCagr5y.text = formatCompact(state.proj5y)
                    otherCagrSection.visibility  = View.VISIBLE
                    dividerOtherCagr.visibility  = View.VISIBLE
                }
                otherValueContainer.visibility = View.VISIBLE
                refreshOtherFooter()
            }
            is OtherAssetsWidgetState.Error -> {
                otherLoadingBar.visibility     = View.GONE
                tvOtherValue.text              = "--"
                otherValueContainer.visibility = View.VISIBLE
                tvOtherCount.text              = ""
            }
            is OtherAssetsWidgetState.Idle -> { /* no-op */ }
        }
    }

    // ── Footer helpers (use latestNetWorthDto once available) ──────────────────
    private fun refreshStocksFooter() {
        val count = latestNetWorthDto?.stocksCount ?: return
        tvStocksPnl.text = "$count Active Position${if (count != 1) "s" else ""}"
    }

    private fun refreshMfFooter() {
        val count = latestNetWorthDto?.mfCount ?: return
        tvMfDayPnl.text = "$count Active Fund${if (count != 1) "s" else ""}"
    }

    private fun refreshOtherFooter() {
        val count = latestNetWorthDto?.otherAssetsCount ?: return
        tvOtherCount.text = "$count Asset${if (count != 1) "s" else ""}"
    }

    // ── Snapshots / Chart ──────────────────────────────────────────────────────
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

    private fun updateChart(snapshots: List<NetWorthSnapshotDto>) {
        snapshotDates = snapshots.map { it.snapshotDate }

        val entries = snapshots.mapIndexed { index, snapshot ->
            Entry(index.toFloat(), snapshot.netWorth.toFloat())
        }

        val latestNw   = snapshots.last().netWorth
        val lineColor  = if (latestNw >= 0) Color.parseColor("#00695C") else Color.parseColor("#F44336")
        val fillColor  = if (latestNw >= 0) Color.parseColor("#00695C") else Color.parseColor("#F44336")

        val dataSet = LineDataSet(entries, "Net Worth").apply {
            color              = lineColor
            lineWidth          = 2f
            setDrawCircles(false)
            setDrawValues(false)
            mode               = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            this.fillColor     = fillColor
            fillAlpha          = 20
            highLightColor     = Color.parseColor("#00695C")
        }

        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
        val labels = snapshots.map { snapshot ->
            try {
                LocalDate.parse(snapshot.snapshotDate.take(10)).format(dateFormatter)
            } catch (e: Exception) {
                snapshot.snapshotDate.take(6)
            }
        }

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.labelCount     = minOf(labels.size, 6)
        lineChart.data                 = LineData(dataSet)
        lineChart.invalidate()
    }

    // ── MarkerView ─────────────────────────────────────────────────────────────
    inner class NetWorthMarkerView(context: Context) : MarkerView(context, R.layout.marker_net_worth) {
        private val tvDate:  TextView = findViewById(R.id.tvMarkerDate)
        private val tvValue: TextView = findViewById(R.id.tvMarkerValue)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            if (e != null) {
                val index = e.x.toInt()
                tvDate.text  = if (index in snapshotDates.indices) snapshotDates[index].take(10) else ""
                tvValue.text = formatCompact(e.y.toDouble())
            }
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat() - 8f)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private fun formatCompact(amount: Double): String = when {
        amount >= 1_00_00_000 -> "₹${String.format("%.2f", amount / 1_00_00_000)}Cr"
        amount >= 1_00_000    -> "₹${String.format("%.2f", amount / 1_00_000)}L"
        else                  -> currencyFormat.format(amount)
    }

    private fun projPct(projected: Double, base: Double): String {
        if (base <= 0) return ""
        val pct = ((projected - base) / base) * 100
        return if (pct >= 0) "+${String.format("%.0f", pct)}%" else "${String.format("%.0f", pct)}%"
    }
}
