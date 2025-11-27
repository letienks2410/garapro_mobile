package com.example.garapro.data.model

//data class otpRequest(val phoneNumber: String)
//data class otpVerifyRequest(val phoneNumber: String, val otp: String)
//data class otpResponse(val message: String, val resetToken: String? = null)

data class ResetPasswordRequest(
    val phoneNumber: String,
    val resetToken: String,
    val newPassword: String
)

data class ResetPasswordResponse(val message: String)
