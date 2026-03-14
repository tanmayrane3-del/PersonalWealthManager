package com.example.personalwealthmanager.presentation.metals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.databinding.ItemMetalHeaderBinding
import com.example.personalwealthmanager.databinding.ItemMetalHoldingBinding
import com.example.personalwealthmanager.domain.model.MetalHolding
import java.text.NumberFormat
import java.util.Locale

class MetalHoldingAdapter(
    private val onEdit: (MetalHolding) -> Unit,
    private val onDelete: (MetalHolding) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>() // String (header) or MetalHolding

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    fun submitGrouped(holdings: List<MetalHolding>) {
        items.clear()
        val grouped = holdings.groupBy { it.metalType }

        val typeOrder = listOf("physical_gold", "digital_gold", "sgb")
        for (type in typeOrder) {
            val group = grouped[type] ?: continue
            val headerLabel = when (type) {
                "physical_gold" -> "Physical Gold"
                "digital_gold" -> "Digital Gold"
                "sgb" -> "Sovereign Gold Bonds"
                else -> type
            }
            items.add(headerLabel)
            items.addAll(group)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderViewHolder(ItemMetalHeaderBinding.inflate(inflater, parent, false))
        } else {
            ItemViewHolder(ItemMetalHoldingBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.bind(items[position] as String)
        } else if (holder is ItemViewHolder) {
            holder.bind(items[position] as MetalHolding)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemMetalHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.tvSectionHeader.text = title
        }
    }

    inner class ItemViewHolder(private val binding: ItemMetalHoldingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(holding: MetalHolding) {
            binding.tvLabel.text = holding.label
            binding.tvSubType.text = holding.subType?.replaceFirstChar { it.uppercase() } ?: ""
            binding.tvSubType.visibility =
                if (holding.subType != null) android.view.View.VISIBLE else android.view.View.GONE

            binding.tvPurity.text = holding.purity.uppercase()
            binding.tvQuantity.text = String.format("%.2f g", holding.quantityGrams)
            binding.tvCurrentValue.text = formatInr(holding.currentValue)

            binding.root.setOnLongClickListener {
                showContextMenu(holding)
                true
            }
        }

        private fun showContextMenu(holding: MetalHolding) {
            val context = binding.root.context
            val popup = android.widget.PopupMenu(context, binding.root)
            popup.menu.add(0, 0, 0, "Edit")
            popup.menu.add(0, 1, 1, "Delete")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> onEdit(holding)
                    1 -> onDelete(holding)
                }
                true
            }
            popup.show()
        }

        private fun formatInr(amount: Double): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            return formatter.format(amount)
        }
    }
}
