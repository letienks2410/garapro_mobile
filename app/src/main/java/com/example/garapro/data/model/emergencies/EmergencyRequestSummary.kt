package com.example.garapro.data.model.emergencies

import com.google.gson.annotations.SerializedName

data class EmergencyRequestSummary(
    val emergencyRequestId: String,
    val vehicleName: String?,
    val issueDescription: String?,
    val emergencyType: String?,
    val requestTime: String?,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val mapUrl: String?,
    val responseDeadline: String?,
    val respondedAt: String?,
    val autoCanceledAt: String?,
    val distanceToGarageKm: Double?,
    val estimatedArrivalMinutes: Int?,
    val customerName: String?,
    val customerPhone: String?,
    val assignedTechnicianName: String?,
    @SerializedName(
        value = "assignedTechnicianPhone",
        alternate = [
            "AssignedTechnicianPhone",
            "TechnicianPhone",
            "technicianPhone",
            "PhoneNumberTechnician",
            "phoneNumberTechnician",
            "PhoneNumberTecnician",
            "phoneNumberTecnician"
        ]
    )
    val assignedTechnicianPhone: String?,
    val emergencyFee: Double?
)
