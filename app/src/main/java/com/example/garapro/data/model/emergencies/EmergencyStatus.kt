package com.example.garapro.data.model.emergencies

import com.google.gson.annotations.SerializedName

enum class EmergencyStatus {
    @SerializedName("Pending")
    PENDING,

    @SerializedName("Accepted")
    ACCEPTED,

    @SerializedName("InProgress")
    IN_PROGRESS,

    @SerializedName("Completed")
    COMPLETED,

    @SerializedName("Cancelled")
    CANCELLED
}
