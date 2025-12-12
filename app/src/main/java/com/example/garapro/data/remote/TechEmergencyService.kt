package com.example.garapro.data.remote

import com.example.garapro.data.model.techEmergencies.TechnicianEmergencyResponse
import com.example.garapro.data.model.techEmergencies.TechnicianLocationBody
import com.example.garapro.data.model.techEmergencies.UpdateEmergencyStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TechEmergencyService {
    @GET("TechnicianEmergency/technician/me")
    suspend fun getMyEmergencies(): Response<TechnicianEmergencyResponse>

    @POST("Technician/location/update")
    suspend fun updateLocation(
        @Body body: TechnicianLocationBody
    ): Response<Unit>



        @PUT("TechnicianEmergency/{id}/status")
        suspend fun updateStatus(
            @Path("id") id: String?,
            @Body body: UpdateEmergencyStatusRequest
        ): Response<Unit>


}