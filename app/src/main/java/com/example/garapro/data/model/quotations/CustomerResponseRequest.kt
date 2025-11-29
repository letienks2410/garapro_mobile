package com.example.garapro.data.model.quotations

data class CustomerResponseRequest(
    val quotationId: String,
    val status: QuotationStatus,
    val customerNote: String?,
    val selectedServices: List<SelectedService>,

)
data class SelectedService(
    val quotationServiceId: String,
    val selectedPartIds: List<String>,
    val appliedPromotionId: String?
)