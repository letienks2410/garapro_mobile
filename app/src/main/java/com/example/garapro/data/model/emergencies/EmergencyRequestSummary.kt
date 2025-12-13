package com.example.garapro.data.model.emergencies

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
    val emergencyFee: Double?
)
