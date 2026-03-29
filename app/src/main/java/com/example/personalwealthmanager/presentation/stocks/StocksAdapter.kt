package com.example.personalwealthmanager.presentation.stocks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.StockHolding
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class StocksAdapter(
    private var holdings: List<StockHolding>
) : RecyclerView.Adapter<StocksAdapter.ViewHolder>() {

    private val expanded = mutableSetOf<Int>()

    private val currencyFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val currencyFormatNoDecimal = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rowCollapsed: LinearLayout = view.findViewById(R.id.rowCollapsed)
        val tvSymbol: TextView        = view.findViewById(R.id.tvSymbol)
        val tvQtyLtp: TextView        = view.findViewById(R.id.tvQtyLtp)
        val tvPnlPercent: TextView    = view.findViewById(R.id.tvPnlPercent)
        val ivChevron: ImageView      = view.findViewById(R.id.ivChevron)
        val expandedDetail: LinearLayout = view.findViewById(R.id.expandedDetail)
        val tvInvested: TextView      = view.findViewById(R.id.tvInvested)
        val tvPnlAmount: TextView     = view.findViewById(R.id.tvPnlAmount)
        val tvCagr1y: TextView        = view.findViewById(R.id.tvCagr1y)
        val tvCagr3y: TextView        = view.findViewById(R.id.tvCagr3y)
        val tvCagr5y: TextView        = view.findViewById(R.id.tvCagr5y)
        val tvDayChange: TextView     = view.findViewById(R.id.tvDayChange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock_holding, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val h = holdings[position]
        val ctx = holder.itemView.context

        // ── Collapsed row ────────────────────────────────────────────────
        holder.tvSymbol.text = h.tradingSymbol
        holder.tvQtyLtp.text = "Qty: ${h.quantity} • LTP: ₹${currencyFormat.format(h.lastPrice)}"

        val pnlPos = h.pnlPercentage >= 0
        val pnlSign = if (pnlPos) "+" else ""
        val pnlColor = if (pnlPos) ctx.getColor(R.color.amount_positive) else ctx.getColor(R.color.amount_negative)
        holder.tvPnlPercent.text = "$pnlSign${String.format("%.2f", h.pnlPercentage)}%"
        holder.tvPnlPercent.setTextColor(pnlColor)

        // ── Expand/collapse toggle ────────────────────────────────────────
        val isExpanded = position in expanded
        holder.expandedDetail.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivChevron.rotation = if (isExpanded) 180f else 0f

        holder.rowCollapsed.setOnClickListener {
            if (position in expanded) expanded.remove(position) else expanded.add(position)
            notifyItemChanged(position)
        }

        // ── Expanded detail ───────────────────────────────────────────────
        if (isExpanded) {
            val invested = h.averagePrice * h.quantity
            holder.tvInvested.text = "₹${currencyFormat.format(invested)}"

            val pnlAmtSign = if (h.pnl >= 0) "+" else "-"
            holder.tvPnlAmount.text = "$pnlAmtSign₹${currencyFormat.format(abs(h.pnl))}"
            holder.tvPnlAmount.setTextColor(pnlColor)

            holder.tvCagr1y.text = formatCagrLabel("1Y", h.cagr1y, ctx)
            holder.tvCagr3y.text = formatCagrLabel("3Y", h.cagr3y, ctx)
            holder.tvCagr5y.text = formatCagrLabel("5Y", h.cagr5y, ctx)

            // Per-share day change × quantity for total today's Δ
            val dayTotal = h.dayChange * h.quantity
            val daySign = if (dayTotal >= 0) "+" else "-"
            val dayColor = if (dayTotal >= 0) ctx.getColor(R.color.amount_positive) else ctx.getColor(R.color.amount_negative)
            holder.tvDayChange.text = "$daySign₹${currencyFormat.format(abs(dayTotal))} today"
            holder.tvDayChange.setTextColor(dayColor)

            // Colour the CAGR labels
            colourCagrView(holder.tvCagr1y, h.cagr1y, ctx)
            colourCagrView(holder.tvCagr3y, h.cagr3y, ctx)
            colourCagrView(holder.tvCagr5y, h.cagr5y, ctx)
        }
    }

    override fun getItemCount(): Int = holdings.size

    fun updateHoldings(newHoldings: List<StockHolding>) {
        holdings = newHoldings
        notifyDataSetChanged()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun formatCagrLabel(period: String, cagr: Double?, ctx: android.content.Context): String {
        if (cagr == null) return "$period —"
        val pct = cagr * 100
        val sign = if (pct >= 0) "+" else ""
        return "$period $sign${String.format("%.1f", pct)}%"
    }

    private fun colourCagrView(tv: TextView, cagr: Double?, ctx: android.content.Context) {
        if (cagr == null) {
            tv.setTextColor(ctx.getColor(R.color.text_muted))
            return
        }
        tv.setTextColor(
            if (cagr >= 0) ctx.getColor(R.color.amount_positive) else ctx.getColor(R.color.amount_negative)
        )
    }
}
