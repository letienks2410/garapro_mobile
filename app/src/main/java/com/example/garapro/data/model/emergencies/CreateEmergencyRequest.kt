package com.example.garapro.data.model.emergencies

data class CreateEmergencyRequest(
    val vehicleId: String,
    val branchId: String,
    val issueDescription: String,
    val latitude: Double,
    val longitude: Double
)