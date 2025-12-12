package com.example.garapro.ui.TechEmergencies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.techEmergencies.EmergencyForTechnicianDto

class EmergencyForTechnicianAdapter(
    private val onItemClick: (EmergencyForTechnicianDto) -> Unit
) : RecyclerView.Adapter<EmergencyForTechnicianAdapter.EmergencyViewHolder>() {

    private val items = mutableListOf<EmergencyForTechnicianDto>()

    fun submitList(newList: List<EmergencyForTechnicianDto>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    inner class EmergencyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(item: EmergencyForTechnicianDto) {
            tvCustomerName.text = item.customerName ?: "Unknown"
            tvPhone.text = item.phoneNumber ?: "-"
            tvIssue.text = item.issueDescription

            tvStatus.text = item.emergencyStatus.name  // hoặc map ra text đẹp hơn

            tvTime.text = item.requestTime // có thể format lại nếu cần

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_for_technician, parent, false)
        return EmergencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmergencyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
