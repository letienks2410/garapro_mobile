package com.example.garapro.data.model.techEmergencies



data class EmergencyForTechnicianDto(
    val emergencyRequestId: String,
    val customerId: String,
    val branchId: String,
    val vehicleId: String,
    val issueDescription: String,
    val branchName :String,
    val branchLatitude : Double,
    val branchLongitude : Double,
    val latitude: Double,
    val longitude: Double,
    val requestTime: String,
    val status: Int,
    val customerName: String?,
    val phoneNumber: String?
) {
    val emergencyStatus: EmergencyStatus
        get() = EmergencyStatus.fromInt(status)
}
enum class EmergencyStatus(val value: Int) {
    Pending(0),
    Accepted(1),
    Assigned(2),
    InProgress(3),
    Towing(4),
    Completed(5),
    Canceled(6);

    companion object {
        fun fromInt(value: Int): EmergencyStatus {
            return values().firstOrNull { it.value == value } ?: Pending
        }
    }
}

data class TechnicianLocationBody(
    val latitude: Double,
    val longitude: Double
)
