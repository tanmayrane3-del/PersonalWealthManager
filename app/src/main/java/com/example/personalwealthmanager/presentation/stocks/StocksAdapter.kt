package com.example.personalwealthmanager.presentation.stocks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val currencyFormat = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvQtyAvg: TextView = view.findViewById(R.id.tvQtyAvg)
        val tvSymbol: TextView = view.findViewById(R.id.tvSymbol)
        val tvPnlPercent: TextView = view.findViewById(R.id.tvPnlPercent)
        val tvPnlAmount: TextView = view.findViewById(R.id.tvPnlAmount)
        val tvInvested: TextView = view.findViewById(R.id.tvInvested)
        val tvLtp: TextView = view.findViewById(R.id.tvLtp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock_holding, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val h = holdings[position]

        val cagrSuffix = if (h.cagr1y != null)
            " • 1Y CAGR ${String.format("%.1f", h.cagr1y * 100)}%"
        else ""
        holder.tvQtyAvg.text = "Qty. ${h.quantity} • Avg. ${currencyFormat.format(h.averagePrice)}$cagrSuffix"
        holder.tvSymbol.text = "${h.tradingSymbol} · ${h.exchange}"

        val pnlSign = if (h.pnl >= 0) "+" else "-"
        val pnlColor = if (h.pnl >= 0)
            holder.itemView.context.getColor(R.color.amount_positive)
        else
            holder.itemView.context.getColor(R.color.amount_negative)

        holder.tvPnlPercent.text = "${pnlSign}${String.format("%.2f", abs(h.pnlPercentage))}%"
        holder.tvPnlPercent.setTextColor(pnlColor)

        holder.tvPnlAmount.text = "${pnlSign}₹${currencyFormat.format(abs(h.pnl))}"
        holder.tvPnlAmount.setTextColor(pnlColor)

        val invested = h.averagePrice * h.quantity
        holder.tvInvested.text = "Invested ${currencyFormat.format(invested)}"

        val daySign = if (h.dayChange >= 0) "+" else "-"
        holder.tvLtp.text = "LTP ${currencyFormat.format(h.lastPrice)} (${daySign}${currencyFormat.format(abs(h.dayChange))})"
    }

    override fun getItemCount(): Int = holdings.size

    fun updateHoldings(newHoldings: List<StockHolding>) {
        holdings = newHoldings
        notifyDataSetChanged()
    }
}
