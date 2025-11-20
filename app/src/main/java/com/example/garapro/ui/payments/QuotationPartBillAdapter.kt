package com.example.garapro.ui.payments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.payments.QuotationServicePartDto
import com.example.garapro.utils.MoneyUtils

class QuotationPartAdapter :
    RecyclerView.Adapter<QuotationPartAdapter.PartViewHolder>() {

    private val items = mutableListOf<QuotationServicePartDto>()

    fun submitList(data: List<QuotationServicePartDto>) {
        items.clear()

        // Only show selected parts
        items.addAll(data.filter { it.isSelected })

        notifyDataSetChanged()
    }

    inner class PartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPartName: TextView = view.findViewById(R.id.tvPartName)
//        val tvPartQuantity: TextView = view.findViewById(R.id.tvPartQuantity)
        val tvPartPrice: TextView = view.findViewById(R.id.tvPartPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_part, parent, false)
        return PartViewHolder(view)
    }

    override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
        val part = items[position]

        holder.tvPartName.text = part.partName +"(x${part.quantity.toInt()})" ?: "Unknown part"

//        holder.tvPartQuantity.text = "${part.quantity.toInt()}"

        holder.tvPartPrice.text = MoneyUtils.formatVietnameseCurrency(
            part.price * part.quantity
        )
    }

    override fun getItemCount() = items.size
}
