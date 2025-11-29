package com.example.garapro.data.model.emergencies

data class NearbyBranchDto(
    val branchId: String,
    val branchName: String,
    val phoneNumber: String,
    val address: String,
    val distanceKm: Double
)