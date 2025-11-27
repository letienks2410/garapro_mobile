package com.example.garapro.data.remote

import com.example.garapro.data.model.UpdateDeviceIdRequest
import com.example.garapro.data.model.otpRequest
import com.example.garapro.data.model.otpResponse
import com.example.garapro.data.model.otpVerifyRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthService {

    @PUT("users/device")
    suspend fun updateDeviceId(@Body request: UpdateDeviceIdRequest): Response<Unit>


}