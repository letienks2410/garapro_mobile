package com.example.garapro.data.model.techEmergencies

data class UpdateEmergencyStatusRequest(
    val status: Int,
    val rejectReason: String? = null
)