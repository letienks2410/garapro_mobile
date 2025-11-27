package com.example.garapro.ui.appointments

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.repairRequest.*
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout

import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.garapro.utils.MoneyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

// RepairRequestAdapter.kt
// RepairRequestAdapter.kt
class RepairRequestAdapter(
    private val onItemClick: (RepairRequest) -> Unit,
    private val onCancelClick: (RepairRequest) -> Unit
) : RecyclerView.Adapter<RepairRequestAdapter.ViewHolder>() {

    private val items = mutableListOf<RepairRequest>()
    private var vehicles = emptyList<Vehicle>()
    private var branches = emptyList<Branch>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvVehicle: TextView = itemView.findViewById(R.id.tvVehicle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvRequestDate: TextView = itemView.findViewById(R.id.tvRequestDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvBranch: TextView = itemView.findViewById(R.id.tvBranch)
        private val tvEstimatedCost: TextView = itemView.findViewById(R.id.tvEstimatedCost)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnCancel)
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)

        fun bind(item: RepairRequest) {
            val vehicle = vehicles.find { it.vehicleID == item.vehicleID }
            val branch = branches.find { it.branchId == item.branchId }

            tvVehicle.text = vehicle?.let {
                "${it.brandName} ${it.modelName} - ${it.licensePlate}"
            } ?: "Unknown Vehicle"

            tvDescription.text = item.description
            tvRequestDate.text = formatDate(item.requestDate)
            tvBranch.text = branch?.branchName ?: "Unknown Branch"
            tvEstimatedCost.visibility = View.GONE
            // tvEstimatedCost.text = "Estimated: ${MoneyUtils.formatVietnameseCurrency(item.estimatedCost)}"

            val statusText = getStatusText(item.status)
            tvStatus.text = statusText
            tvStatus.setBackgroundResource(getStatusBackground(item.status))

            // 🔹 Hiện nút Cancel nếu còn được phép cancel
            btnCancel.visibility = if (canShowCancelButton(item)) View.VISIBLE else View.GONE
            btnCancel.setOnClickListener { onCancelClick(item) }

            cardView.setOnClickListener { onItemClick(item) }
        }
        private fun canShowCancelButton(item: RepairRequest): Boolean {
            // Không show cho CANCELLED(3) hoặc COMPLETED(4)
            if (item.status == 3 || item.status == 4) return false

            // (Option) chỉ show cho PENDING(0) hoặc ACCEPTED(1)
            if (item.status != 0 && item.status != 1) return false


            return true
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                dateString
            }
        }

        private fun getStatusText(status: Int): String {
            return when (status) {
                0 -> "PENDING"
                1 -> "ACCEPTED"
                2 -> "ARRIVED"
                3 -> "CANCELLED"
                4 -> "COMPLETED"
                else -> "UNKNOWN"
            }

        }

        private fun getStatusBackground(status: Int): Int {
            return when (status) {
                0 -> R.drawable.bg_status_pending
                1 -> R.drawable.bg_status_accept
                2 -> R.drawable.bg_status_arrived
                3 -> R.drawable.bg_status_cancelled
                4 -> R.drawable.bg_status_arrived
                else -> R.drawable.bg_status_pending
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_repair_request_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<RepairRequest>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setVehicles(vehicleList: List<Vehicle>) {
        vehicles = vehicleList
        notifyDataSetChanged()
    }

    fun setBranches(branchList: List<Branch>) {
        branches = branchList
        notifyDataSetChanged()
    }
}