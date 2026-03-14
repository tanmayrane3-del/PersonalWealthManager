package com.example.personalwealthmanager.presentation.metals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalwealthmanager.databinding.FragmentMetalsBinding
import com.example.personalwealthmanager.domain.model.MetalHolding
import com.example.personalwealthmanager.domain.model.MetalRates
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class MetalsFragment : Fragment() {

    private var _binding: FragmentMetalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MetalsViewModel by activityViewModels()
    private lateinit var adapter: MetalHoldingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMetalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MetalHoldingAdapter(
            onEdit = { holding -> showEditSheet(holding) },
            onDelete = { holding -> confirmDelete(holding) }
        )
        binding.rvHoldings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHoldings.adapter = adapter
        binding.rvHoldings.isNestedScrollingEnabled = false

        binding.fabAddHolding.setOnClickListener {
            AddEditMetalBottomSheet.newInstance().show(childFragmentManager, "add_metal")
        }

        observeState()
        viewModel.fetchAll()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MetalsUiState.Idle -> Unit
                    is MetalsUiState.Loading -> showLoading()
                    is MetalsUiState.Success -> showSuccess(state)
                    is MetalsUiState.Error -> showError(state.message)
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        binding.rvHoldings.visibility = View.GONE
    }

    private fun showSuccess(state: MetalsUiState.Success) {
        binding.progressBar.visibility = View.GONE
        updateRateCard(state.rates)
        updateTotalValue(state.totalValue)

        if (state.holdings.isEmpty()) {
            binding.rvHoldings.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.rvHoldings.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            adapter.submitGrouped(state.holdings)
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.rvHoldings.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = "Error: $message"
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun updateRateCard(rates: MetalRates) {
        val inrFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        binding.tvRate22k.text = inrFormatter.format(rates.gold22kPerGram)
        binding.tvRate24k.text = inrFormatter.format(rates.gold24kPerGram)

        try {
            val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFmt.timeZone = TimeZone.getTimeZone("UTC")
            val date: Date = inputFmt.parse(rates.fetchedAt) ?: Date()
            val displayFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            displayFmt.timeZone = TimeZone.getDefault()
            binding.tvLastUpdated.text = "Updated: ${displayFmt.format(date)}"
        } catch (e: Exception) {
            binding.tvLastUpdated.text = "Updated: --"
        }
    }

    private fun updateTotalValue(totalValue: Double) {
        val inrFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        binding.tvTotalValue.text = inrFormatter.format(totalValue)
    }

    private fun showEditSheet(holding: MetalHolding) {
        AddEditMetalBottomSheet.newInstance(holding)
            .show(childFragmentManager, "edit_metal")
    }

    private fun confirmDelete(holding: MetalHolding) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Holding")
            .setMessage("Delete \"${holding.label}\"?")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteHolding(holding.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
