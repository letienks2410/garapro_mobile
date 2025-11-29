package com.example.garapro.data.repository

import com.example.garapro.data.local.TokenManager
import com.example.garapro.data.model.ApiResponse
import com.example.garapro.data.model.ChangePasswordRequest
import com.example.garapro.data.model.ImageResponse
import com.example.garapro.data.model.User

import com.example.garapro.data.remote.ApiService
import com.example.garapro.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

class UserRepository(
    private val apiService: ApiService
) {

    fun getMe(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getMe()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Lỗi: ${response.message()}"))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Lỗi mạng"))
        } catch (e: HttpException) {
            emit(Resource.Error("Lỗi server: ${e.localizedMessage}"))
        }
    }

    suspend fun updateUser(user: User): Resource<User> {
        return try {
            val response = apiService.updateProfile(user)
            if (response.isSuccessful && response.body() != null)
                Resource.Success(response.body()!!)
            else
                Resource.Error("Lỗi cập nhật: ${response.message()}")
        } catch (e: Exception) {
            Resource.Error("Lỗi: ${e.localizedMessage}")
        }
    }

    suspend fun uploadImage(filePart: MultipartBody.Part): Resource<ImageResponse> {
        return try {
            val response = apiService.uploadImage(filePart)
            if (response.isSuccessful && response.body() != null)
                Resource.Success(response.body()!!)
            else
                Resource.Error("Upload thất bại: ${response.message()}")
        } catch (e: Exception) {
            Resource.Error("Lỗi: ${e.localizedMessage}")
        }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ): Resource<String> {
        return try {
            val request = ChangePasswordRequest(currentPassword, newPassword, confirmNewPassword)
            val response = apiService.changePassword(request)

            if (response.isSuccessful) {
                // parse success message
                val body = response.body()?.string() ?: "{}"
                val json = JSONObject(body)
                val msg = json.optString("message", "Password changed")
                Resource.Success(msg)
            } else {
                val errorBody = response.errorBody()?.string() ?: "{}"
                val json = JSONObject(errorBody)

                // check nếu server trả mảng errors
                val errorsArray = json.optJSONArray("errors")
                val errMsg = if (errorsArray != null && errorsArray.length() > 0) {
                    val list = mutableListOf<String>()
                    for (i in 0 until errorsArray.length()) {
                        list.add(errorsArray.getString(i))
                    }
                    list.joinToString("\n") // hiển thị mỗi lỗi trên 1 dòng
                } else {
                    json.optString("error", "Something went wrong")
                }

                Resource.Error(errMsg)
            }

        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

}
