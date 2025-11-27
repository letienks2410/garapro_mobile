package com.example.garapro.ui.login

import androidx.lifecycle.*
import com.example.garapro.data.model.GenericResponse
import com.example.garapro.data.repository.ForgotRepository
import com.example.garapro.utils.Resource
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(private val repo: ForgotRepository) : ViewModel() {

    private val _sendOtp = MutableLiveData<Resource<GenericResponse>>()
    val sendOtp: LiveData<Resource<GenericResponse>> = _sendOtp

    private val _verifyOtp = MutableLiveData<Resource<GenericResponse>>()
    val verifyOtp: LiveData<Resource<GenericResponse>> = _verifyOtp

    private val _resetPwd = MutableLiveData<Resource<GenericResponse>>()
    val resetPwd: LiveData<Resource<GenericResponse>> = _resetPwd

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            repo.sendOtp(phone).collect { _sendOtp.postValue(it) }
        }
    }

    fun verifyOtp(phone: String, otp: String) {
        viewModelScope.launch {
            repo.verifyOtp(phone, otp).collect { _verifyOtp.postValue(it) }
        }
    }

    fun resetPassword(phone: String, token: String, newPassword: String) {
        viewModelScope.launch {
            repo.resetPassword(phone, token, newPassword).collect { _resetPwd.postValue(it) }
        }
    }
}

class ForgotPasswordVMFactory(private val repo: ForgotRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ForgotPasswordViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
