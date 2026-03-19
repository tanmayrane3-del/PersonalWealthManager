package com.example.personalwealthmanager.presentation.mutualfunds

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.personalwealthmanager.data.remote.dto.AddLotRequest
import com.example.personalwealthmanager.databinding.BottomSheetAddMfBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddMutualFundBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddMfBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MutualFundsViewModel by activityViewModels()

    private var lookupJob: Job? = null
    private var resolvedSchemeCode: String? = null
    private var resolvedSchemeName: String? = null
    private var resolvedAmcName: String?    = null
    private var selectedDate: String?       = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddMfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ISIN field: debounced lookup
        binding.etIsin.doAfterTextChanged { text ->
            val isin = text?.toString()?.trim()?.uppercase() ?: ""
            if (isin.length == 12) {
                lookupJob?.cancel()
                lookupJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(500)
                    viewModel.lookupScheme(isin)
                }
            } else {
                // Reset resolved info if ISIN is changed
                resolvedSchemeCode = null
                resolvedSchemeName = null
                resolvedAmcName = null
                binding.etSchemeName.setText("")
                binding.tvLookupStatus.visibility = View.GONE
            }
        }

        // Date picker
        binding.etPurchaseDate.setOnClickListener { showDatePicker() }

        // Observe lookup state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lookupState.collect { state ->
                when (state) {
                    is LookupState.Idle -> {
                        binding.tvLookupStatus.visibility = View.GONE
                    }
                    is LookupState.Loading -> {
                        binding.tvLookupStatus.text = getString(com.example.personalwealthmanager.R.string.looking_up_scheme)
                        binding.tvLookupStatus.visibility = View.VISIBLE
                    }
                    is LookupState.Found -> {
                        resolvedSchemeCode = state.result.schemeCode
                        resolvedSchemeName = state.result.schemeName
                        resolvedAmcName    = state.result.amcName
                        binding.etSchemeName.setText(state.result.schemeName ?: "")
                        binding.tvLookupStatus.visibility = View.GONE
                    }
                    is LookupState.Error -> {
                        binding.tvLookupStatus.text = getString(com.example.personalwealthmanager.R.string.scheme_not_found)
                        binding.tvLookupStatus.visibility = View.VISIBLE
                        resolvedSchemeCode = null
                        binding.etSchemeName.setText("")
                    }
                }
            }
        }

        binding.btnSave.setOnClickListener { onSave() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val dateStr = "%04d-%02d-%02d".format(year, month + 1, day)
                selectedDate = dateStr
                binding.etPurchaseDate.setText(dateStr)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun onSave() {
        val isin       = binding.etIsin.text?.toString()?.trim()?.uppercase() ?: ""
        val unitsStr   = binding.etUnits.text?.toString()?.trim() ?: ""
        val navStr     = binding.etPurchaseNav.text?.toString()?.trim() ?: ""
        val date       = selectedDate

        if (isin.length != 12) {
            binding.etIsin.error = "Enter a valid 12-character ISIN"
            return
        }
        val units = unitsStr.toDoubleOrNull()
        if (units == null || units <= 0) {
            Toast.makeText(requireContext(), "Enter valid units", Toast.LENGTH_SHORT).show()
            return
        }
        val nav = navStr.toDoubleOrNull()
        if (nav == null || nav <= 0) {
            Toast.makeText(requireContext(), "Enter valid purchase NAV", Toast.LENGTH_SHORT).show()
            return
        }
        if (date.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Select purchase date", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AddLotRequest(
            isin         = isin,
            schemeCode   = resolvedSchemeCode,
            schemeName   = resolvedSchemeName ?: binding.etSchemeName.text?.toString()?.trim(),
            amcName      = resolvedAmcName,
            folioNumber  = null,
            units        = units,
            purchaseNav  = nav,
            purchaseDate = date,
            notes        = null
        )

        viewModel.addLot(request)
        viewModel.resetLookup()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetLookup()
        _binding = null
    }
}
