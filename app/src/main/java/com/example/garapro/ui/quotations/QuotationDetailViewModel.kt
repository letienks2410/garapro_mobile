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

    private val _quotation = MutableLiveData<Quotation?>()
    val quotation: LiveData<Quotation?> = _quotation

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

    val canSubmit: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(_quotation) {
            updateCanSubmit()
            _hasUnselectedServices.value =
                it?.quotationServices?.any { service -> !service.isSelected } == true
        }
        addSource(_customerNote) { updateCanSubmit() }
        addSource(_hasUnselectedServices) { updateCanSubmit() }
    }
    data class ServiceToggleEvent(
        val serviceId: String,
        val serviceName: String,
        val currentChecked: Boolean
    )
    fun clearError() {
        _errorMessage.value = null
    }
    fun loadQuotation(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getQuotationById(id)
                .onSuccess { quotation ->
                    _quotation.value = quotation

                    loadCustomerNoteFromQuotation(quotation)}
                .onFailure { _errorMessage.value = it.message }
            _isLoading.value = false
        }
    }
    fun updateCustomerNote(note: String) {
        _customerNote.value = note
    }
    private fun loadCustomerNoteFromQuotation(quotation: Quotation) {
        // Nếu quotation có customer note, load lên
        // (Giả sử quotation có field customerNote, nếu không có thì dùng field khác)
        val note = quotation.note ?: ""
        _customerNote.value = note
        Log.d("quotation note load", note);
        // Cập nhật trạng thái ban đầu
        val hasUnselected = quotation.quotationServices.any { !it.isSelected }
        _hasUnselectedServices.value = hasUnselected
    }
    private fun updateCanSubmit() {
        val quotation = _quotation.value
        val note = _customerNote.value
        val hasUnselected = _hasUnselectedServices.value == true

        val hasNote = !note.isNullOrBlank()
        val hasValidNote = hasNote && note.length >= 10 // KIỂM TRA ÍT NHẤT 10 KÝ TỰ


        // - KHÔNG có service nào bị bỏ chọn (chấp nhận tất cả) HOẶC
        // - Có service bị bỏ chọn VÀ có note hợp lệ (>=10 ký tự)
        val canSubmitValue = !hasUnselected || (hasUnselected && hasValidNote)
        (canSubmit as MediatorLiveData).value = canSubmitValue
    }

    fun onServiceCheckChanged(serviceId: String, isChecked: Boolean) {
        val service = _quotation.value?.quotationServices?.find { it.quotationServiceId == serviceId } ?: return

        if (!isChecked && service.isSelected) {
            // Hiển thị cảnh báo khi bỏ chọn
            pendingServiceId = serviceId

            _pendingServiceToggle.value = ServiceToggleEvent(serviceId, service.serviceName, isChecked)
        } else {
            // Áp dụng thay đổi ngay khi chọn
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
        Log.d("quotation",updatedServices.count().toString());
        // CẬP NHẬT: Kiểm tra có service nào bị bỏ chọn không
        val hasUnselected = updatedServices.any { !it.isSelected }
        _hasUnselectedServices.value = hasUnselected
    }

    fun getSubmitConfirmationType(): SubmitConfirmationType {
        val quotation = _quotation.value ?: return SubmitConfirmationType.REJECTED

        // LOGIC MỚI: Nếu có BẤT KỲ service nào bị bỏ chọn => TỪ CHỐI
        val hasUnselectedServices = quotation.quotationServices.any { !it.isSelected }

        return if (hasUnselectedServices) {
            SubmitConfirmationType.REJECTED
        } else {
            SubmitConfirmationType.APPROVED
        }
    }

    fun submitCustomerResponse() {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val quotation = _quotation.value ?: return@launch
            val selectedServices = quotation.quotationServices
                .filter { it.isSelected }
                .map { SelectedService(it.quotationServiceId) }

            Log.d("quotation",selectedServices.count().toString());

            val status = if (quotation.quotationServices.any{ !it.isSelected}) QuotationStatus.Rejected else QuotationStatus.Approved
            Log.d("quotation note", _customerNote.value.toString());

            repository.submitCustomerResponse(
                CustomerResponseRequest(
                    quotationId = quotation.quotationId,
                    status = status,
                    customerNote = _customerNote.value,
                    selectedServices = selectedServices,
                    selectedServiceParts = emptyList()
                )
            ).onSuccess { _submitSuccess.value = true }
                .onFailure { _errorMessage.value = it.message }

            _isSubmitting.value = false
        }
    }

}