package com.example.garapro.data.model.quotations

data class QuotationDetail(
    val quotationId: String,
    val inspectionId: String?,
    val repairOrderId: String?,
    val userId: String,
    val vehicleId: String,
    val createdAt: String,
    val sentToCustomerAt: String?,
    val customerResponseAt: String?,
    val status: QuotationStatus,
    val totalAmount: Double,
    val discountAmount: Double,
    val note: String?,
    val isArchived : Boolean,
    val archivedAt : String?,
    val customerNote: String?,
    val expiresAt: String?,
    val customerName: String,
    val vehicleInfo: String?,
    val quotationServices: List<QuotationServiceDetail>,
    val inspection: Inspection?,
    val repairOrder: RepairOrder?
)

data class QuotationServiceDetail(
    val quotationServiceId: String,
    val quotationId: String,
    val serviceId: String,
    var isSelected: Boolean,
    val isAdvanced: Boolean,

    val isRequired: Boolean,
    val isGood : Boolean,
    val price: Double,
    val quantity: Int,

    val createdAt: String,
    val discountValue: Double?,            // số tiền giảm (hoặc 0 / null nếu không có)
    val finalPrice: Double?,               // giá sau giảm cho service
    val appliedPromotionId: String?,       // id ưu đãi áp dụng (nếu có)
    val appliedPromotion: AppliedPromotion?,
    val serviceName: String,
    val serviceDescription: String?,
    val partCategories: List<PartCategory>
)
data class AppliedPromotion(
    val id: String,
    val name: String,
    val description: String?
//    val type: Int,
//    val discountType: Int,
//    val discountValue: Double

)
data class PartCategory(
    val partCategoryId: String,
    val partCategoryName: String,
    val parts: List<QuotationServicePart>
)

