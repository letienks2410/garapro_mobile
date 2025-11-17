package com.example.garapro.hubs

import android.util.Log
import com.google.gson.JsonObject
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
class QuotationSignalRService(
    hubUrl: String // ví dụ: https://api.you.com/hubs/quotation
) {

    private val hub: HubConnection =
        HubConnectionBuilder.create(hubUrl).build()

    // Chỉ emit event "có gì đó mới" → Fragment tự load lại list
    private val _events = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<Unit> = _events

    fun setupListeners() {
        hub.on("QuotationCreated", { json: JsonObject ->
            Log.d("SignalR", "QuotationCreated payload: $json")

            // Nếu muốn, bạn có thể đọc thêm:
            // val quotationId = json.get("quotationId")?.asString
            // val userId = json.get("userId")?.asString

            _events.tryEmit(Unit) // chỉ báo có thay đổi
        }, JsonObject::class.java)
    }

    fun startAndJoinUser(userId: String) {
        when (hub.connectionState) {
            HubConnectionState.CONNECTED -> {
                joinUserGroup(userId)
            }
            HubConnectionState.DISCONNECTED -> {
                Log.d("SignalR", "QuotationHub starting connection...")
                hub.start().subscribe({
                    Log.d("SignalR", "QuotationHub connected")
                    joinUserGroup(userId)
                }, { error ->
                    Log.e("SignalR", "Error starting QuotationHub", error)
                })
            }
            else -> {
                Log.d("SignalR", "QuotationHub state=${hub.connectionState}")
            }
        }
    }

    private fun joinUserGroup(userId: String) {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            Log.d("SignalR", "Joining User_$userId on QuotationHub")
            hub.send("JoinUserGroup", userId)
        } else {
            Log.w("SignalR", "joinUserGroup called while not CONNECTED")
        }
    }

    fun stop() {
        if (hub.connectionState == HubConnectionState.CONNECTED) {
            hub.stop()
        }
    }
}