package com.example.personalwealthmanager.presentation.liabilities

import android.app.AlertDialog
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
import com.example.personalwealthmanager.data.remote.dto.CreateLiabilityRequest
import com.example.personalwealthmanager.data.remote.dto.UpdateLiabilityRequest
import com.example.personalwealthmanager.domain.model.PhysicalAsset
import com.example.personalwealthmanager.presentation.otherassets.OtherAssetsUiState
import com.example.personalwealthmanager.presentation.otherassets.OtherAssetsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AddEditLiabilityActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIABILITY_ID = "extra_liability_id"
        const val EXTRA_LOAN_TYPE = "extra_loan_type"
        const val EXTRA_LENDER_NAME = "extra_lender_name"
        const val EXTRA_LOAN_ACCOUNT_NUMBER = "extra_loan_account_number"
        const val EXTRA_INTEREST_TYPE = "extra_interest_type"
        const val EXTRA_INTEREST_RATE = "extra_interest_rate"
        const val EXTRA_ORIGINAL_AMOUNT = "extra_original_amount"
        const val EXTRA_OUTSTANDING_PRINCIPAL = "extra_outstanding_principal"
        const val EXTRA_EMI_AMOUNT = "extra_emi_amount"
        const val EXTRA_EMI_DUE_DAY = "extra_emi_due_day"
        const val EXTRA_START_DATE = "extra_start_date"
        const val EXTRA_TENURE_MONTHS = "extra_tenure_months"
        const val EXTRA_PHYSICAL_ASSET_ID = "extra_physical_asset_id"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_NOTES = "extra_notes"
    }

    private val viewModel: LiabilitiesViewModel by viewModels()
    private val assetsViewModel: OtherAssetsViewModel by viewModels()

    private lateinit var tvTitle: TextView
    private lateinit var spinnerLoanType: Spinner
    private lateinit var etLenderName: EditText
    private lateinit var etLoanAccountNumber: EditText
    private lateinit var spinnerInterestType: Spinner
    private lateinit var etInterestRate: EditText
    private lateinit var etOriginalAmount: EditText
    private lateinit var etOutstandingPrincipal: EditText
    private lateinit var etEmiAmount: EditText
    private lateinit var etEmiDueDay: EditText
    private lateinit var tvStartDate: TextView
    private lateinit var etTenureMonths: EditText
    private lateinit var spinnerLinkedAsset: Spinner
    private lateinit var layoutStatus: LinearLayout
    private lateinit var spinnerStatus: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var selectedStartDate: String = ""
    private var editLiabilityId: String? = null
    private var physicalAssets: List<PhysicalAsset> = emptyList()

    private val loanTypeLabels = listOf("Home Loan", "Car Loan", "Personal Loan", "Education Loan", "Business Loan", "Other")
    private val loanTypeValues = listOf("home", "car", "personal", "education", "business", "other")
    private val interestTypeLabels = listOf("Fixed", "Floating")
    private val interestTypeValues = listOf("fixed", "floating")
    private val statusLabels = listOf("Active", "Closed", "Foreclosed")
    private val statusValues = listOf("active", "closed", "foreclosed")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_liability)

        tvTitle = findViewById(R.id.tvTitle)
        spinnerLoanType = findViewById(R.id.spinnerLoanType)
        etLenderName = findViewById(R.id.etLenderName)
        etLoanAccountNumber = findViewById(R.id.etLoanAccountNumber)
        spinnerInterestType = findViewById(R.id.spinnerInterestType)
        etInterestRate = findViewById(R.id.etInterestRate)
        etOriginalAmount = findViewById(R.id.etOriginalAmount)
        etOutstandingPrincipal = findViewById(R.id.etOutstandingPrincipal)
        etEmiAmount = findViewById(R.id.etEmiAmount)
        etEmiDueDay = findViewById(R.id.etEmiDueDay)
        tvStartDate = findViewById(R.id.tvStartDate)
        etTenureMonths = findViewById(R.id.etTenureMonths)
        spinnerLinkedAsset = findViewById(R.id.spinnerLinkedAsset)
        layoutStatus = findViewById(R.id.layoutStatus)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        etNotes = findViewById(R.id.etNotes)
        btnSave = findViewById(R.id.btnSave)
        btnDelete = findViewById(R.id.btnDelete)

        setupSpinners()

        editLiabilityId = intent.getStringExtra(EXTRA_LIABILITY_ID)

        if (editLiabilityId != null) {
            tvTitle.text = "Edit Liability"
            btnDelete.visibility = View.VISIBLE
            layoutStatus.visibility = View.VISIBLE
            prefillEditData()
        }

        tvStartDate.setOnClickListener { showDatePicker() }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        btnSave.setOnClickListener { save() }
        btnDelete.setOnClickListener { confirmDelete() }

        observeActionState()
        assetsViewModel.fetchSummary()
        observeAssetsState()
    }

    private fun setupSpinners() {
        spinnerLoanType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, loanTypeLabels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerInterestType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, interestTypeLabels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusLabels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        // Initialize linked asset spinner with "None"
        updateAssetSpinner(emptyList())
    }

    private fun updateAssetSpinner(assets: List<PhysicalAsset>) {
        physicalAssets = assets
        val labels = mutableListOf("None")
        labels.addAll(assets.map { asset ->
            val icon = if (asset.assetType == "real_estate") "🏠" else "🚗"
            "$icon ${asset.label}"
        })
        spinnerLinkedAsset.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Restore selection in edit mode
        val existingAssetId = intent.getStringExtra(EXTRA_PHYSICAL_ASSET_ID)
        if (!existingAssetId.isNullOrEmpty()) {
            val idx = assets.indexOfFirst { it.id == existingAssetId }
            if (idx >= 0) spinnerLinkedAsset.setSelection(idx + 1)
        }
    }

    private fun observeAssetsState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                assetsViewModel.uiState.collect { state ->
                    if (state is OtherAssetsUiState.Success) {
                        updateAssetSpinner(state.summary.assets)
                    }
                }
            }
        }
    }

    private fun prefillEditData() {
        val loanType = intent.getStringExtra(EXTRA_LOAN_TYPE) ?: "home"
        spinnerLoanType.setSelection(loanTypeValues.indexOf(loanType).coerceAtLeast(0))

        etLenderName.setText(intent.getStringExtra(EXTRA_LENDER_NAME) ?: "")
        etLoanAccountNumber.setText(intent.getStringExtra(EXTRA_LOAN_ACCOUNT_NUMBER) ?: "")

        val interestType = intent.getStringExtra(EXTRA_INTEREST_TYPE) ?: "fixed"
        spinnerInterestType.setSelection(interestTypeValues.indexOf(interestType).coerceAtLeast(0))

        val rate = intent.getDoubleExtra(EXTRA_INTEREST_RATE, 0.0)
        if (rate > 0) etInterestRate.setText(rate.toString())

        val original = intent.getDoubleExtra(EXTRA_ORIGINAL_AMOUNT, 0.0)
        if (original > 0) etOriginalAmount.setText(original.toLong().toString())

        val outstanding = intent.getDoubleExtra(EXTRA_OUTSTANDING_PRINCIPAL, 0.0)
        if (outstanding > 0) etOutstandingPrincipal.setText(outstanding.toLong().toString())

        val emi = intent.getDoubleExtra(EXTRA_EMI_AMOUNT, 0.0)
        if (emi > 0) etEmiAmount.setText(emi.toLong().toString())

        val emiDay = intent.getIntExtra(EXTRA_EMI_DUE_DAY, 0)
        if (emiDay > 0) etEmiDueDay.setText(emiDay.toString())

        selectedStartDate = intent.getStringExtra(EXTRA_START_DATE) ?: ""
        if (selectedStartDate.isNotEmpty()) {
            tvStartDate.text = selectedStartDate
            tvStartDate.setTextColor(getColor(android.R.color.white))
        }

        val tenure = intent.getIntExtra(EXTRA_TENURE_MONTHS, 0)
        if (tenure > 0) etTenureMonths.setText(tenure.toString())

        val status = intent.getStringExtra(EXTRA_STATUS) ?: "active"
        spinnerStatus.setSelection(statusValues.indexOf(status).coerceAtLeast(0))

        etNotes.setText(intent.getStringExtra(EXTRA_NOTES) ?: "")
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        if (selectedStartDate.isNotEmpty()) {
            try {
                val parts = selectedStartDate.split("-")
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (_: Exception) {}
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedStartDate = "%04d-%02d-%02d".format(year, month + 1, day)
                tvStartDate.text = selectedStartDate
                tvStartDate.setTextColor(getColor(android.R.color.white))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun save() {
        val loanType = loanTypeValues[spinnerLoanType.selectedItemPosition]
        val lenderName = etLenderName.text.toString().trim()
        val loanAccountNumber = etLoanAccountNumber.text.toString().trim().ifEmpty { null }
        val interestType = interestTypeValues[spinnerInterestType.selectedItemPosition]
        val interestRate = etInterestRate.text.toString().trim().toDoubleOrNull()
        val originalAmount = etOriginalAmount.text.toString().trim().toDoubleOrNull()
        val outstandingPrincipal = etOutstandingPrincipal.text.toString().trim().toDoubleOrNull()
        val emiAmount = etEmiAmount.text.toString().trim().toDoubleOrNull()
        val emiDueDayStr = etEmiDueDay.text.toString().trim()
        val emiDueDay = if (emiDueDayStr.isNotEmpty()) emiDueDayStr.toIntOrNull() else null
        val tenureMonths = etTenureMonths.text.toString().trim().toIntOrNull()
        val notes = etNotes.text.toString().trim().ifEmpty { null }

        val selectedAssetIdx = spinnerLinkedAsset.selectedItemPosition
        val physicalAssetId = if (selectedAssetIdx > 0) physicalAssets[selectedAssetIdx - 1].id else null

        if (lenderName.isEmpty()) { etLenderName.error = "Required"; return }
        if (interestRate == null || interestRate <= 0) { etInterestRate.error = "Enter a valid rate"; return }
        if (originalAmount == null || originalAmount <= 0) { etOriginalAmount.error = "Enter a valid amount"; return }
        if (outstandingPrincipal == null || outstandingPrincipal < 0) { etOutstandingPrincipal.error = "Enter a valid amount"; return }
        if (emiAmount == null || emiAmount <= 0) { etEmiAmount.error = "Enter a valid EMI"; return }
        if (selectedStartDate.isEmpty()) { Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show(); return }
        if (tenureMonths == null || tenureMonths <= 0) { etTenureMonths.error = "Enter tenure in months"; return }
        if (emiDueDay != null && (emiDueDay < 1 || emiDueDay > 31)) { etEmiDueDay.error = "Must be 1-31"; return }

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        if (editLiabilityId != null) {
            val status = statusValues[spinnerStatus.selectedItemPosition]
            viewModel.updateLiability(
                editLiabilityId!!,
                UpdateLiabilityRequest(
                    loanType = loanType,
                    lenderName = lenderName,
                    loanAccountNumber = loanAccountNumber,
                    interestType = interestType,
                    interestRate = interestRate,
                    originalAmount = originalAmount,
                    outstandingPrincipal = outstandingPrincipal,
                    emiAmount = emiAmount,
                    emiDueDay = emiDueDay,
                    startDate = selectedStartDate,
                    tenureMonths = tenureMonths,
                    physicalAssetId = physicalAssetId,
                    status = status,
                    notes = notes
                )
            )
        } else {
            viewModel.createLiability(
                CreateLiabilityRequest(
                    loanType = loanType,
                    lenderName = lenderName,
                    loanAccountNumber = loanAccountNumber,
                    interestType = interestType,
                    interestRate = interestRate,
                    originalAmount = originalAmount,
                    outstandingPrincipal = outstandingPrincipal,
                    emiAmount = emiAmount,
                    emiDueDay = emiDueDay,
                    startDate = selectedStartDate,
                    tenureMonths = tenureMonths,
                    physicalAssetId = physicalAssetId,
                    notes = notes
                )
            )
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Liability")
            .setMessage("Delete this loan entry? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                editLiabilityId?.let { viewModel.deleteLiability(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeActionState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionState.collect { state ->
                    when (state) {
                        is LiabilitiesActionState.Saving -> {
                            btnSave.isEnabled = false
                            btnSave.text = "Saving..."
                        }
                        is LiabilitiesActionState.Saved -> {
                            viewModel.resetActionState()
                            finish()
                        }
                        is LiabilitiesActionState.Error -> {
                            btnSave.isEnabled = true
                            btnSave.text = "Save Liability"
                            Toast.makeText(this@AddEditLiabilityActivity, state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetActionState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
