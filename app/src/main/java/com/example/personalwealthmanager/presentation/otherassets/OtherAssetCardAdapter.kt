package com.example.personalwealthmanager.presentation.otherassets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.core.utils.PhysicalAssetCagrCalculator
import com.example.personalwealthmanager.domain.model.PhysicalAsset
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class OtherAssetCardAdapter(
    private val onLongPress: (PhysicalAsset) -> Unit
) : ListAdapter<PhysicalAsset, OtherAssetCardAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    private val inputDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<PhysicalAsset> = object : DiffUtil.ItemCallback<PhysicalAsset>() {
            override fun areItemsTheSame(old: PhysicalAsset, new: PhysicalAsset) = old.id == new.id
            override fun areContentsTheSame(old: PhysicalAsset, new: PhysicalAsset) = old == new
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAssetIcon: TextView = itemView.findViewById(R.id.tvAssetIcon)
        val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
        val tvAssetTypeBadge: TextView = itemView.findViewById(R.id.tvAssetTypeBadge)
        val tvLoanChip: TextView = itemView.findViewById(R.id.tvLoanChip)
        val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        val tvLastUpdated: TextView = itemView.findViewById(R.id.tvLastUpdated)
        val tvDepreciation: TextView = itemView.findViewById(R.id.tvDepreciation)
        val tvPurchasePrice: TextView = itemView.findViewById(R.id.tvPurchasePrice)
        val tvPurchaseDate: TextView = itemView.findViewById(R.id.tvPurchaseDate)
        val tvCagr: TextView = itemView.findViewById(R.id.tvCagr)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_other_asset_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asset = getItem(position)
        val ctx = holder.itemView.context

        // Icon + label
        holder.tvAssetIcon.text = if (asset.assetType == "real_estate") "🏠" else "🚗"
        holder.tvLabel.text = asset.label
        holder.tvAssetTypeBadge.text = if (asset.assetType == "real_estate") "Home / Property" else "Car / Vehicle"

        // Loan chip
        holder.tvLoanChip.visibility = if (asset.hasActiveLoan) View.VISIBLE else View.GONE

        // Current value
        val currentValue = PhysicalAssetCagrCalculator.getAssetCurrentValue(asset)
        holder.tvCurrentValue.text = currencyFormat.format(currentValue)

        // Asset-type-specific info
        if (asset.assetType == "real_estate") {
            holder.tvLastUpdated.visibility = View.VISIBLE
            holder.tvDepreciation.visibility = View.GONE
            val lastUpdated = asset.marketValueLastUpdated?.let { dateStr ->
                try {
                    // Parse ISO timestamp
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = isoFormat.parse(dateStr.take(19)) ?: return@let null
                    "Last updated: ${displayDateFormat.format(date)}"
                } catch (e: Exception) { null }
            } ?: "Last updated: --"
            holder.tvLastUpdated.text = lastUpdated
        } else {
            holder.tvLastUpdated.visibility = View.GONE
            holder.tvDepreciation.visibility = View.VISIBLE
            val deprRate = asset.depreciationRatePct ?: 10.0
            holder.tvDepreciation.text = "Depreciation: -${deprRate.toInt()}%/yr"
        }

        // Purchase info
        holder.tvPurchasePrice.text = currencyFormat.format(asset.purchasePrice)
        holder.tvPurchaseDate.text = try {
            val date = inputDateFormat.parse(asset.purchaseDate)
            if (date != null) displayDateFormat.format(date) else asset.purchaseDate
        } catch (e: Exception) { asset.purchaseDate }

        // CAGR
        val cagr = PhysicalAssetCagrCalculator.getAssetCagr(asset)
        val cagrPct = cagr * 100
        val cagrSign = if (cagrPct >= 0) "+" else ""
        holder.tvCagr.text = "${cagrSign}${"%.1f".format(cagrPct)}% CAGR"
        holder.tvCagr.setTextColor(
            ContextCompat.getColor(
                ctx,
                if (cagrPct >= 0) R.color.income_green else R.color.expense_red
            )
        )

        // Long press
        holder.itemView.setOnLongClickListener {
            onLongPress(asset)
            true
        }
    }

    private fun formatCompact(amount: Double): String = when {
        amount >= 1_00_00_000 -> "₹${"%.2f".format(amount / 1_00_00_000)}Cr"
        amount >= 1_00_000 -> "₹${"%.2f".format(amount / 1_00_000)}L"
        else -> currencyFormat.format(amount)
    }
}
