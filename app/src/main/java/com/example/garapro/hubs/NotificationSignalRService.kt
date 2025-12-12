package com.example.garapro.hubs

import android.util.Log
import com.example.garapro.data.local.TokenManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class NotificationPayload(
    val notificationId: String,
    val type: String,
    val title: String,
    val content: String,
    val jobId: String? = null,
    val jobName: String? = null,
    val serviceName: String? = null,
    val inspectionId: String? = null,
    val customerConcern: String? = null,
    val repairOrderId: String? = null,
    val hoursRemaining: Int? = null,
    val hoursOverdue: Int? = null,
    val daysOverdue: Int? = null,
    val target: String,
    val timeSent: String,
    val status: String,
    val notificationType: String
)

class NotificationSignalRService(
    private val tokenManager: TokenManager,
    hubUrl: String
) {
    private val gson = Gson()

    private val hub: HubConnection = HubConnectionBuilder.create(hubUrl)
        .withAccessTokenProvider(Single.defer {
            Single.just(runBlocking { tokenManager.getAccessTokenSync() ?: "" })
        })
        .build()

    // Flow để emit notification payload
    private val _notificationFlow = MutableSharedFlow<NotificationPayload>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val notificationFlow: SharedFlow<NotificationPayload> = _notificationFlow

    // Flow để emit connection state
    private val _connectionStateFlow = MutableSharedFlow<Boolean>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val connectionStateFlow: SharedFlow<Boolean> = _connectionStateFlow

    init {
        setupListeners()
        setupConnectionHandlers()
    }

    private fun setupListeners() {
        hub.on("ReceiveNotification", { json: JsonObject ->
            Log.d(TAG, "ReceiveNotification payload: $json")
            handleNotification(json)
        }, JsonObject::class.java)
    }

    private fun setupConnectionHandlers() {
        hub.onClosed { error ->
            Log.e(TAG, "Connection closed: ${error?.message}")
            CoroutineScope(Dispatchers.Main).launch {
                _connectionStateFlow.emit(false)
            }
        }
    }

    private fun handleNotification(json: JsonObject) {
        try {
            val notification = NotificationPayload(
                notificationId = json.get("notificationId")?.asString ?: "",
                type = json.get("type")?.asString ?: "",
                title = json.get("title")?.asString ?: "",
                content = json.get("content")?.asString ?: "",
                jobId = json.get("jobId")?.asString,
                jobName = json.get("jobName")?.asString,
                serviceName = json.get("serviceName")?.asString,
                inspectionId = json.get("inspectionId")?.asString,
                customerConcern = json.get("customerConcern")?.asString,
                repairOrderId = json.get("repairOrderId")?.asString,
                hoursRemaining = json.get("hoursRemaining")?.asInt,
                hoursOverdue = json.get("hoursOverdue")?.asInt,
                daysOverdue = json.get("daysOverdue")?.asInt,
                target = json.get("target")?.asString ?: "",
                timeSent = json.get("timeSent")?.asString ?: "",
                status = json.get("status")?.asString ?: "",
                notificationType = json.get("notificationType")?.asString ?: ""
            )

            Log.d(TAG, "Parsed notification: ${notification.type} - ${notification.title}")
            _notificationFlow.tryEmit(notification)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notification: ${e.message}", e)
        }
    }

    fun start(onConnected: (() -> Unit)? = null) {
        when (hub.connectionState) {
            HubConnectionState.CONNECTED -> {
                Log.d(TAG, "NotificationHub already connected")
                onConnected?.invoke()
                joinMyGroup()
            }
            HubConnectionState.DISCONNECTED -> {
                Log.d(TAG, "NotificationHub starting connection...")
                hub.start().subscribe({
                    Log.d(TAG, "NotificationHub connected successfully")
                    CoroutineScope(Dispatchers.Main).launch {
                        _connectionStateFlow.emit(true)
                    }
                    onConnected?.invoke()
                    joinMyGroup()
                }, { error ->
                    Log.e(TAG, "Error starting NotificationHub", error)
                    CoroutineScope(Dispatchers.Main).launch {
                        _connectionStateFlow.emit(false)
                    }
                })
            }
            else -> {
                Log.d(TAG, "NotificationHub state=${hub.connectionState}")
            }
        }
    }

    private fun joinMyGroup() {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            try {
                hub.send("JoinMyGroup")
                Log.d(TAG, "Joined user notification group")
            } catch (e: Exception) {
                Log.e(TAG, "Error joining group: ${e.message}", e)
            }
        }
    }

    fun stop() {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.stop()
            Log.d(TAG, "NotificationHub stopped")
            CoroutineScope(Dispatchers.Main).launch {
                _connectionStateFlow.emit(false)
            }
        }
    }

    fun isConnected(): Boolean {
        return hub.connectionState == HubConnectionState.CONNECTED
    }

    companion object {
        private const val TAG = "NotificationSignalR"
    }
}