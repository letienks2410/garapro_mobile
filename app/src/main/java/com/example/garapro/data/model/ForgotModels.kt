package com.example.garapro.data.model

// Send OTP request -> { "phoneNumber": "..." }
data class SendOtpRequest(
    val phoneNumber: String
)

// Verify OTP request -> { "phoneNumber": "...", "token": "123456" }
data class VerifyOtpRequest(
    val phoneNumber: String,
    val token: String
)

// Reset password request -> { "phoneNumber": "...", "resetToken": "...", "newPassword": "..." }
data class ResetPasswordForgotRequest(
    val phoneNumber: String,
    val resetToken: String,
    val newPassword: String
)

// Generic response mapping backend you provided:
// success responses: { "message": "...", "resetToken": "..."? }
// error responses: { "error": "..." }
data class GenericResponse(
    val message: String? = null,
    val error: String? = null,
    val resetToken: String? = null
)
