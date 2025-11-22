package com.example.garapro.data.model.payments

data class RepairOrderPaymentDto(
    val repairOrderId: String,
    val receiveDate: String,
    val cost: Double,
    val paidStatus: Int, // hoặc enum nếu bạn có
    val vehicle: VehiclePaymentDto,
    val approvedQuotations: List<ApprovedQuotationDto>
)

data class VehiclePaymentDto(
    val vehicleId: String,
    val licensePlate: String,
    val vin: String,
    val year: Int,
    val odometer: Long?,
    val brandName: String,
    val modelName: String
)

data class ApprovedQuotationDto(
    val quotationId: String,
    val createdAt: String,
    val totalAmount: Double,
    val discountAmount: Double,
    val netAmount: Double,
    val note: String?,
    val customerNote: String?,
    val services: List<QuotationServiceDto>
)

data class QuotationServiceDto(
    val quotationServiceId: String,
    val serviceId: String,
    val serviceName: String?,
    val isSelected: Boolean,
    val isRequired: Boolean,
    val price: Double,
    val discountValue: Double?,
    val finalPrice: Double?,
    val parts: List<QuotationServicePartDto>
)

data class QuotationServicePartDto(
    val quotationServicePartId: String,
    val partId: String,
    val partName: String,
    val price: Double,
    val isSelected: Boolean,
    val quantity: Double
)