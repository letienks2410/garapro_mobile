package com.example.garapro.data.model.quotations

import java.io.Serializable

data class CustomerPromotionResponse(
    val serviceId: String,
    val serviceName: String,
    val servicePrice: Double,
    val promotions: List<CustomerPromotion>,
    val bestPromotion: CustomerPromotion?
)

data class CustomerPromotion(
    val id: String,
    val name: String,
    val description: String,
    val type: Int,
    val discountType: Int,
    val discountValue: Double,
    val startDate: String,
    val endDate: String,
    val isActive: Boolean,
    val minimumOrderValue: Double,
    val maximumDiscount: Double,
    val usageLimit: Int,
    val usedCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val calculatedDiscount: Double,
    val isEligible: Boolean,
    val discountDisplayText: String,
    val eligibilityMessage: String,
    val finalPriceAfterDiscount: Double
): Serializable
