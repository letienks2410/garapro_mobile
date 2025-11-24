package com.example.garapro.data.model.repairRequest

sealed class SubmitState {
    object Idle : SubmitState()
    object Loading : SubmitState()
    object Success : SubmitState()
    data class Error(val message: String?) : SubmitState()
}