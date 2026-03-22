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

        setupChart()
        setupChipGroup()

        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        ivRefresh.setOnClickListener { viewModel.fetchCurrent() }

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        setupDrawerMenu()
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
}
