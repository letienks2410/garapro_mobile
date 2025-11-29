package com.example.garapro.ui.RepairProgress.archived

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.data.model.RepairProgresses.ArchivedTechnician
import com.example.garapro.databinding.ItemArchivedJobBinding
import com.example.garapro.databinding.ItemArchivedJobTechnicianBinding


class ArchivedJobTechnicianAdapter(
    private val items: List<ArchivedTechnician>
) : RecyclerView.Adapter<ArchivedJobTechnicianAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemArchivedJobTechnicianBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ArchivedTechnician) = with(binding) {
            Log.d("techname",item.fullName );

            tvTechName.text = item.fullName?: "nAn"

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemArchivedJobTechnicianBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
