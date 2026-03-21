package com.example.personalwealthmanager.presentation.liabilities

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
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
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.roundToLong

@AndroidEntryPoint
class AddEditLiabilityActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIABILITY_ID          = "extra_liability_id"
        const val EXTRA_LOAN_TYPE             = "extra_loan_type"
        const val EXTRA_LENDER_NAME           = "extra_lender_name"
        const val EXTRA_LOAN_ACCOUNT_NUMBER   = "extra_loan_account_number"
        const val EXTRA_INTEREST_TYPE         = "extra_interest_type"
        const val EXTRA_INTEREST_RATE         = "extra_interest_rate"
        const val EXTRA_ORIGINAL_AMOUNT       = "extra_original_amount"
        const val EXTRA_OUTSTANDING_PRINCIPAL = "extra_outstanding_principal"
        const val EXTRA_EMI_AMOUNT            = "extra_emi_amount"
        const val EXTRA_EMI_DUE_DAY           = "extra_emi_due_day"
        const val EXTRA_START_DATE            = "extra_start_date"
        const val EXTRA_TENURE_MONTHS         = "extra_tenure_months"
        const val EXTRA_PHYSICAL_ASSET_ID     = "extra_physical_asset_id"
        const val EXTRA_STATUS                = "extra_status"
        const val EXTRA_NOTES                 = "extra_notes"
    }

    private val viewModel: LiabilitiesViewModel by viewModels()
    private val assetsViewModel: OtherAssetsViewModel by viewModels()

    // Input views
    private lateinit var tvTitle: TextView
    private lateinit var spinnerLoanType: Spinner
    private lateinit var etLenderName: EditText
    private lateinit var etLoanAccountNumber: EditText
    private lateinit var spinnerInterestType: Spinner
    private lateinit var etOriginalAmount: EditText
    private lateinit var etInterestRate: EditText
    private lateinit var etTenureMonths: EditText
    private lateinit var tvStartDate: TextView
    private lateinit var btnCalculate: Button

    // Status card views
    private lateinit var cardLoanStatus: MaterialCardView
    private lateinit var tvStatusEmi: TextView
    private lateinit var tvStatusEmiDueDay: TextView
    private lateinit var tvStatusLoanEnd: TextView
    private lateinit var tvStatusTotalInterest: TextView
    private lateinit var tvStatusOutstanding: TextView
    private lateinit var tvStatusEmisPaid: TextView
    private lateinit var tvStatusEmisRemaining: TextView

    // Optional / edit views
    private lateinit var tvLinkedAssetTip: TextView
    private lateinit var layoutStatus: LinearLayout
    private lateinit var spinnerStatus: Spinner
    private lateinit var spinnerLinkedAsset: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button

    private var selectedStartDate: String = ""
    private var editLiabilityId: String? = null
    private var physicalAssets: List<PhysicalAsset> = emptyList()

    // Computed values (set by calculateAndShow)
    private var calculatedEmi: Double = 0.0
    private var calculatedOutstanding: Double = 0.0
    private var calculatedEmiDueDay: Int = 0
    private var isCalculated: Boolean = false

    private val loanTypeLabels   = listOf("Home Loan", "Car Loan", "Personal Loan", "Education Loan", "Business Loan", "Other")
    private val loanTypeValues   = listOf("home", "car", "personal", "education", "business", "other")
    private val interestTypeLabels = listOf("Fixed", "Floating")
    private val interestTypeValues = listOf("fixed", "floating")
    private val statusLabels     = listOf("Active", "Closed", "Foreclosed")
    private val statusValues     = listOf("active", "closed", "foreclosed")

    private val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_liability)

        tvTitle              = findViewById(R.id.tvTitle)
        spinnerLoanType      = findViewById(R.id.spinnerLoanType)
        etLenderName         = findViewById(R.id.etLenderName)
        etLoanAccountNumber  = findViewById(R.id.etLoanAccountNumber)
        spinnerInterestType  = findViewById(R.id.spinnerInterestType)
        etOriginalAmount     = findViewById(R.id.etOriginalAmount)
        etInterestRate       = findViewById(R.id.etInterestRate)
        etTenureMonths       = findViewById(R.id.etTenureMonths)
        tvStartDate          = findViewById(R.id.tvStartDate)
        btnCalculate         = findViewById(R.id.btnCalculate)

        cardLoanStatus          = findViewById(R.id.cardLoanStatus)
        tvStatusEmi             = findViewById(R.id.tvStatusEmi)
        tvStatusEmiDueDay       = findViewById(R.id.tvStatusEmiDueDay)
        tvStatusLoanEnd         = findViewById(R.id.tvStatusLoanEnd)
        tvStatusTotalInterest   = findViewById(R.id.tvStatusTotalInterest)
        tvStatusOutstanding     = findViewById(R.id.tvStatusOutstanding)
        tvStatusEmisPaid        = findViewById(R.id.tvStatusEmisPaid)
        tvStatusEmisRemaining   = findViewById(R.id.tvStatusEmisRemaining)

        tvLinkedAssetTip     = findViewById(R.id.tvLinkedAssetTip)
        layoutStatus         = findViewById(R.id.layoutStatus)
        spinnerStatus        = findViewById(R.id.spinnerStatus)
        spinnerLinkedAsset   = findViewById(R.id.spinnerLinkedAsset)
        etNotes              = findViewById(R.id.etNotes)
        btnSave              = findViewById(R.id.btnSave)
        btnCancel            = findViewById(R.id.btnCancel)
        btnDelete            = findViewById(R.id.btnDelete)

        setupSpinners()

        editLiabilityId = intent.getStringExtra(EXTRA_LIABILITY_ID)
        if (editLiabilityId != null) {
            tvTitle.text = "Edit Liability"
            btnSave.text = "Update Liability"
            btnCancel.visibility = View.VISIBLE
            btnDelete.visibility = View.VISIBLE
            layoutStatus.visibility = View.VISIBLE
            prefillEditData()
        }

        tvStartDate.setOnClickListener { showDatePicker() }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        btnCalculate.setOnClickListener { calculateAndShow() }
        btnSave.setOnClickListener { save() }
        btnCancel.setOnClickListener { finish() }
        btnDelete.setOnClickListener { confirmDelete() }

        // Show required warning for car/home loans
        spinnerLoanType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val loanType = loanTypeValues[position]
                when (loanType) {
                    "car" -> {
                        tvLinkedAssetTip.visibility = View.VISIBLE
                        tvLinkedAssetTip.text = "⚠️ Required — Link to your car. If not listed, add it in Other Assets first."
                        tvLinkedAssetTip.setBackgroundColor(0x33FF8F00.toInt())
                        tvLinkedAssetTip.setTextColor(0xFFFF8F00.toInt())
                    }
                    "home" -> {
                        tvLinkedAssetTip.visibility = View.VISIBLE
                        tvLinkedAssetTip.text = "⚠️ Required — Link to your property. If not listed, add it in Other Assets first."
                        tvLinkedAssetTip.setBackgroundColor(0x33FF8F00.toInt())
                        tvLinkedAssetTip.setTextColor(0xFFFF8F00.toInt())
                    }
                    else -> tvLinkedAssetTip.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        observeActionState()
        assetsViewModel.fetchSummary()
        observeAssetsState()
    }

    // ── Spinners ────────────────────────────────────────────────────────────

    private fun setupSpinners() {
        spinnerLoanType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, loanTypeLabels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerInterestType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, interestTypeLabels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerStatus.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusLabels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        updateAssetSpinner(emptyList())
    }

    private fun updateAssetSpinner(assets: List<PhysicalAsset>) {
        physicalAssets = assets
        val labels = mutableListOf("None")
        labels.addAll(assets.map { "${if (it.assetType == "real_estate") "🏠" else "🚗"} ${it.label}" })
        spinnerLinkedAsset.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

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
                    if (state is OtherAssetsUiState.Success) updateAssetSpinner(state.summary.assets)
                }
            }
        }
    }

    // ── Prefill (edit mode) ─────────────────────────────────────────────────

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

        val tenure = intent.getIntExtra(EXTRA_TENURE_MONTHS, 0)
        if (tenure > 0) etTenureMonths.setText(tenure.toString())

        // Backend returns full ISO timestamp "2023-10-10T00:00:00.000Z" — keep only date part
        selectedStartDate = (intent.getStringExtra(EXTRA_START_DATE) ?: "").take(10)
        if (selectedStartDate.isNotEmpty()) {
            tvStartDate.text = selectedStartDate
            tvStartDate.setTextColor(0xFF1A1A1A.toInt())
        }

        val status = intent.getStringExtra(EXTRA_STATUS) ?: "active"
        spinnerStatus.setSelection(statusValues.indexOf(status).coerceAtLeast(0))

        etNotes.setText(intent.getStringExtra(EXTRA_NOTES) ?: "")

        // Auto-calculate if all parameters are available
        if (original > 0 && rate > 0 && tenure > 0 && selectedStartDate.isNotEmpty()) {
            calculateAndShow()
        }
    }

    // ── Date Picker ─────────────────────────────────────────────────────────

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
                tvStartDate.setTextColor(0xFF1A1A1A.toInt())
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ── EMI / Loan Calculation ───────────────────────────────────────────────

    private fun calculateAndShow() {
        val principal = etOriginalAmount.text.toString().trim().toDoubleOrNull()
        val rate      = etInterestRate.text.toString().trim().toDoubleOrNull()
        val tenure    = etTenureMonths.text.toString().trim().toIntOrNull()

        if (principal == null || principal <= 0) { etOriginalAmount.error = "Required"; return }
        if (rate == null || rate <= 0)           { etInterestRate.error = "Required"; return }
        if (tenure == null || tenure <= 0)       { etTenureMonths.error = "Required"; return }
        if (selectedStartDate.isEmpty()) {
            Toast.makeText(this, "Select a start date", Toast.LENGTH_SHORT).show()
            return
        }

        val r       = rate / 1200.0
        val factor  = (1 + r).pow(tenure.toDouble())
        val emi     = (principal * r * factor / (factor - 1)).roundToLong().toDouble()

        val parts   = selectedStartDate.take(10).split("-")
        val startYear  = parts[0].toInt()
        val startMonth = parts[1].toInt()
        val dueDay     = parts[2].toInt()

        val emisPaid = countEmisPaid(startYear, startMonth, dueDay, tenure)

        val outstanding = if (emisPaid <= 0) {
            principal
        } else {
            val paidFactor = (1 + r).pow(emisPaid.toDouble())
            (principal * paidFactor - emi * (paidFactor - 1) / r).coerceAtLeast(0.0)
        }

        // Loan end date = start date + tenure months
        val endCal = Calendar.getInstance().apply {
            set(startYear, startMonth - 1, dueDay)
            add(Calendar.MONTH, tenure)
        }
        val loanEndStr = "${dueDay} ${monthNames[endCal.get(Calendar.MONTH)]} ${endCal.get(Calendar.YEAR)}"

        val totalInterest = emi * tenure - principal

        // Store for save()
        calculatedEmi        = emi
        calculatedOutstanding = outstanding
        calculatedEmiDueDay  = dueDay
        isCalculated         = true

        // Populate card
        tvStatusEmi.text           = formatAmount(emi)
        tvStatusEmiDueDay.text     = ordinal(dueDay) + " of month"
        tvStatusLoanEnd.text       = loanEndStr
        tvStatusTotalInterest.text = formatAmount(totalInterest)
        tvStatusOutstanding.text   = formatAmount(outstanding)
        tvStatusEmisPaid.text      = "$emisPaid of $tenure"
        tvStatusEmisRemaining.text = "${tenure - emisPaid} EMIs"

        cardLoanStatus.visibility = View.VISIBLE
    }

    private fun countEmisPaid(startYear: Int, startMonth: Int, dueDay: Int, tenure: Int): Int {
        val today     = Calendar.getInstance()
        val todayYear = today.get(Calendar.YEAR)
        val todayMon  = today.get(Calendar.MONTH) + 1
        val todayDay  = today.get(Calendar.DAY_OF_MONTH)

        // First EMI is one month after start date
        var year  = startYear
        var month = startMonth + 1
        if (month > 12) { month = 1; year++ }

        var count = 0
        while (true) {
            if (year > todayYear) break
            if (year == todayYear && month > todayMon) break
            if (year == todayYear && month == todayMon && dueDay > todayDay) break
            count++
            if (count >= tenure) break
            month++
            if (month > 12) { month = 1; year++ }
        }
        return count
    }

    private fun formatAmount(amount: Double): String =
        "₹%,.0f".format(amount)

    private fun ordinal(n: Int): String = when {
        n in 11..13  -> "${n}th"
        n % 10 == 1  -> "${n}st"
        n % 10 == 2  -> "${n}nd"
        n % 10 == 3  -> "${n}rd"
        else         -> "${n}th"
    }

    // ── Save ────────────────────────────────────────────────────────────────

    private fun save() {
        if (!isCalculated) {
            Toast.makeText(this, "Tap 'Calculate Status' first", Toast.LENGTH_SHORT).show()
            return
        }

        val lenderName = etLenderName.text.toString().trim()
        if (lenderName.isEmpty()) { etLenderName.error = "Required"; return }

        val loanType          = loanTypeValues[spinnerLoanType.selectedItemPosition]
        val selectedAssetIdx  = spinnerLinkedAsset.selectedItemPosition

        // Linked asset is mandatory for car and home loans
        if ((loanType == "car" || loanType == "home") && selectedAssetIdx == 0) {
            Toast.makeText(
                this,
                if (loanType == "car") "Please link this loan to a car. Add one in Other Assets first."
                else "Please link this loan to a property. Add one in Other Assets first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val loanAccountNumber = etLoanAccountNumber.text.toString().trim().ifEmpty { null }
        val interestType      = interestTypeValues[spinnerInterestType.selectedItemPosition]
        val interestRate      = etInterestRate.text.toString().trim().toDoubleOrNull() ?: return
        val originalAmount    = etOriginalAmount.text.toString().trim().toDoubleOrNull() ?: return
        val tenureMonths      = etTenureMonths.text.toString().trim().toIntOrNull() ?: return
        val notes             = etNotes.text.toString().trim().ifEmpty { null }

        val physicalAssetId   = if (selectedAssetIdx > 0) physicalAssets[selectedAssetIdx - 1].id else null

        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        if (editLiabilityId != null) {
            val status = statusValues[spinnerStatus.selectedItemPosition]
            viewModel.updateLiability(
                editLiabilityId!!,
                UpdateLiabilityRequest(
                    loanType             = loanType,
                    lenderName           = lenderName,
                    loanAccountNumber    = loanAccountNumber,
                    interestType         = interestType,
                    interestRate         = interestRate,
                    originalAmount       = originalAmount,
                    outstandingPrincipal = calculatedOutstanding,
                    emiAmount            = calculatedEmi,
                    emiDueDay            = calculatedEmiDueDay,
                    startDate            = selectedStartDate,
                    tenureMonths         = tenureMonths,
                    physicalAssetId      = physicalAssetId,
                    status               = status,
                    notes                = notes
                )
            )
        } else {
            viewModel.createLiability(
                CreateLiabilityRequest(
                    loanType             = loanType,
                    lenderName           = lenderName,
                    loanAccountNumber    = loanAccountNumber,
                    interestType         = interestType,
                    interestRate         = interestRate,
                    originalAmount       = originalAmount,
                    outstandingPrincipal = calculatedOutstanding,
                    emiAmount            = calculatedEmi,
                    emiDueDay            = calculatedEmiDueDay,
                    startDate            = selectedStartDate,
                    tenureMonths         = tenureMonths,
                    physicalAssetId      = physicalAssetId,
                    notes                = notes
                )
            )
        }
    }

    // ── Delete ──────────────────────────────────────────────────────────────

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

    // ── Observe ─────────────────────────────────────────────────────────────

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
