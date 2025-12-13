package com.example.garapro.data.remote

import com.example.garapro.data.model.Notification
import com.example.garapro.data.model.UnreadCountResponse
import retrofit2.Response
import retrofit2.http.*

interface NotificationApiService {

    // Lấy tất cả thông báo của user
    @GET("Notification")
    suspend fun getMyNotifications(): Response<List<Notification>>

    // Lấy thông báo chưa đọc
    @GET("Notification/unread")
    suspend fun getUnreadNotifications(): Response<List<Notification>>

    // Lấy số lượng thông báo chưa đọc
    @GET("Notification/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    // Đánh dấu một thông báo là đã đọc
    @PUT("Notification/{id}/read")
    suspend fun markAsRead(@Path("id") notificationId: String): Response<Unit>

    // Đánh dấu tất cả thông báo là đã đọc
    @PUT("Notification/read-all")
    suspend fun markAllAsRead(): Response<Unit>

    // Xóa một thông báo
    @DELETE("Notification/{id}")
    suspend fun deleteNotification(@Path("id") notificationId: String): Response<Unit>
}