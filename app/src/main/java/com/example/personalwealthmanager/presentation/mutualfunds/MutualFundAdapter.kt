package com.example.personalwealthmanager.presentation.mutualfunds

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.MutualFundHolding
import com.example.personalwealthmanager.domain.model.MutualFundLot
import java.text.NumberFormat
import java.util.Locale

class MutualFundAdapter(
    private val onToggleExpand: (String) -> Unit,
    private val onDeleteLot: (MutualFundLot) -> Unit,
    private val onDeleteFund: (MutualFundHolding) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class MfListItem {
        data class FundHeader(val fund: MutualFundHolding, val isExpanded: Boolean) : MfListItem()
        data class LotRow(val lot: MutualFundLot, val parentFund: MutualFundHolding) : MfListItem()
    }

    private val items = mutableListOf<MfListItem>()
    private val inrFmt = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())

    companion object {
        private const val VIEW_TYPE_FUND = 0
        private const val VIEW_TYPE_LOT  = 1
    }

    fun submitList(funds: List<MutualFundHolding>, expandedIsins: Set<String>) {
        val newItems = mutableListOf<MfListItem>()
        for (fund in funds) {
            val expanded = fund.isin in expandedIsins
            newItems.add(MfListItem.FundHeader(fund, expanded))
            if (expanded) {
                fund.lots.forEach { newItems.add(MfListItem.LotRow(it, fund)) }
            }
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(o: Int, n: Int): Boolean {
                val old = items[o]; val new = newItems[n]
                return when {
                    old is MfListItem.FundHeader && new is MfListItem.FundHeader -> old.fund.isin == new.fund.isin
                    old is MfListItem.LotRow && new is MfListItem.LotRow -> old.lot.id == new.lot.id
                    else -> false
                }
            }
            override fun areContentsTheSame(o: Int, n: Int) = items[o] == newItems[n]
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is MfListItem.FundHeader -> VIEW_TYPE_FUND
        is MfListItem.LotRow     -> VIEW_TYPE_LOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_FUND -> FundVH(inflater.inflate(R.layout.item_mutual_fund, parent, false))
            else           -> LotVH(inflater.inflate(R.layout.item_mf_lot, parent, false))
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MfListItem.FundHeader -> (holder as FundVH).bind(item.fund, item.isExpanded)
            is MfListItem.LotRow     -> (holder as LotVH).bind(item.lot, item.parentFund)
        }
    }

    inner class FundVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSchemeName    = itemView.findViewById<TextView>(R.id.tvSchemeName)
        private val tvAmcName       = itemView.findViewById<TextView>(R.id.tvAmcName)
        private val tvTotalUnits    = itemView.findViewById<TextView>(R.id.tvTotalUnits)
        private val tvAvgNav        = itemView.findViewById<TextView>(R.id.tvAvgNav)
        private val tvCurrentNav    = itemView.findViewById<TextView>(R.id.tvCurrentNav)
        private val tvCurrentValue  = itemView.findViewById<TextView>(R.id.tvCurrentValue)
        private val tvAbsReturn     = itemView.findViewById<TextView>(R.id.tvAbsReturn)
        private val tvXirr          = itemView.findViewById<TextView>(R.id.tvXirr)
        private val ivExpandChevron = itemView.findViewById<ImageView>(R.id.ivExpandChevron)

        fun bind(fund: MutualFundHolding, isExpanded: Boolean) {
            tvSchemeName.text   = fund.schemeName
            tvAmcName.text      = fund.amcName ?: ""
            tvTotalUnits.text   = "%.3f units".format(fund.totalUnits)
            tvAvgNav.text       = "Avg NAV: ₹%.4f".format(fund.avgNav)
            tvCurrentNav.text   = fund.latestNav?.let { "NAV: ₹%.4f".format(it) } ?: "NAV: --"
            tvCurrentValue.text = formatCompact(fund.currentValue)

            val returnColor = if (fund.absoluteReturn >= 0)
                ContextCompat.getColor(itemView.context, R.color.amount_positive)
            else
                ContextCompat.getColor(itemView.context, R.color.amount_negative)

            tvAbsReturn.text      = "%s (%.2f%%)".format(formatCompact(fund.absoluteReturn), fund.absoluteReturnPct)
            tvAbsReturn.setTextColor(returnColor)

            if (fund.xirr != null) {
                tvXirr.text = "XIRR: %.1f%%".format(fund.xirr * 100)
                tvXirr.setTextColor(if (fund.xirr >= 0) returnColor else ContextCompat.getColor(itemView.context, R.color.amount_negative))
                tvXirr.visibility = View.VISIBLE
            } else {
                tvXirr.visibility = View.GONE
            }

            ivExpandChevron.setImageResource(
                if (isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
            )
            ivExpandChevron.visibility = if (fund.lots.size > 1) View.VISIBLE else View.GONE

            itemView.setOnClickListener { if (fund.lots.size > 1) onToggleExpand(fund.isin) }
            itemView.setOnLongClickListener { onDeleteFund(fund); true }
        }

        private fun formatCompact(amount: Double): String = when {
            amount >= 1_00_00_000 -> "₹${"%.2f".format(amount / 1_00_00_000)}Cr"
            amount >= 1_00_000    -> "₹${"%.2f".format(amount / 1_00_000)}L"
            else                  -> inrFmt.format(amount)
        }
    }

    inner class LotVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLotDate     = itemView.findViewById<TextView>(R.id.tvLotDate)
        private val tvLotUnits    = itemView.findViewById<TextView>(R.id.tvLotUnits)
        private val tvLotNav      = itemView.findViewById<TextView>(R.id.tvLotNav)
        private val tvLotInvested = itemView.findViewById<TextView>(R.id.tvLotInvested)

        fun bind(lot: MutualFundLot, parentFund: MutualFundHolding) {
            tvLotDate.text     = lot.purchaseDate
            tvLotUnits.text    = "%.3f units".format(lot.units)
            tvLotNav.text      = "@ ₹%.4f".format(lot.purchaseNav)
            tvLotInvested.text = inrFmt.format(lot.amountInvested)
            itemView.setOnLongClickListener { onDeleteLot(lot); true }
        }
    }
}
