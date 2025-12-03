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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.garapro.MainActivity
import com.example.garapro.R
import com.example.garapro.data.model.UpdateDeviceIdRequest
import com.example.garapro.data.model.emergencies.EmergencySoundPlayer
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.ui.TechEmergencies.IncomingEmergencyActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationService : FirebaseMessagingService() {

    private val CHANNEL_ID = "my_channel_id"
    private val CHANNEL_ID_EMERGENCY = "emergency_channel_v2"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        updateDeviceIdToServer(token)
        Log.d("DeviceToken", "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("Notification", "Message received: ${remoteMessage.data}")

        createNotificationChannel()

        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val data = remoteMessage.data

        Log.d("data ne",data.isEmpty().toString())
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
                val intent = Intent("com.example.garapro.NEW_CHAT_MESSAGE").apply {
                    putExtra("message", message)
                    putExtra("fromUserId", fromUserId)
                    putExtra("conversationId", conversationId)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                Log.d("Notification", "Broadcast sent to UI (foreground). message=$message")
            } else {
                if (canNotify) {
                    showSystemNotification(
                        title = data["title"] ?: "New message",
                        body = message,
                        extraIds = mapOf(
                            "conversationId" to (conversationId ?: ""),
                            "fromUserId" to (fromUserId ?: "")
                        )
                    )
                } else {
                    Log.w("Notification", "No POST_NOTIFICATIONS permission, cannot show chat notification")
                }
            }
            return
        }

        // =========================
// CASE: EMERGENCY
// =========================
        Log.d("type ne",type)
        if (type.equals("Emergency", ignoreCase = true)) {
            val title = data["title"] ?: "Emergency"
            val body = data["body"] ?: data["message"] ?: ""
            val screen = data["screen"] ?: "ReportsFragment"

//            EmergencySoundPlayer.start(this)

            if (isAppInForeground()) {
                val intent = Intent("com.example.garapro.EMERGENCY_EVENT").apply {
                    putExtra("title", title)
                    putExtra("body", body)
                    putExtra("screen", screen)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            } else {
                Log.d("typeEmer", "emerr")
                if (canNotify) {
                    showSystemNotification(
                        title = title,
                        body = body,
                        extraIds = mapOf(
                            "screen" to screen,
                            "notificationType" to "Emergency",
                            "from_notification" to "true"
                        ),
                        channelId = CHANNEL_ID_EMERGENCY,
                        emergency = true
                    )
                }
            }

            return
        }


        // =========================
        // CASE 2: Các loại notification khác
        // =========================
        val title = data["title"] ?: "Notification"
        val body = data["body"] ?: data["message"] ?: ""
        val screen = data["screen"]
        val action = data["action"]

        val allIds = extractAllIdsFromMessage(data)

        if (canNotify) {
            showSystemNotification(
                title = title,
                body = body,
                extraIds = allIds + mapOf(
                    "screen" to (screen ?: ""),
                    "type" to type,
                    "action" to (action ?: "")
                )
            )
        } else {
            Log.w("Notification", "Notification permission not granted, skip system notification")
        }
    }

    private fun showSystemNotification(
        title: String?,
        body: String?,
        extraIds: Map<String, String>,
        channelId: String = CHANNEL_ID,
        emergency: Boolean = false
    ) {
        // Intent mở MainActivity khi user bấm vào notification (khi không dùng full-screen)
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_notification", true)
            extraIds.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val notificationId = generateNotificationId(extraIds)

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (!body.isNullOrEmpty() && body.length > 50) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        if (emergency) {

            val fullScreenIntent = Intent(this, IncomingEmergencyActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

                putExtra("title", title ?: "Emergency")
                putExtra("body", body ?: "")
                putExtra("screen", extraIds["screen"] ?: "ReportsFragment")
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                notificationId + 1,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )


            builder
                .setContentIntent(fullScreenPendingIntent)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
            // Không cần addAction "OK" nữa, chạm vào noti là mở màn IncomingEmergencyActivity
        }

        NotificationManagerCompat.from(this)
            .notify(notificationId, builder.build())

        Log.d(
            "Notification",
            "System notification shown with ID: $notificationId, extras=$extraIds, channel=$channelId, emergency=$emergency"
        )
    }




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
            val notificationManager = getSystemService(NotificationManager::class.java)

            val name = "My Notifications"
            val descriptionText = "Thông báo từ app"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val emergencyChannel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Emergency Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency"
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(emergencyChannel)
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


