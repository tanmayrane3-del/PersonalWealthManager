package com.example.personalwealthmanager.presentation.otherassets

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.data.remote.dto.CreatePhysicalAssetRequest
import com.example.personalwealthmanager.data.remote.dto.UpdatePhysicalAssetRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddEditOtherAssetActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ASSET_ID = "extra_asset_id"
        const val EXTRA_ASSET_TYPE = "extra_asset_type"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_PURCHASE_PRICE = "extra_purchase_price"
        const val EXTRA_PURCHASE_DATE = "extra_purchase_date"
        const val EXTRA_CURRENT_MARKET_VALUE = "extra_current_market_value"
        const val EXTRA_DEPRECIATION_RATE = "extra_depreciation_rate"
        const val EXTRA_NOTES = "extra_notes"
        const val EXTRA_HAS_ACTIVE_LOAN = "extra_has_active_loan"
    }

    private val viewModel: OtherAssetsViewModel by viewModels()

    private lateinit var tvTitle: TextView
    private lateinit var spinnerAssetType: Spinner
    private lateinit var etLabel: EditText
    private lateinit var etPurchasePrice: EditText
    private lateinit var tvPurchaseDate: TextView
    private lateinit var layoutRealEstate: LinearLayout
    private lateinit var etCurrentMarketValue: EditText
    private lateinit var layoutVehicle: LinearLayout
    private lateinit var etDepreciationRate: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var cardLoanBanner: View

    private var selectedDate: String = ""
    private var editAssetId: String? = null
    private var hasActiveLoan: Boolean = false

    private val assetTypeLabels = listOf("Home / Property", "Car / Vehicle")
    private val assetTypeValues = listOf("real_estate", "vehicle")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_other_asset)

        tvTitle = findViewById(R.id.tvTitle)
        spinnerAssetType = findViewById(R.id.spinnerAssetType)
        etLabel = findViewById(R.id.etLabel)
        etPurchasePrice = findViewById(R.id.etPurchasePrice)
        tvPurchaseDate = findViewById(R.id.tvPurchaseDate)
        layoutRealEstate = findViewById(R.id.layoutRealEstate)
        etCurrentMarketValue = findViewById(R.id.etCurrentMarketValue)
        layoutVehicle = findViewById(R.id.layoutVehicle)
        etDepreciationRate = findViewById(R.id.etDepreciationRate)
        etNotes = findViewById(R.id.etNotes)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)
        cardLoanBanner = findViewById(R.id.cardLoanBanner)

        // Setup spinner
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, assetTypeLabels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAssetType.adapter = spinnerAdapter

        // Populate edit mode data
        editAssetId = intent.getStringExtra(EXTRA_ASSET_ID)
        hasActiveLoan = intent.getBooleanExtra(EXTRA_HAS_ACTIVE_LOAN, false)

        if (editAssetId != null) {
            tvTitle.text = "Edit Asset"
            btnDelete.visibility = View.VISIBLE

            val assetType = intent.getStringExtra(EXTRA_ASSET_TYPE) ?: "real_estate"
            val typeIdx = assetTypeValues.indexOf(assetType).coerceAtLeast(0)
            spinnerAssetType.setSelection(typeIdx)

            etLabel.setText(intent.getStringExtra(EXTRA_LABEL) ?: "")
            val price = intent.getDoubleExtra(EXTRA_PURCHASE_PRICE, 0.0)
            if (price > 0) etPurchasePrice.setText(price.toLong().toString())

            selectedDate = intent.getStringExtra(EXTRA_PURCHASE_DATE) ?: ""
            tvPurchaseDate.text = if (selectedDate.isNotEmpty()) selectedDate else "Tap to select date"
            tvPurchaseDate.setTextColor(
                if (selectedDate.isNotEmpty())
                    getColor(android.R.color.white)
                else
                    0x88FFFFFF.toInt()
            )

            if (assetType == "real_estate") {
                val cmv = intent.getDoubleExtra(EXTRA_CURRENT_MARKET_VALUE, 0.0)
                if (cmv > 0) etCurrentMarketValue.setText(cmv.toLong().toString())
            } else {
                val dr = intent.getDoubleExtra(EXTRA_DEPRECIATION_RATE, 10.0)
                etDepreciationRate.setText(dr.toInt().toString())
            }
            etNotes.setText(intent.getStringExtra(EXTRA_NOTES) ?: "")

            if (hasActiveLoan) {
                cardLoanBanner.visibility = View.VISIBLE
                spinnerAssetType.isEnabled = false
                btnDelete.isEnabled = false
                btnDelete.alpha = 0.5f
                btnDelete.text = "Delete Asset (Linked to Active Loan)"
            }
        }

        // Update conditional fields on type change
        spinnerAssetType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                updateConditionalFields(assetTypeValues[pos])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        updateConditionalFields(assetTypeValues[spinnerAssetType.selectedItemPosition])

        // Date picker
        tvPurchaseDate.setOnClickListener { showDatePicker() }

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Save
        btnSave.setOnClickListener { save() }

        // Delete
        btnDelete.setOnClickListener {
            if (hasActiveLoan) {
                Toast.makeText(this, "Cannot delete — active loan linked to this asset", Toast.LENGTH_LONG).show()
            } else {
                confirmDelete()
            }
        }

        observeActionState()
    }

    private fun updateConditionalFields(assetType: String) {
        if (assetType == "real_estate") {
            layoutRealEstate.visibility = View.VISIBLE
            layoutVehicle.visibility = View.GONE
        } else {
            layoutRealEstate.visibility = View.GONE
            layoutVehicle.visibility = View.VISIBLE
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        if (selectedDate.isNotEmpty()) {
            try {
                val parts = selectedDate.split("-")
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (_: Exception) {}
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                tvPurchaseDate.text = selectedDate
                tvPurchaseDate.setTextColor(getColor(android.R.color.white))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun save() {
        val assetType = assetTypeValues[spinnerAssetType.selectedItemPosition]
        val label = etLabel.text.toString().trim()
        val priceStr = etPurchasePrice.text.toString().trim()
        val price = priceStr.toDoubleOrNull()
        val notes = etNotes.text.toString().trim().ifEmpty { null }

        if (label.isEmpty()) { etLabel.error = "Required"; return }
        if (price == null || price <= 0) { etPurchasePrice.error = "Enter a valid price"; return }
        if (selectedDate.isEmpty()) { Toast.makeText(this, "Please select a purchase date", Toast.LENGTH_SHORT).show(); return }

        var currentMarketValue: Double? = null
        var depreciationRate: Double? = null

        if (assetType == "real_estate") {
            val cmvStr = etCurrentMarketValue.text.toString().trim()
            currentMarketValue = cmvStr.toDoubleOrNull()
            if (currentMarketValue == null || currentMarketValue < 0) {
                etCurrentMarketValue.error = "Enter a valid market value"
                return
            }
        } else {
            val drStr = etDepreciationRate.text.toString().trim()
            depreciationRate = drStr.toDoubleOrNull() ?: 10.0
        }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        if (editAssetId != null) {
            viewModel.updateAsset(
                editAssetId!!,
                UpdatePhysicalAssetRequest(
                    assetType = assetType,
                    label = label,
                    purchasePrice = price,
                    purchaseDate = selectedDate,
                    currentMarketValue = currentMarketValue,
                    depreciationRatePct = depreciationRate,
                    notes = notes
                )
            )
        } else {
            viewModel.createAsset(
                CreatePhysicalAssetRequest(
                    assetType = assetType,
                    label = label,
                    purchasePrice = price,
                    purchaseDate = selectedDate,
                    currentMarketValue = currentMarketValue,
                    depreciationRatePct = depreciationRate,
                    notes = notes
                )
            )
        }
    }

    private fun confirmDelete() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Asset")
            .setMessage("Delete this asset? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                editAssetId?.let { viewModel.deleteAsset(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeActionState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionState.collect { state ->
                    when (state) {
                        is OtherAssetsActionState.Saving -> {
                            btnSave.isEnabled = false
                            btnSave.text = "Saving..."
                        }
                        is OtherAssetsActionState.Saved -> {
                            viewModel.resetActionState()
                            finish()
                        }
                        is OtherAssetsActionState.Error -> {
                            btnSave.isEnabled = true
                            btnSave.text = "Save Asset"
                            Toast.makeText(this@AddEditOtherAssetActivity, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetActionState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
