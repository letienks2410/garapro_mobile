package com.example.garapro.data.model.techEmergencies

data class EmergencyDetailDto(
    val emergencyRequestId: String,
    val customerName: String?,
    val customerPhone: String?,
    val branchName: String?,
    val branchAddress: String?,
    val vehiclePlate: String?,
    val vehicleName: String?,
    val issueDescription: String?,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val status: String?,
    val type: String?,
    val requestTime: String?,
    val estimatedCost: Double?,
    val distanceToGarageKm: Double?,
    val rejectReason: String?
)
