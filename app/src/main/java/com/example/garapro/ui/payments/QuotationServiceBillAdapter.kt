package com.example.garapro.ui.payments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.payments.QuotationServiceDto
import com.example.garapro.utils.MoneyUtils

class QuotationServiceAdapter :
    RecyclerView.Adapter<QuotationServiceAdapter.ServiceViewHolder>() {

    private val items = mutableListOf<QuotationServiceDto>()

    fun submitList(data: List<QuotationServiceDto>) {
        items.clear()

        // Only show selected services
        items.addAll(data.filter { it.isSelected })

        notifyDataSetChanged()
    }

    inner class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvServiceName: TextView = view.findViewById(R.id.tvServiceName)
        val tvServicePrice: TextView = view.findViewById(R.id.tvServicePrice)

        val rvParts: RecyclerView = view.findViewById(R.id.rvParts)

        val partAdapter = QuotationPartAdapter()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val item = items[position]

        holder.tvServiceName.text = item.serviceName ?: "Service ${item.serviceId}"

        // Format price
        holder.tvServicePrice.text = "Service Price: ${
            MoneyUtils.formatVietnameseCurrency(item.price)
        }"

        // Flags
//        val flags = mutableListOf<String>()
//        if (item.isRequired) flags.add("Required")
//        if (item.isSelected) flags.add("Selected")
//
//        holder.tvFlags.text =
//            if (flags.isNotEmpty()) flags.joinToString(" • ")
//            else "Optional"

        // Setup parts adapter
        holder.rvParts.apply {
            layoutManager = LinearLayoutManager(holder.itemView.context)
            adapter = holder.partAdapter
        }

        // Submit parts list
        holder.partAdapter.submitList(item.parts)
    }

    override fun getItemCount() = items.size
}
