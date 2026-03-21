package com.example.personalwealthmanager.presentation.liabilities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.Liability
import com.example.personalwealthmanager.domain.model.LiabilitySummary
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class LiabilitiesFragment : Fragment() {

    private val viewModel: LiabilitiesViewModel by activityViewModels()
    private lateinit var adapter: LiabilityCardAdapter

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var rvLiabilities: RecyclerView
    private lateinit var summaryCard: View
    private lateinit var tvTotalOutstanding: TextView
    private lateinit var tvTotalEmi: TextView
    private lateinit var tvActiveCount: TextView
    private lateinit var fabAddLiability: FloatingActionButton

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_liabilities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        emptyState = view.findViewById(R.id.emptyState)
        rvLiabilities = view.findViewById(R.id.rvLiabilities)
        summaryCard = view.findViewById(R.id.summaryCard)
        tvTotalOutstanding = view.findViewById(R.id.tvTotalOutstanding)
        tvTotalEmi = view.findViewById(R.id.tvTotalEmi)
        tvActiveCount = view.findViewById(R.id.tvActiveCount)
        fabAddLiability = view.findViewById(R.id.fabAddLiability)

        adapter = LiabilityCardAdapter(onClick = { liability -> openEdit(liability) })
        rvLiabilities.layoutManager = LinearLayoutManager(requireContext())
        rvLiabilities.adapter = adapter
        rvLiabilities.isNestedScrollingEnabled = false

        fabAddLiability.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditLiabilityActivity::class.java))
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
                        is LiabilitiesUiState.Idle -> Unit
                        is LiabilitiesUiState.Loading -> showLoading()
                        is LiabilitiesUiState.Success -> showSuccess(state.summary)
                        is LiabilitiesUiState.Error -> showError(state.message)
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
                        is LiabilitiesActionState.Error -> {
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
        rvLiabilities.visibility = View.GONE
        summaryCard.visibility = View.GONE
    }

    private fun showSuccess(summary: LiabilitySummary) {
        progressBar.visibility = View.GONE
        summaryCard.visibility = View.VISIBLE

        tvTotalOutstanding.text = currencyFormat.format(summary.totalOutstanding)
        tvTotalEmi.text = currencyFormat.format(summary.totalEmi)
        tvActiveCount.text = "${summary.activeCount} active loan${if (summary.activeCount != 1) "s" else ""}"

        if (summary.liabilities.isEmpty()) {
            rvLiabilities.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            emptyState.visibility = View.GONE
            rvLiabilities.visibility = View.VISIBLE
            adapter.submitList(summary.liabilities)
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        rvLiabilities.visibility = View.GONE
        summaryCard.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun openEdit(liability: Liability) {
        val intent = Intent(requireContext(), AddEditLiabilityActivity::class.java)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_LIABILITY_ID, liability.id)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_LOAN_TYPE, liability.loanType)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_LENDER_NAME, liability.lenderName)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_LOAN_ACCOUNT_NUMBER, liability.loanAccountNumber ?: "")
        intent.putExtra(AddEditLiabilityActivity.EXTRA_INTEREST_TYPE, liability.interestType)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_INTEREST_RATE, liability.interestRate)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_ORIGINAL_AMOUNT, liability.originalAmount)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_OUTSTANDING_PRINCIPAL, liability.outstandingPrincipal)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_EMI_AMOUNT, liability.emiAmount)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_EMI_DUE_DAY, liability.emiDueDay ?: 0)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_START_DATE, liability.startDate)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_TENURE_MONTHS, liability.tenureMonths)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_PHYSICAL_ASSET_ID, liability.physicalAssetId ?: "")
        intent.putExtra(AddEditLiabilityActivity.EXTRA_STATUS, liability.status)
        intent.putExtra(AddEditLiabilityActivity.EXTRA_NOTES, liability.notes ?: "")
        startActivity(intent)
    }

    private fun confirmDelete(liability: Liability) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Liability")
            .setMessage("Delete loan from \"${liability.lenderName}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteLiability(liability.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
