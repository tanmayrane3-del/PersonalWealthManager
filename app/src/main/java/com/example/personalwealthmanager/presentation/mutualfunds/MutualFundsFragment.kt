package com.example.personalwealthmanager.presentation.mutualfunds

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.databinding.FragmentMutualFundsBinding
import com.example.personalwealthmanager.domain.model.MutualFundHolding
import com.example.personalwealthmanager.domain.model.MutualFundLot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.pow

@AndroidEntryPoint
class MutualFundsFragment : Fragment() {

    private var _binding: FragmentMutualFundsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MutualFundsViewModel by activityViewModels()
    private lateinit var adapter: MutualFundAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMutualFundsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MutualFundAdapter(
            onToggleExpand = { isin -> viewModel.toggleExpanded(isin) },
            onDeleteLot    = { lot  -> confirmDeleteLot(lot) },
            onDeleteFund   = { fund -> confirmDeleteFund(fund) }
        )
        binding.rvHoldings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHoldings.adapter = adapter
        binding.rvHoldings.isNestedScrollingEnabled = false

        binding.fabAdd.setOnClickListener { showAddOptions() }
        binding.ivSyncCagr.setOnClickListener { viewModel.syncCagrAndRefresh() }

        observeState()
        observeCagrState()
        observeExpansion()

        viewModel.fetchAll()
        viewModel.fetchSummary()
    }

    private fun showAddOptions() {
        val options = arrayOf("Import CAS PDF", "Add Manually")
        AlertDialog.Builder(requireContext())
            .setTitle("Add Mutual Fund")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(requireContext(), CasImportActivity::class.java))
                    1 -> AddMutualFundBottomSheet().show(childFragmentManager, "add_mf")
                }
            }
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MutualFundsUiState.Idle    -> Unit
                    is MutualFundsUiState.Loading -> showLoading()
                    is MutualFundsUiState.Success -> showSuccess(state)
                    is MutualFundsUiState.Error   -> showError(state.message)
                }
            }
        }
    }

    private fun observeCagrState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cagrState.collect { state ->
                when (state) {
                    is MutualFundsCagrState.Idle -> {
                        binding.cagrProjectionsSection.visibility = View.GONE
                        binding.tvCagrSyncing.visibility = View.GONE
                        binding.ivSyncCagr.isEnabled = true
                        binding.ivSyncCagr.alpha = 1.0f
                    }
                    is MutualFundsCagrState.Syncing -> {
                        binding.cagrProjectionsSection.visibility = View.GONE
                        binding.tvCagrSyncing.visibility = View.VISIBLE
                        binding.ivSyncCagr.isEnabled = false
                        binding.ivSyncCagr.alpha = 0.4f
                    }
                    is MutualFundsCagrState.Available -> {
                        binding.tvCagrSyncing.visibility = View.GONE
                        binding.ivSyncCagr.isEnabled = true
                        binding.ivSyncCagr.alpha = 1.0f
                        val total = state.totalValue
                        if (total > 0) {
                            val fmt = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
                            val cagr1y = (state.projected1y / total - 1) * 100
                            val cagr3y = (state.projected3y / total).pow(1.0 / 3) * 100 - 100
                            val cagr5y = (state.projected5y / total).pow(1.0 / 5) * 100 - 100
                            binding.tvCagrProjection1y.text = "1Y:  %.1f%%   %s".format(cagr1y, formatCompact(state.projected1y, fmt))
                            binding.tvCagrProjection3y.text = "3Y:  %.1f%%   %s".format(cagr3y, formatCompact(state.projected3y, fmt))
                            binding.tvCagrProjection5y.text = "5Y:  %.1f%%   %s".format(cagr5y, formatCompact(state.projected5y, fmt))
                        }
                        binding.cagrProjectionsSection.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun observeExpansion() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.expandedIsins.collect { expandedIsins ->
                val state = viewModel.uiState.value
                if (state is MutualFundsUiState.Success) {
                    adapter.submitList(state.funds, expandedIsins)
                }
            }
        }
    }

    private fun formatCompact(amount: Double, fmt: NumberFormat): String = when {
        amount >= 1_00_00_000 -> "₹${"%.2f".format(amount / 1_00_00_000)}Cr"
        amount >= 1_00_000    -> "₹${"%.2f".format(amount / 1_00_000)}L"
        else                  -> fmt.format(amount)
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        binding.rvHoldings.visibility = View.GONE
    }

    private fun showSuccess(state: MutualFundsUiState.Success) {
        binding.progressBar.visibility = View.GONE

        val fmt = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
        binding.tvTotalInvested.text = formatCompact(state.summary.totalInvested, fmt)
        binding.tvCurrentValue.text  = formatCompact(state.summary.currentValue, fmt)

        val gain = state.summary.absoluteReturn
        val gainPct = state.summary.absoluteReturnPct
        binding.tvGainLoss.text = "%s (%.2f%%)".format(formatCompact(gain, fmt), gainPct)
        binding.tvGainLoss.setTextColor(
            ContextCompat.getColor(requireContext(), if (gain >= 0) R.color.amount_positive else R.color.amount_negative)
        )

        if (state.funds.isEmpty()) {
            binding.rvHoldings.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.rvHoldings.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            adapter.submitList(state.funds, viewModel.expandedIsins.value)
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.rvHoldings.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = "Error: $message"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun confirmDeleteLot(lot: MutualFundLot) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Lot")
            .setMessage(getString(R.string.delete_lot_confirm))
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteLot(lot.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteFund(fund: MutualFundHolding) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Fund")
            .setMessage(getString(R.string.delete_fund_confirm, fund.schemeName))
            .setPositiveButton("Delete All Lots") { _, _ ->
                fund.lots.forEach { viewModel.deleteLot(it.id) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
