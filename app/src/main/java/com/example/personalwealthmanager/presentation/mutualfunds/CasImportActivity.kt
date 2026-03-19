package com.example.personalwealthmanager.presentation.mutualfunds

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CasImportActivity : AppCompatActivity() {

    private val viewModel: CasImportViewModel by viewModels()
    private lateinit var adapter: CasImportPreviewAdapter

    private lateinit var layoutUpload  : LinearLayout
    private lateinit var layoutProgress: LinearLayout
    private lateinit var layoutPreview : LinearLayout
    private lateinit var tvProgressMsg : TextView
    private lateinit var tvPreviewTitle: TextView
    private lateinit var rvPreview     : RecyclerView
    private lateinit var btnConfirm    : Button

    private val pickPdf = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) readAndUpload(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cas_import)

        layoutUpload   = findViewById(R.id.layoutUpload)
        layoutProgress = findViewById(R.id.layoutProgress)
        layoutPreview  = findViewById(R.id.layoutPreview)
        tvProgressMsg  = findViewById(R.id.tvProgressMessage)
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle)
        rvPreview      = findViewById(R.id.rvPreview)
        btnConfirm     = findViewById(R.id.btnConfirmImport)

        adapter = CasImportPreviewAdapter()
        rvPreview.layoutManager = LinearLayoutManager(this)
        rvPreview.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSelectPdf).setOnClickListener {
            pickPdf.launch("application/pdf")
        }
        btnConfirm.setOnClickListener {
            val selected = adapter.getSelectedFunds()
            if (selected.isEmpty()) {
                Toast.makeText(this, "Select at least one fund to import", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.confirmImport(selected)
        }

        observeState()
    }

    private fun readAndUpload(uri: Uri) {
        val bytes = contentResolver.openInputStream(uri)?.readBytes()
        if (bytes == null || bytes.isEmpty()) {
            Toast.makeText(this, "Could not read PDF file", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = uri.lastPathSegment ?: "cas.pdf"
        viewModel.uploadCas(bytes, fileName)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is CasImportUiState.Idle -> showUpload()
                    is CasImportUiState.Uploading -> {
                        tvProgressMsg.text = getString(R.string.cas_upload_progress)
                        showProgress()
                    }
                    is CasImportUiState.Preview -> {
                        val funds = state.data.funds
                        adapter.submitList(funds)
                        tvPreviewTitle.text = getString(R.string.cas_preview_title, funds.size)
                        showPreview()
                    }
                    is CasImportUiState.Confirming -> {
                        tvProgressMsg.text = "Saving holdings…"
                        showProgress()
                    }
                    is CasImportUiState.Done -> {
                        val r = state.result
                        Toast.makeText(
                            this@CasImportActivity,
                            "Imported ${r.inserted} lots (${r.skipped} skipped as duplicates)",
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    is CasImportUiState.Error -> {
                        Toast.makeText(this@CasImportActivity, state.message, Toast.LENGTH_LONG).show()
                        showUpload()
                    }
                }
            }
        }
    }

    private fun showUpload() {
        layoutUpload.visibility   = View.VISIBLE
        layoutProgress.visibility = View.GONE
        layoutPreview.visibility  = View.GONE
    }

    private fun showProgress() {
        layoutUpload.visibility   = View.GONE
        layoutProgress.visibility = View.VISIBLE
        layoutPreview.visibility  = View.GONE
    }

    private fun showPreview() {
        layoutUpload.visibility   = View.GONE
        layoutProgress.visibility = View.GONE
        layoutPreview.visibility  = View.VISIBLE
    }
}
