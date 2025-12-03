package com.example.garapro.data.model.emergencies

import com.google.gson.annotations.SerializedName

data class Emergency(
    @SerializedName(value = "id", alternate = ["emergencyRequestId", "EmergencyRequestId", "EmergenciesId", "EmergencyId"]) val id: String = "",
    @SerializedName(value = "userId", alternate = ["customerId", "UserId"]) val userId: String = "",
    @SerializedName(value = "latitude", alternate = ["Latitude"]) val latitude: Double = 0.0,
    @SerializedName(value = "longitude", alternate = ["Longitude"]) val longitude: Double = 0.0,
    @SerializedName(value = "timestamp", alternate = ["requestTime", "Timestamp"]) val timestamp: Long = 0L,
    @SerializedName(value = "status", alternate = ["Status"]) val status: EmergencyStatus = EmergencyStatus.PENDING,
    @SerializedName(value = "assignedGarageId", alternate = ["branchId", "BranchId", "GarageId"]) val assignedGarageId: String? = null
)

