package com.example.garapro.ui.TechEmergencies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.garapro.R
import com.example.garapro.data.model.techEmergencies.EmergencyDetailDto
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EmergencyDetailBottomSheet(private val detail: EmergencyDetailDto) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.bottomsheet_emergency_detail, container, false)

        v.findViewById<TextView>(R.id.tvCustomer).text =
            "Customer: ${detail.customerName ?: "-"} - ${detail.customerPhone ?: "-"}"

        v.findViewById<TextView>(R.id.tvVehicle).text =
            "Vehicle: ${(detail.vehicleName ?: "-")} (${detail.vehiclePlate ?: "-"})"

        v.findViewById<TextView>(R.id.tvBranch).text =
            "Branch: ${detail.branchName ?: "-"}\n${detail.branchAddress ?: ""}"

        v.findViewById<TextView>(R.id.tvIssue).text =
            "Issue: ${detail.issueDescription ?: "-"}\nAddr: ${detail.address ?: "-"}"

        v.findViewById<TextView>(R.id.tvStatus).text =
            "Status: ${detail.status ?: "-"} | Type: ${detail.type ?: "-"}"

        return v
    }
}

