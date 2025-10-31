package com.example.garapro.data.model.quotations

data class Quotation(
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
    val expiresAt: String?,
    val customerName: String? = "", // Cho phép null và có giá trị mặc định
    val vehicleInfo: String? = "",  // Cho phép null và có giá trị mặc định
    val quotationServices: List<QuotationService>,
    val quotationServiceParts: List<QuotationServicePart>?,
    val inspection: Inspection?,
    val repairOrder: RepairOrder?


) {

    fun getSafeVehicleInfo(): String {
        return vehicleInfo ?: "Không có thông tin xe"
    }

    // Thêm hàm helper để lấy customerName an toàn
    fun getSafeCustomerName(): String {
        return customerName ?: "Khách hàng"
    }
}

data class QuotationService(
    val quotationServiceId: String,
    val quotationId: String,
    val serviceId: String,
    var isSelected: Boolean,
    val price: Double,
    val quantity: Int,
    val totalPrice: Double,
    val createdAt: String,
    val serviceName: String,
    val serviceDescription: String?,
    val quotationServiceParts: List<QuotationServicePart>
)

data class QuotationServicePart(
    val quotationServicePartId: String,
    val quotationServiceId: String,
    val partId: String,
    var isSelected: Boolean,
    val isRecommended: Boolean,
    val recommendationNote: String?,
    val price: Double,
    val quantity: Int,
    val totalPrice: Double,
    val createdAt: String,
    val partName: String,
    val partDescription: String?
)
// Thêm các class mới
data class Inspection(
    val inspectionId: String,
    val vehicleId: String,
    val technicianId: String,
    val inspectionDate: String,
    val status: String,
    val notes: String?,
    val createdAt: String
)

data class RepairOrder(
    val repairOrderId: String,
    val vehicleId: String,
    val customerId: String,
    val technicianId: String,
    val status: String,
    val totalAmount: Double,
    val createdAt: String,
    val completedAt: String?
)
data class QuotationResponse(
    val data: List<Quotation>,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val totalPages: Int
)

data class CustomerResponseRequest(
    val quotationId: String,
    val status: QuotationStatus,
    val customerNote: String?,
    val selectedServices: List<SelectedService>,
    val selectedServiceParts: List<SelectedServicePart>
)

data class SelectedService(
    val quotationServiceId: String
)

data class SelectedServicePart(
    val quotationServicePartId: String
)