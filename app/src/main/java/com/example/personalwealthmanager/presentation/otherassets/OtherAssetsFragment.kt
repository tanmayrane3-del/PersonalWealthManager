package com.example.personalwealthmanager.presentation.otherassets

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.core.utils.PhysicalAssetCagrCalculator
import com.example.personalwealthmanager.domain.model.PhysicalAsset
import com.example.personalwealthmanager.domain.model.PhysicalAssetSummary
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class OtherAssetsFragment : Fragment() {

    private val viewModel: OtherAssetsViewModel by activityViewModels()
    private lateinit var adapter: OtherAssetCardAdapter

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var rvAssets: RecyclerView
    private lateinit var summaryCard: View
    private lateinit var tvTotalValue: TextView
    private lateinit var cagrSection: LinearLayout
    private lateinit var tvCagr1y: TextView
    private lateinit var tvCagr3y: TextView
    private lateinit var tvCagr5y: TextView
    private lateinit var tvProjected1y: TextView
    private lateinit var tvProjected3y: TextView
    private lateinit var tvProjected5y: TextView
    private lateinit var fabAddAsset: FloatingActionButton

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_other_assets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        emptyState = view.findViewById(R.id.emptyState)
        rvAssets = view.findViewById(R.id.rvAssets)
        summaryCard = view.findViewById(R.id.summaryCard)
        tvTotalValue = view.findViewById(R.id.tvTotalValue)
        cagrSection = view.findViewById(R.id.cagrSection)
        tvCagr1y = view.findViewById(R.id.tvCagr1y)
        tvCagr3y = view.findViewById(R.id.tvCagr3y)
        tvCagr5y = view.findViewById(R.id.tvCagr5y)
        tvProjected1y = view.findViewById(R.id.tvProjected1y)
        tvProjected3y = view.findViewById(R.id.tvProjected3y)
        tvProjected5y = view.findViewById(R.id.tvProjected5y)
        fabAddAsset = view.findViewById(R.id.fabAddAsset)

        adapter = OtherAssetCardAdapter(onLongPress = { asset -> showLongPressMenu(asset) })
        rvAssets.layoutManager = LinearLayoutManager(requireContext())
        rvAssets.adapter = adapter
        rvAssets.isNestedScrollingEnabled = false

        fabAddAsset.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditOtherAssetActivity::class.java))
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.fetchSummary()
        }

        observeUiState()
        observeActionState()
        viewModel.fetchSummary()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchSummary()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    swipeRefresh.isRefreshing = false
                    when (state) {
                        is OtherAssetsUiState.Idle -> Unit
                        is OtherAssetsUiState.Loading -> showLoading()
                        is OtherAssetsUiState.Success -> showSuccess(state.summary)
                        is OtherAssetsUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun observeActionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionState.collect { state ->
                    when (state) {
                        is OtherAssetsActionState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetActionState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        rvAssets.visibility = View.GONE
        summaryCard.visibility = View.GONE
    }

    private fun showSuccess(summary: PhysicalAssetSummary) {
        progressBar.visibility = View.GONE
        summaryCard.visibility = View.VISIBLE

        // Total value
        tvTotalValue.text = currencyFormat.format(summary.totalCurrentValue)

        // CAGR projections
        val assets = summary.assets
        if (assets.isNotEmpty()) {
            val totalCurrent = summary.totalCurrentValue.takeIf { it > 0 }
                ?: assets.sumOf { PhysicalAssetCagrCalculator.getAssetCurrentValue(it) }

            val proj1y = assets.sumOf { PhysicalAssetCagrCalculator.getProjectedValue(it, 1) }
            val proj3y = assets.sumOf { PhysicalAssetCagrCalculator.getProjectedValue(it, 3) }
            val proj5y = assets.sumOf { PhysicalAssetCagrCalculator.getProjectedValue(it, 5) }

            if (totalCurrent > 0) {
                val cagr1y = (proj1y / totalCurrent - 1) * 100
                val cagr3y = (Math.pow(proj3y / totalCurrent, 1.0 / 3) - 1) * 100
                val cagr5y = (Math.pow(proj5y / totalCurrent, 1.0 / 5) - 1) * 100

                fun cagrColor(pct: Double) = ContextCompat.getColor(
                    requireContext(),
                    if (pct >= 0) R.color.income_green else R.color.expense_red
                )

                tvCagr1y.text = "${if (cagr1y >= 0) "+" else ""}${"%.1f".format(cagr1y)}%"
                tvCagr3y.text = "${if (cagr3y >= 0) "+" else ""}${"%.1f".format(cagr3y)}%"
                tvCagr5y.text = "${if (cagr5y >= 0) "+" else ""}${"%.1f".format(cagr5y)}%"
                tvCagr1y.setTextColor(cagrColor(cagr1y))
                tvCagr3y.setTextColor(cagrColor(cagr3y))
                tvCagr5y.setTextColor(cagrColor(cagr5y))
                tvProjected1y.text = formatCompact(proj1y)
                tvProjected3y.text = formatCompact(proj3y)
                tvProjected5y.text = formatCompact(proj5y)
                cagrSection.visibility = View.VISIBLE
            }
        } else {
            cagrSection.visibility = View.GONE
        }

        if (summary.assets.isEmpty()) {
            rvAssets.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            emptyState.visibility = View.GONE
            rvAssets.visibility = View.VISIBLE
            adapter.submitList(summary.assets)
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        rvAssets.visibility = View.GONE
        summaryCard.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showLongPressMenu(asset: PhysicalAsset) {
        val editLabel = "Edit"
        val deleteLabel = if (asset.hasActiveLoan) "Delete (linked to active loan)" else "Delete"
        val items = arrayOf(editLabel, deleteLabel)

        AlertDialog.Builder(requireContext())
            .setTitle(asset.label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openEdit(asset)
                    1 -> {
                        if (asset.hasActiveLoan) {
                            Toast.makeText(
                                requireContext(),
                                "Cannot delete — active loan linked to this asset",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            confirmDelete(asset)
                        }
                    }
                }
            }
            .show()
    }

    private fun openEdit(asset: PhysicalAsset) {
        val intent = Intent(requireContext(), AddEditOtherAssetActivity::class.java)
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_ASSET_ID, asset.id)
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_ASSET_TYPE, asset.assetType)
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_LABEL, asset.label)
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_PURCHASE_PRICE, asset.purchasePrice)
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_PURCHASE_DATE, asset.purchaseDate)
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_CURRENT_MARKET_VALUE, asset.currentMarketValue ?: 0.0)
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_DEPRECIATION_RATE, asset.depreciationRatePct ?: 10.0)
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_NOTES, asset.notes ?: "")
        intent.putExtra(AddEditOtherAssetActivity.EXTRA_HAS_ACTIVE_LOAN, asset.hasActiveLoan)
        startActivity(intent)
    }

    private fun confirmDelete(asset: PhysicalAsset) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Asset")
            .setMessage("Delete \"${asset.label}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteAsset(asset.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatCompact(amount: Double): String = when {
        amount >= 1_00_00_000 -> "₹${"%.2f".format(amount / 1_00_00_000)}Cr"
        amount >= 1_00_000 -> "₹${"%.2f".format(amount / 1_00_000)}L"
        else -> currencyFormat.format(amount)
    }
}
