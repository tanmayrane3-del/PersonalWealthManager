package com.example.personalwealthmanager.presentation.mutualfunds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.data.remote.dto.CasPreviewFundDto

class CasImportPreviewAdapter : RecyclerView.Adapter<CasImportPreviewAdapter.VH>() {

    private val items = mutableListOf<CasPreviewFundDto>()
    private val checked = mutableSetOf<Int>()   // indices of selected items

    fun submitList(funds: List<CasPreviewFundDto>) {
        items.clear()
        items.addAll(funds)
        checked.clear()
        checked.addAll(funds.indices)            // all checked by default
        notifyDataSetChanged()
    }

    fun getSelectedFunds(): List<CasPreviewFundDto> =
        checked.sorted().map { items[it] }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cas_preview, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val fund = items[position]

        holder.tvSchemeName.text = fund.schemeName
        holder.tvIsin.text       = "${fund.isin}  ·  Folio: ${fund.folioNumber}"
        holder.tvUnits.text      = "Units: ${"%.3f".format(fund.closingUnits)}"
        holder.tvAmount.text     = "₹${"%.0f".format(fund.amountInvested)}"

        holder.tvLookupWarning.visibility =
            if (fund.lookupFailed) View.VISIBLE else View.GONE

        holder.cbInclude.setOnCheckedChangeListener(null)
        holder.cbInclude.isChecked = position in checked
        holder.cbInclude.setOnCheckedChangeListener { _, isChecked ->
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_ID.toInt()) {
                if (isChecked) checked.add(pos) else checked.remove(pos)
            }
        }

        holder.itemView.setOnClickListener { holder.cbInclude.toggle() }
    }

    override fun getItemCount() = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cbInclude      : CheckBox = view.findViewById(R.id.cbInclude)
        val tvSchemeName   : TextView = view.findViewById(R.id.tvPreviewSchemeName)
        val tvIsin         : TextView = view.findViewById(R.id.tvPreviewIsin)
        val tvUnits        : TextView = view.findViewById(R.id.tvPreviewUnits)
        val tvAmount       : TextView = view.findViewById(R.id.tvPreviewAmount)
        val tvLookupWarning: TextView = view.findViewById(R.id.tvLookupWarning)
    }
}
