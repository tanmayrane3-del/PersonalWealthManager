package com.example.personalwealthmanager.presentation.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.domain.model.Category

class CategoryAdapter(
    private var categories: List<Category>,
    private val showEditButton: Boolean,
    private val onEditClick: ((Category) -> Unit)? = null
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
        val tvCategoryDescription: TextView = view.findViewById(R.id.tvCategoryDescription)
        val btnEditCategory: ImageView = view.findViewById(R.id.btnEditCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        // Set category icon (emoji) or default
        holder.tvCategoryIcon.text = category.icon ?: "📁"

        // Set category name
        holder.tvCategoryName.text = category.name

        // Set description if available
        if (!category.description.isNullOrBlank()) {
            holder.tvCategoryDescription.text = category.description
            holder.tvCategoryDescription.visibility = View.VISIBLE
        } else {
            holder.tvCategoryDescription.visibility = View.GONE
        }

        // Show edit button only for user-specific categories
        if (showEditButton && category.isUserSpecific) {
            holder.btnEditCategory.visibility = View.VISIBLE
            holder.btnEditCategory.setOnClickListener {
                onEditClick?.invoke(category)
            }
        } else {
            holder.btnEditCategory.visibility = View.GONE
        }
    }

    override fun getItemCount() = categories.size

    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
}
