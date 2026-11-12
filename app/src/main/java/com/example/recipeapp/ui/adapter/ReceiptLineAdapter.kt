package com.example.recipeapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.databinding.ItemReceiptLineBinding
import com.example.recipeapp.ml.ReceiptLine

class ReceiptLineAdapter(
    private var items: List<ReceiptLine>,
    private val onLineClick: (String) -> Unit
) : RecyclerView.Adapter<ReceiptLineAdapter.ReceiptViewHolder>() {

    inner class ReceiptViewHolder(private val binding: ItemReceiptLineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReceiptLine) {
            binding.tvReceiptLineText.text = item.text
            binding.tvAmountBadge.visibility = if (item.isAmount) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onLineClick(item.text)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReceiptViewHolder {
        val binding = ItemReceiptLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReceiptViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReceiptViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: List<ReceiptLine>) {
        items = newItems
        notifyDataSetChanged()
    }
}
