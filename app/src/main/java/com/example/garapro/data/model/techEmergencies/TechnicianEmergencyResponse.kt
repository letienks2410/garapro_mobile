package com.example.garapro.data.model.techEmergencies

data class TechnicianEmergencyResponse(
    val current: EmergencyForTechnicianDto?,
    val list: List<EmergencyForTechnicianDto>
)