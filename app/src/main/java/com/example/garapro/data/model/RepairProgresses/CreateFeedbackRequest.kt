package com.example.garapro.data.model.RepairProgresses

data class CreateFeedbackRequest(
    val description: String,
    val rating: Int,
    val repairOrderId: String
)

// Optional response DTO
data class CreateFeedbackResponse(
    val feedBackId: String?,
    val repairOrderId: String?,
    val rating: Int?,
    val description: String?,
    val createdAt: String?
)