package com.example.garapro.ui.vehicles

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.Vehicles.Vehicle as VehicleItem

class VehicleAdapter(
    private val vehicles: List<VehicleItem>,
    private val onItemClick: (VehicleItem) -> Unit,
    private val onEditClick: (VehicleItem) -> Unit,
    private val onDeleteClick: (VehicleItem) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    class VehicleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVehicleName: TextView = view.findViewById(R.id.tvVehicleName)
        val tvLicensePlate: TextView = view.findViewById(R.id.tvLicensePlate)
        val tvDetailInfo: TextView = view.findViewById(R.id.tvDetailInfo)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vehicle, parent, false)
        return VehicleViewHolder(view)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        val vehicle = vehicles[position]

        holder.tvVehicleName.text = listOfNotNull(
            vehicle.brandName?.takeIf { it.isNotBlank() },
            vehicle.modelName?.takeIf { it.isNotBlank() }
        ).joinToString(separator = " ")

        holder.tvLicensePlate.text = vehicle.licensePlate ?: "-"

        val detailItems = listOfNotNull(
            vehicle.colorName?.takeIf { it.isNotBlank() },
            vehicle.year?.toString()
        ).filter { it.isNotBlank() }

        holder.tvDetailInfo.text = detailItems
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " • ")
            ?: "—"

        holder.itemView.setOnClickListener { onItemClick(vehicle) }
        holder.btnEdit.setOnClickListener { onEditClick(vehicle) }
        holder.btnDelete.setOnClickListener { onDeleteClick(vehicle) }
    }

    override fun getItemCount() = vehicles.size
}