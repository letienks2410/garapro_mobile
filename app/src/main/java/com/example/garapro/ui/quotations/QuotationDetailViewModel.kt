package com.example.garapro.ui.quotations

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.quotations.*
import com.example.garapro.data.repository.QuotationRepository
import com.example.garapro.databinding.FragmentQuotationDetailBinding
import kotlinx.coroutines.launch

class QuotationDetailViewModel(
private val repository: QuotationRepository
) : ViewModel() {

    private val _quotation = MutableLiveData<QuotationDetail?>()
    val quotation: LiveData<QuotationDetail?> = _quotation

    private val _isRejectMode = MutableLiveData<Boolean>()
    val isRejectMode: LiveData<Boolean> = _isRejectMode

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _submitSuccess = MutableLiveData(false)
    val submitSuccess: LiveData<Boolean> = _submitSuccess

    // Thêm LiveData để force update adapter khi cancel
    private val _refreshAdapter = MutableLiveData<Unit>()
    val refreshAdapter: LiveData<Unit> = _refreshAdapter

    private val _pendingServiceToggle = MutableLiveData<ServiceToggleEvent?>()
    val pendingServiceToggle: LiveData<ServiceToggleEvent?> = _pendingServiceToggle

    private val _customerNote = MutableLiveData<String>()
    val customerNote: LiveData<String> = _customerNote

    private val _hasUnselectedServices = MutableLiveData<Boolean>()
    val hasUnselectedServices: LiveData<Boolean> = _hasUnselectedServices

    private var pendingServiceId: String? = null

    // 🔹 STATE ƯU ĐÃI
    private val _servicePromotions =
        MutableLiveData<Map<String, ServicePromotionUiState>>(emptyMap())
    val servicePromotions: LiveData<Map<String, ServicePromotionUiState>> = _servicePromotions

    // 🔹 Event để Fragment mở dialog chọn ưu đãi
    private val _openPromotionDialog =
        MutableLiveData<CustomerPromotionResponse?>()
    val openPromotionDialog: LiveData<CustomerPromotionResponse?> = _openPromotionDialog

    // region data class & canSubmit
    data class ServiceToggleEvent(
        val serviceId: String,
        val serviceName: String,
        val currentChecked: Boolean
    )

    val canSubmit: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(_quotation) {
            updateCanSubmit()
            _hasUnselectedServices.value =
                it?.quotationServices?.any { service -> !service.isSelected } == true
        }
        addSource(_customerNote) { updateCanSubmit() }
        addSource(_hasUnselectedServices) { updateCanSubmit() }
    }
    // endregion

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadQuotation(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getQuotationDetailById(id)
                .onSuccess { quotation ->
                    _quotation.value = quotation
                    loadCustomerNoteFromQuotation(quotation)
                }
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateCustomerNote(note: String) {
        _customerNote.value = note
    }

    private fun loadCustomerNoteFromQuotation(quotation: QuotationDetail) {
        val note = quotation.customerNote ?: ""
        _customerNote.value = note
        Log.d("quotation note load", note)

        val hasUnselected = quotation.quotationServices.any { !it.isSelected }
        _hasUnselectedServices.value = hasUnselected
    }

    private fun updateCanSubmit() {
        val quotation = _quotation.value
        val note = _customerNote.value
        val hasUnselected = _hasUnselectedServices.value == true

        val hasSelectedServices = quotation?.quotationServices?.any { it.isSelected } == true
        val hasNote = !note.isNullOrBlank()
        val hasValidNote = hasNote && note.length >= 10

        // - Approve: có service được chọn
        // - Reject: có note hợp lệ
        val canSubmitValue = hasSelectedServices || hasValidNote
        (canSubmit as MediatorLiveData).value = canSubmitValue

        _isRejectMode.value = !hasSelectedServices
    }

    // region Service selection / parts

    fun onServiceCheckChanged(serviceId: String, isChecked: Boolean) {
        val service = _quotation.value?.quotationServices?.find { it.quotationServiceId == serviceId } ?: return

        if (!isChecked && service.isSelected) {
            pendingServiceId = serviceId
            _pendingServiceToggle.value = ServiceToggleEvent(serviceId, service.serviceName, isChecked)
        } else {
            updateServiceSelection(serviceId, isChecked)
        }
    }

    fun confirmServiceToggle(serviceId: String, isSelected: Boolean) {
        updateServiceSelection(serviceId, isSelected)
        _pendingServiceToggle.value = null
    }

    fun clearPendingState() {
        _pendingServiceToggle.value = null
        pendingServiceId = null
    }

    fun cancelServiceToggle() {
        _pendingServiceToggle.value = null
        pendingServiceId = null
        _refreshAdapter.value = Unit
    }

    private fun updateServiceSelection(serviceId: String, isSelected: Boolean) {
        val current = _quotation.value ?: return
        val updatedServices = current.quotationServices.map {
            if (it.quotationServiceId == serviceId) it.copy(isSelected = isSelected) else it
        }
        _quotation.value = current.copy(quotationServices = updatedServices)
        Log.d("quotation", updatedServices.count().toString())

        val hasUnselected = updatedServices.any { !it.isSelected }
        _hasUnselectedServices.value = hasUnselected
    }

    fun togglePartSelection(serviceId: String, partCategoryId: String, partId: String) {
        val currentQuotation = _quotation.value ?: return

        val updatedServices = currentQuotation.quotationServices.map { service ->
            if (service.quotationServiceId == serviceId) {
                val updatedCategories = if (service.isAdvanced) {
                    handleAdvancedSelection(service, partCategoryId, partId)
                } else {
                    handleNonAdvancedSelection(service, partCategoryId, partId)
                }
                service.copy(partCategories = updatedCategories)
            } else {
                service
            }
        }

        _quotation.value = currentQuotation.copy(quotationServices = updatedServices)
        // Sau khi chọn part xong, nếu cần bạn có thể gọi lại tính tổng / update UI
    }

    private fun handleAdvancedSelection(
        service: QuotationServiceDetail,
        targetCategoryId: String,
        targetPartId: String
    ): List<PartCategory> {
        return service.partCategories.map { category ->
            if (category.partCategoryId == targetCategoryId) {
                val updatedParts = category.parts.map { part ->
                    part.copy(isSelected = part.quotationServicePartId == targetPartId)
                }
                category.copy(parts = updatedParts)
            } else {
                category
            }
        }
    }

    private fun handleNonAdvancedSelection(
        service: QuotationServiceDetail,
        targetCategoryId: String,
        targetPartId: String
    ): List<PartCategory> {
        val targetPart = service.partCategories
            .flatMap { it.parts }
            .find { it.quotationServicePartId == targetPartId }

        val shouldDeselectAll = targetPart?.isSelected == true

        val totalSelectedParts = service.partCategories.flatMap { it.parts }.count { it.isSelected }
        val canDeselect = totalSelectedParts > 1

        return service.partCategories.map { category ->
            val updatedParts = if (category.partCategoryId == targetCategoryId) {
                if (shouldDeselectAll && canDeselect) {
                    category.parts.map { part ->
                        if (part.quotationServicePartId == targetPartId) {
                            part.copy(isSelected = false)
                        } else {
                            part
                        }
                    }
                } else if (shouldDeselectAll && !canDeselect) {
                    category.parts.map { part ->
                        part.copy(isSelected = part.quotationServicePartId == targetPartId)
                    }
                } else {
                    category.parts.map { part ->
                        part.copy(isSelected = part.quotationServicePartId == targetPartId)
                    }
                }
            } else {
                category.parts.map { it.copy(isSelected = false) }
            }
            category.copy(parts = updatedParts)
        }
    }

    fun isServiceFullySelected(service: QuotationServiceDetail): Boolean {
        if (!service.isSelected) return false

        return if (service.isAdvanced) {
            service.partCategories.all { category ->
                category.parts.any { it.isSelected }
            }
        } else {
            service.partCategories.flatMap { it.parts }.count { it.isSelected } == 1
        }
    }

    fun validateQuotationSelection(): Boolean {
        val quotation = _quotation.value ?: return false

        val selectedServices = quotation.quotationServices.filter { it.isSelected }

        if (selectedServices.isEmpty()) {
            _errorMessage.value = "Vui lòng chọn ít nhất một service."
            return false
        }

        val incompleteServices = selectedServices.filterNot { isServiceFullySelected(it) }

        if (incompleteServices.isNotEmpty()) {
            _errorMessage.value = getValidationMessage()
            return false
        }

        return true
    }

    fun getValidationMessage(): String {
        val quotation = _quotation.value ?: return ""
        val selectedServices = quotation.quotationServices.filter { it.isSelected }

        if (selectedServices.isEmpty()) {
            return "Please select at least one service."
        }

        val incompleteServices = selectedServices.filterNot { isServiceFullySelected(it) }
        return if (incompleteServices.isNotEmpty()) {
            "The following services require selecting parts:\n" +
                    incompleteServices.joinToString("\n") { it.serviceName }
        } else {
            ""
        }
    }

    fun toggleServiceSelection(serviceId: String, currentCheckedState: Boolean) {
        val currentQuotation = _quotation.value ?: return

        val serviceToToggle = currentQuotation.quotationServices.find { it.quotationServiceId == serviceId }
            ?: return

        if (!currentCheckedState && serviceToToggle.isRequired) {
            _errorMessage.value = "Không thể bỏ chọn dịch vụ bắt buộc: ${serviceToToggle.serviceName}"
            return
        }

        if (!currentCheckedState && serviceToToggle.isSelected) {
            _pendingServiceToggle.value =
                ServiceToggleEvent(serviceId, serviceToToggle.serviceName, currentCheckedState)
        } else {
            updateServiceSelection(serviceId, currentCheckedState)
        }
    }

    // endregion

    // region ƯU ĐÃI

    /**
     * Tính currentOrderValue = giá service + parts đã chọn
     */
    fun calculateCurrentOrderValue(service: QuotationServiceDetail): Double {
        val partsTotal = service.partCategories
            .flatMap { it.parts }
            .filter { it.isSelected }
            .sumOf { it.price * it.quantity }

        return service.price + partsTotal
    }

    /**
     * Gọi API lấy ưu đãi cho 1 service.
     * Gửi currentOrderValue = service + parts đã chọn.
     */
    fun onPromotionClick(service: QuotationServiceDetail) {
        val currentValue = calculateCurrentOrderValue(service)

        viewModelScope.launch {
            try {
                repository.getCustomerPromotions(service.serviceId, currentValue)
                    .onSuccess { response ->
                        // Use currentValue as original price (service + parts)
                        val fixed = response.copy(
                            servicePrice = currentValue
                        )
                        _openPromotionDialog.value = fixed
                    }
                    .onFailure {
                        _errorMessage.value = it.message
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearOpenPromotionDialog() {
        _openPromotionDialog.value = null
    }

    /**
     * Áp dụng ưu đãi cho service
     * @param originalPrice = giá gốc (service + parts) tại thời điểm chọn ưu đãi
     */
    fun applyPromotion(
        serviceId: String,
        promotion: CustomerPromotion?,
        originalPrice: Double
    ) {
        val finalPrice = promotion?.finalPriceAfterDiscount ?: originalPrice

        val currentMap = _servicePromotions.value ?: emptyMap()
        val newMap = currentMap.toMutableMap()
        newMap[serviceId] = ServicePromotionUiState(
            serviceId = serviceId,
            originalPrice = originalPrice,
            selectedPromotion = promotion,
            finalPrice = finalPrice
        )
        _servicePromotions.value = newMap
    }

    /**
     * Dùng hàm này ở Fragment để tính tổng đã trừ ưu đãi.
     */
    fun getFinalPriceForService(service: QuotationServiceDetail): Double {
        // Nếu service này là “Good” thì không tính tiền luôn
        if (service.isGood) return 0.0

        val partsTotal = service.partCategories
            .flatMap { it.parts }
            .filter { it.isSelected }
            .sumOf { it.price * it.quantity}

        val priceWithParts = service.price + partsTotal

        val promoState = _servicePromotions.value?.get(service.serviceId)

        return when {
            promoState != null -> promoState.finalPrice
            service.finalPrice != null && service.finalPrice > 0.0 -> {
                service.finalPrice + partsTotal
            }
            else -> priceWithParts
        }
    }



    // endregion

    fun rejectQuotation(customerNote: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val quotation = _quotation.value ?: return@launch

            val request = CustomerResponseRequest(
                quotationId = quotation.quotationId,
                status = QuotationStatus.Rejected,
                customerNote = customerNote,
                selectedServices = emptyList()

            )

            repository.submitCustomerResponse(request)
                .onSuccess { _submitSuccess.value = true }
                .onFailure { _errorMessage.value = it.message }

            _isSubmitting.value = false
        }
    }

    fun getSubmitConfirmationType(): SubmitConfirmationType {
        val quotation = _quotation.value ?: return SubmitConfirmationType.REJECTED

        val hasUnselectedRequired = quotation.quotationServices.any { !it.isSelected && it.isRequired }

        return if (hasUnselectedRequired) {
            SubmitConfirmationType.REJECTED
        } else {
            SubmitConfirmationType.APPROVED
        }
    }

    fun submitCustomerResponse() {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val quotation = _quotation.value ?: run {
                _isSubmitting.value = false
                return@launch
            }

            // Map ưu đãi hiện tại theo serviceId (API serviceId, không phải quotationServiceId)
            val promotionMap = _servicePromotions.value.orEmpty()

            // Build selectedServices theo DTO mới
            val selectedServices = quotation.quotationServices
                .filter { it.isSelected }
                .map { service ->
                    // Lấy tất cả part đang được chọn trong service này
                    val selectedPartIds = service.partCategories
                        .flatMap { it.parts }
                        .filter { it.isSelected }
                        .map { it.quotationServicePartId }

                    // Lấy promotion (nếu có) dựa trên serviceId
                    val promoState = promotionMap[service.serviceId]
                    val appliedPromotionId = promoState?.selectedPromotion?.id ?: null

                    SelectedService(
                        quotationServiceId = service.quotationServiceId,
                        selectedPartIds = selectedPartIds,
                        appliedPromotionId = appliedPromotionId
                    )
                }

            

            val request = CustomerResponseRequest(
                quotationId = quotation.quotationId,
                status = QuotationStatus.Approved,
                customerNote = _customerNote.value,
                selectedServices = selectedServices
            )

            Log.d("quotation Services", selectedServices.toString())
            Log.d("quotation Request", request.toString())

            repository.submitCustomerResponse(request)
                .onSuccess { _submitSuccess.value = true }
                .onFailure { _errorMessage.value = it.message }

            _isSubmitting.value = false
        }
    }
}