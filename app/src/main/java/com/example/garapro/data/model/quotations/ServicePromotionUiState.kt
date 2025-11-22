package com.example.garapro.data.model.quotations

data class ServicePromotionUiState(
    val serviceId: String,
    val originalPrice: Double,              // giá gốc: service + parts đã chọn
    val selectedPromotion: CustomerPromotion?,
    val finalPrice: Double                  // giá sau ưu đãi (nếu có), nếu không -> originalPrice
)
