package com.example.garapro.data.repository

import com.example.garapro.data.model.*
import com.example.garapro.data.remote.ApiService
import com.example.garapro.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class ForgotRepository(private val api: ApiService) {

    fun sendOtp(phone: String): Flow<Resource<GenericResponse>> = flow {
        emit(Resource.Loading())
        try {
            val resp = api.sendOtpForgot(SendOtpRequest(phone))
            if (resp.isSuccessful && resp.body() != null) {
                emit(Resource.Success(resp.body()!!))
            } else {
                val msg = resp.errorBody()?.string() ?: resp.message()
                emit(Resource.Error(msg ?: "Gửi OTP thất bại"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Lỗi server: ${e.localizedMessage}"))
        } catch (e: IOException) {
            emit(Resource.Error("Lỗi kết nối: ${e.localizedMessage}"))
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi: ${e.localizedMessage}"))
        }
    }

    fun verifyOtp(phone: String, otp: String): Flow<Resource<GenericResponse>> = flow {
        emit(Resource.Loading())
        try {
            val resp = api.verifyOtpForgot(VerifyOtpRequest(phone, otp))
            if (resp.isSuccessful && resp.body() != null) {
                emit(Resource.Success(resp.body()!!))
            } else {
                val msg = resp.errorBody()?.string() ?: resp.message()
                emit(Resource.Error(msg ?: "Xác thực OTP thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi: ${e.localizedMessage}"))
        }
    }

    fun resetPassword(phone: String, token: String, newPassword: String): Flow<Resource<GenericResponse>> = flow {
        emit(Resource.Loading())
        try {
            val resp = api.resetPasswordForgot(ResetPasswordForgotRequest(phone, token, newPassword))
            if (resp.isSuccessful && resp.body() != null) {
                emit(Resource.Success(resp.body()!!))
            } else {
                val msg = resp.errorBody()?.string() ?: resp.message()
                emit(Resource.Error(msg ?: "Reset mật khẩu thất bại"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lỗi: ${e.localizedMessage}"))
        }
    }
}
