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

        // 1) Create channel if needed
        createNotificationChannel()

        // 2) Permission check Android 13+
        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!canNotify) {
            Log.w("Notification", "Notification permission not granted")
            // still attempt to deliver to UI via broadcast if foreground
        }

        // 3) If data payload present, handle it
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: ""
            val message = data["message"] ?: ""
            val fromUserId = data["fromUserId"]
            val conversationId = data["conversationId"]

            // If chat message type -> prefer to send to UI when app is foreground
            if (type == "chat_message") {
                if (isAppInForeground()) {
                    // send broadcast so ChatFragment can update UI directly
                    val intent = Intent("com.example.garapro.NEW_CHAT_MESSAGE").apply {
                        putExtra("message", message)
                        putExtra("fromUserId", fromUserId)
                        putExtra("conversationId", conversationId)
                    }
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                    Log.d("Notification", "Broadcast sent to UI (foreground). message=$message")
                    return // do not show system notification when app foreground
                } else {
                    // App background -> show system notification (recommended for reliability)
                    showSystemNotification(
                        title = data["title"] ?: "Tin nhắn mới",
                        body = message,
                        conversationId = conversationId,
                        fromUserId = fromUserId
                    )
                    return
                }
            }

            // If other types, fallback to existing behavior: extract ids and show notification
            val title = data["title"] ?: "Thông báo"
            val body = data["body"] ?: message
            showSystemNotification(title, body, conversationId, fromUserId)
        }
    }

    private fun showSystemNotification(
        title: String?,
        body: String?,
        conversationId: String?,
        fromUserId: String?
    ) {
        // Build intent to open MainActivity (and pass ids so activity can open chat)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
            putExtra("conversationId", conversationId)
            putExtra("fromUserId", fromUserId)
        }

        val allIds = mutableMapOf<String, String>()
        conversationId?.let { allIds["conversationId"] = it }
        fromUserId?.let { allIds["fromUserId"] = it }

        val notificationId = generateNotificationId(allIds)

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

        if (body != null && body.length > 50) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        NotificationManagerCompat.from(this)
            .notify(notificationId, builder.build())

        Log.d("Notification", "System notification shown with ID: $notificationId")
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = applicationContext.packageName
        for (appProcess in appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && appProcess.processName == packageName) {
                return true
            }
        }
        return false
    }

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
