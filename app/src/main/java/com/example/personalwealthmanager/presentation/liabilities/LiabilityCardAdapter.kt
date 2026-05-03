package com.pwm.personalwealthmanager.presentation.liabilities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pwm.personalwealthmanager.R
import com.pwm.personalwealthmanager.domain.model.Liability
import java.text.NumberFormat
import java.util.Locale

class LiabilityCardAdapter(
    private val onClick: (Liability) -> Unit
) : ListAdapter<Liability, LiabilityCardAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Liability> = object : DiffUtil.ItemCallback<Liability>() {
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
        styleStatusChip(holder.tvStatusChip, liability.status)

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
        holder.tvOriginalAmount.text = currencyFormat.format(liability.originalAmount)

        // EMI
        holder.tvEmi.text = currencyFormat.format(liability.emiAmount)
        holder.tvInterestRate.text = "${liability.interestRate}% ${liability.interestType}"

        // EMI due day
        if (liability.emiDueDay != null) {
            holder.tvEmiDueDay.visibility = View.VISIBLE
            holder.tvEmiDueDay.text = "${liability.emiDueDay}${daySuffix(liability.emiDueDay)} of month"
        } else {
            holder.tvEmiDueDay.visibility = View.GONE
        }

        // Tap to edit
        holder.itemView.setOnClickListener {
            onClick(liability)
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

    private fun styleStatusChip(chip: TextView, status: String) {
        val (text, bgColor, textColor) = when (status) {
            "active"     -> Triple("Active",     0xFFC8E6C9.toInt(), 0xFF1B5E20.toInt())
            "closed"     -> Triple("Closed",     0xFFE0E0E0.toInt(), 0xFF424242.toInt())
            "foreclosed" -> Triple("Foreclosed", 0xFFFFDAD6.toInt(), 0xFFBA1A1A.toInt())
            else         -> Triple(status.replaceFirstChar { it.uppercase() }, 0xFFE0E0E0.toInt(), 0xFF424242.toInt())
        }
        chip.text = text
        chip.setTextColor(textColor)
        val bg = android.graphics.drawable.GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = chip.context.resources.displayMetrics.density * 12f
        }
        chip.background = bg
    }

    private fun daySuffix(day: Int): String = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
}
