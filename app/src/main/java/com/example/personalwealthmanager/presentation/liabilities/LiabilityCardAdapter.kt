package com.example.personalwealthmanager.presentation.liabilities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.Liability
import java.text.NumberFormat
import java.util.Locale

class LiabilityCardAdapter(
    private val onLongPress: (Liability) -> Unit
) : ListAdapter<Liability, LiabilityCardAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Liability>() {
            override fun areItemsTheSame(old: Liability, new: Liability) = old.id == new.id
            override fun areContentsTheSame(old: Liability, new: Liability) = old == new
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLoanIcon: TextView = itemView.findViewById(R.id.tvLoanIcon)
        val tvLenderName: TextView = itemView.findViewById(R.id.tvLenderName)
        val tvLoanTypeBadge: TextView = itemView.findViewById(R.id.tvLoanTypeBadge)
        val tvStatusChip: TextView = itemView.findViewById(R.id.tvStatusChip)
        val tvAssetChip: TextView = itemView.findViewById(R.id.tvAssetChip)
        val tvOutstanding: TextView = itemView.findViewById(R.id.tvOutstanding)
        val tvOriginalAmount: TextView = itemView.findViewById(R.id.tvOriginalAmount)
        val tvEmi: TextView = itemView.findViewById(R.id.tvEmi)
        val tvInterestRate: TextView = itemView.findViewById(R.id.tvInterestRate)
        val tvEmiDueDay: TextView = itemView.findViewById(R.id.tvEmiDueDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_liability_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val liability = getItem(position)

        // Loan type icon + label
        holder.tvLoanIcon.text = loanIcon(liability.loanType)
        holder.tvLenderName.text = liability.lenderName
        holder.tvLoanTypeBadge.text = loanTypeLabel(liability.loanType)

        // Status chip
        val (statusText, statusColor) = statusStyle(liability.status)
        holder.tvStatusChip.text = statusText
        holder.tvStatusChip.setTextColor(statusColor)

        // Linked asset chip
        if (!liability.assetLabel.isNullOrBlank()) {
            holder.tvAssetChip.visibility = View.VISIBLE
            val assetIcon = if (liability.assetType == "real_estate") "🏠" else "🚗"
            holder.tvAssetChip.text = "$assetIcon Linked to: ${liability.assetLabel}"
        } else {
            holder.tvAssetChip.visibility = View.GONE
        }

        // Outstanding principal
        holder.tvOutstanding.text = currencyFormat.format(liability.outstandingPrincipal)
        holder.tvOriginalAmount.text = "of ${currencyFormat.format(liability.originalAmount)}"

        // EMI
        holder.tvEmi.text = currencyFormat.format(liability.emiAmount)
        holder.tvInterestRate.text = "${liability.interestRate}% ${liability.interestType}"

        // EMI due day
        if (liability.emiDueDay != null) {
            holder.tvEmiDueDay.visibility = View.VISIBLE
            holder.tvEmiDueDay.text = "Due: ${liability.emiDueDay}${daySuffix(liability.emiDueDay)} of month"
        } else {
            holder.tvEmiDueDay.visibility = View.GONE
        }

        // Long press
        holder.itemView.setOnLongClickListener {
            onLongPress(liability)
            true
        }
    }

    private fun loanIcon(loanType: String): String = when (loanType) {
        "home" -> "🏠"
        "car" -> "🚗"
        "personal" -> "👤"
        "education" -> "🎓"
        "business" -> "💼"
        else -> "💳"
    }

    private fun loanTypeLabel(loanType: String): String = when (loanType) {
        "home" -> "Home Loan"
        "car" -> "Car Loan"
        "personal" -> "Personal Loan"
        "education" -> "Education Loan"
        "business" -> "Business Loan"
        else -> "Other Loan"
    }

    private fun statusStyle(status: String): Pair<String, Int> = when (status) {
        "active" -> Pair("● Active", 0xFF4CAF50.toInt())
        "closed" -> Pair("● Closed", 0xFF9E9E9E.toInt())
        "foreclosed" -> Pair("● Foreclosed", 0xFFF44336.toInt())
        else -> Pair(status.replaceFirstChar { it.uppercase() }, 0xFFFFFFFF.toInt())
    }

    private fun daySuffix(day: Int): String = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
}
