package com.example.garapro.services

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.garapro.MainActivity
import com.example.garapro.R
import com.example.garapro.data.model.UpdateDeviceIdRequest
import com.example.garapro.data.remote.RetrofitInstance
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : FirebaseMessagingService() {

    private val CHANNEL_ID = "my_channel_id"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        updateDeviceIdToServer(token)
        Log.d("DeviceToken", "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("Notification", "Message received: ${remoteMessage.data}")

        // 1) Tạo channel nếu cần
        createNotificationChannel()

        // 2) Check permission Android 13+
        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val data = remoteMessage.data
        if (data.isEmpty()) return

        val type = data["type"] ?: ""

        // =========================
        // CASE 1: Chat message
        // =========================
        if (type == "chat_message") {
            val message = data["message"] ?: ""
            val fromUserId = data["fromUserId"]
            val conversationId = data["conversationId"]

            if (isAppInForeground()) {
                // App đang mở -> đẩy vào UI qua broadcast, không show system notification
                val intent = Intent("com.example.garapro.NEW_CHAT_MESSAGE").apply {
                    putExtra("message", message)
                    putExtra("fromUserId", fromUserId)
                    putExtra("conversationId", conversationId)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                Log.d("Notification", "Broadcast sent to UI (foreground). message=$message")
                return
            } else {
                // App background -> show system notification nếu có permission
                if (canNotify) {
                    showSystemNotification(
                        title = data["title"] ?: "Tin nhắn mới",
                        body = message,
                        extraIds = mapOf(
                            "conversationId" to (conversationId ?: ""),
                            "fromUserId" to (fromUserId ?: "")
                        )
                    )
                } else {
                    Log.w("Notification", "No POST_NOTIFICATIONS permission, cannot show chat notification")
                }
                return
            }
        }

        // =========================
        // CASE 2: Các loại notification khác
        // =========================
        val title = data["title"] ?: "Thông báo"
        val body = data["body"] ?: data["message"] ?: ""
        val screen = data["screen"]
        val notificationType = data["type"]
        val action = data["action"]

        // Lấy tất cả ID (repairRequestId, orderId, quotationId, conversationId, fromUserId, ...)
        val allIds = extractAllIdsFromMessage(data)

        if (canNotify) {
            showSystemNotification(
                title = title,
                body = body,
                extraIds = allIds + mapOf(
                    "screen" to (screen ?: ""),
                    "type" to (notificationType ?: ""),
                    "action" to (action ?: "")
                )
            )
        } else {
            Log.w("Notification", "Notification permission not granted, skip system notification")
            // App đang foreground? nếu có logic broadcast riêng cho non-chat thì xử lý thêm ở đây (nếu cần)
        }
    }

    /**
     * Show system notification, mở MainActivity và truyền toàn bộ extraIds qua Intent
     */
    private fun showSystemNotification(
        title: String?,
        body: String?,
        extraIds: Map<String, String>
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)

            // Put all extras để MainActivity tự route
            extraIds.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val notificationId = generateNotificationId(extraIds)

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (!body.isNullOrEmpty() && body.length > 50) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        NotificationManagerCompat.from(this)
            .notify(notificationId, builder.build())

        Log.d("Notification", "System notification shown with ID: $notificationId, extras=$extraIds")
    }

    /**
     * Kiểm tra app đang foreground hay không
     */
    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = applicationContext.packageName

        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == packageName
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Lấy tất cả ID có thể có từ data
     */
    private fun extractAllIdsFromMessage(data: Map<String, String>): Map<String, String> {
        val idMap = mutableMapOf<String, String>()

        val possibleIdKeys = listOf(
            "repairRequestId", "repairOrderId", "orderId", "quotationId",
            "appointmentId", "serviceId", "technicianId", "customerId",
            "paymentId", "invoiceId", "chatId", "messageId",
            "quoteId", "estimateId", "taskId", "requestId",
            "conversationId", "fromUserId"
        )

        possibleIdKeys.forEach { key ->
            data[key]?.let { value ->
                if (value.isNotEmpty()) {
                    idMap[key] = value
                }
            }
        }
        return idMap
    }

    /**
     * Tạo notificationId dựa trên tất cả IDs -> đảm bảo cùng 1 "entity" sẽ có ID ổn định
     */
    private fun generateNotificationId(ids: Map<String, String>): Int {
        val combinedString = ids.entries
            .sortedBy { it.key }
            .joinToString("|") { "${it.key}=${it.value}" }

        return if (combinedString.isNotEmpty()) {
            combinedString.hashCode() and 0x7fffffff
        } else {
            System.currentTimeMillis().toInt() and 0x7fffffff
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Notifications"
            val descriptionText = "Thông báo từ app"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateDeviceIdToServer(deviceToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = UpdateDeviceIdRequest(deviceId = deviceToken)
                val response = RetrofitInstance.UserService.updateDeviceId(request)
                if (response.isSuccessful) {
                    Log.d("DeviceToken", "Device ID updated successfully")
                } else {
                    Log.e("DeviceToken", "Failed to update device ID: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("DeviceToken", "Error updating device ID: ${e.message}")
            }
        }
    }
}

