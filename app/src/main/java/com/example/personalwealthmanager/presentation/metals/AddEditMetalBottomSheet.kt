package com.example.personalwealthmanager.presentation.metals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.data.remote.dto.MetalHoldingRequest
import com.example.personalwealthmanager.databinding.BottomSheetAddEditMetalBinding
import com.example.personalwealthmanager.domain.model.MetalHolding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditMetalBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEditMetalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MetalsViewModel by activityViewModels()

    private var editHolding: MetalHolding? = null

    companion object {
        private const val ARG_HOLDING_ID = "holding_id"
        private const val ARG_METAL_TYPE = "metal_type"
        private const val ARG_SUB_TYPE = "sub_type"
        private const val ARG_LABEL = "label"
        private const val ARG_QUANTITY = "quantity"
        private const val ARG_PURITY = "purity"
        private const val ARG_NOTES = "notes"

        fun newInstance(holding: MetalHolding? = null): AddEditMetalBottomSheet {
            return AddEditMetalBottomSheet().apply {
                if (holding != null) {
                    arguments = Bundle().apply {
                        putString(ARG_HOLDING_ID, holding.id)
                        putString(ARG_METAL_TYPE, holding.metalType)
                        putString(ARG_SUB_TYPE, holding.subType)
                        putString(ARG_LABEL, holding.label)
                        putDouble(ARG_QUANTITY, holding.quantityGrams)
                        putString(ARG_PURITY, holding.purity)
                        putString(ARG_NOTES, holding.notes)
                    }
                }
            }
        }
    }

    private val metalTypes = listOf("Physical Gold", "Digital Gold", "SGB")
    private val metalTypeValues = listOf("physical_gold", "digital_gold", "sgb")
    private val subTypes = listOf("Jewellery", "Coins", "Bars")
    private val subTypeValues = listOf("jewellery", "coins", "bars")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddEditMetalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore edit args if present
        val editId = arguments?.getString(ARG_HOLDING_ID)
        if (editId != null) {
            binding.tvTitle.text = "Edit Metal Holding"
        }

        setupSpinners()
        setupPurityVisibility()

        // Populate for edit mode
        if (editId != null) {
            val metalType = arguments?.getString(ARG_METAL_TYPE) ?: "physical_gold"
            val typeIdx = metalTypeValues.indexOf(metalType).coerceAtLeast(0)
            binding.spinnerMetalType.setSelection(typeIdx)

            arguments?.getString(ARG_SUB_TYPE)?.let { sub ->
                val subIdx = subTypeValues.indexOf(sub).coerceAtLeast(0)
                binding.spinnerSubType.setSelection(subIdx)
            }

            val purity = arguments?.getString(ARG_PURITY) ?: "24k"
            if (purity == "22k") binding.rb22k.isChecked = true else binding.rb24k.isChecked = true

            binding.etLabel.setText(arguments?.getString(ARG_LABEL))
            binding.etQuantityGrams.setText(
                arguments?.getDouble(ARG_QUANTITY, 0.0)?.let {
                    if (it > 0) it.toString() else ""
                }
            )
            binding.etNotes.setText(arguments?.getString(ARG_NOTES))
        }

        binding.btnSave.setOnClickListener { onSave(editId) }
    }

    private fun setupSpinners() {
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, metalTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMetalType.adapter = typeAdapter

        val subAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subTypes)
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSubType.adapter = subAdapter

        binding.spinnerMetalType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                updatePhysicalGoldVisibility(position == 0)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupPurityVisibility() {
        updatePhysicalGoldVisibility(binding.spinnerMetalType.selectedItemPosition == 0)
    }

    private fun updatePhysicalGoldVisibility(isPhysical: Boolean) {
        val visibility = if (isPhysical) View.VISIBLE else View.GONE
        binding.tvSubTypeLabel.visibility = visibility
        binding.spinnerSubType.visibility = visibility
        binding.tvPurityLabel.visibility = visibility
        binding.rgPurity.visibility = visibility
    }

    private fun onSave(editId: String?) {
        val label = binding.etLabel.text?.toString()?.trim() ?: ""
        val quantityStr = binding.etQuantityGrams.text?.toString()?.trim() ?: ""
        val notes = binding.etNotes.text?.toString()?.trim()?.ifBlank { null }

        val metalTypeIdx = binding.spinnerMetalType.selectedItemPosition
        val metalType = metalTypeValues.getOrElse(metalTypeIdx) { "physical_gold" }
        val isPhysical = metalType == "physical_gold"

        val subType = if (isPhysical) subTypeValues.getOrElse(binding.spinnerSubType.selectedItemPosition) { "jewellery" } else null
        val purity = if (binding.rb22k.isChecked) "22k" else "24k"

        if (label.isEmpty()) {
            binding.etLabel.error = "Label is required"
            return
        }
        val quantity = quantityStr.toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            binding.etQuantityGrams.error = "Enter a valid quantity"
            return
        }

        val request = MetalHoldingRequest(
            metalType = metalType,
            subType = subType,
            label = label,
            quantityGrams = quantity,
            purity = purity,
            notes = notes
        )

        if (editId != null) {
            viewModel.updateHolding(editId, request)
        } else {
            viewModel.addHolding(request)
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
