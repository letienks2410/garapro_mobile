package com.example.garapro.ui.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.local.TokenManager
import com.example.garapro.data.model.Message
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ChatFragment : Fragment() {

    private lateinit var adapter: MessageAdapter
    private lateinit var rv: RecyclerView
    private lateinit var edt: EditText
    private lateinit var btn: Button
    private lateinit var tokenManager: TokenManager
    private var fcmToken: String? = null
    private var userId: String? = null

    // Thay bằng webhook của bạn
    private val WEBHOOK_URL = "https://n8n.zanis.id.vn/webhook/b396fc96-535d-4af6-8f1a-33ea454b8bb2"
    private val AUTH_TOKEN: String? = null // nếu cần: "Bearer xxxxx"

    private val client = OkHttpClient()

    // BroadcastReceiver để nhận message từ NotificationService
    private val newMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message") ?: return
            val fromUserId = intent.getStringExtra("fromUserId")
            val conversationId = intent.getStringExtra("conversationId")

            // Nếu bạn có logic kiểm tra conversationId để chỉ insert vào chat đúng
            // hiện tại ta append trực tiếp (bạn có thể thêm điều kiện kiểm tra)
            Handler(Looper.getMainLooper()).post {
                adapter.addMessage(Message(message, false))
                rv.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rv = view.findViewById(R.id.rvMessages)
        edt = view.findViewById(R.id.edtMessage)
        btn = view.findViewById(R.id.btnSend)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = MessageAdapter(mutableListOf())
        rv.adapter = adapter

        tokenManager = TokenManager(requireContext())

        // Lấy userId sync (suspend) và fcm token
        lifecycleScope.launch {
            userId = tokenManager.getUserIdSync()
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
            }
        }

        btn.setOnClickListener {
            val text = edt.text.toString().trim()
            if (text.isNotEmpty()) {
                adapter.addMessage(Message(text, true))
                rv.scrollToPosition(adapter.itemCount - 1)
                edt.setText("")

                // Gọi hàm gửi, kèm userId + fcmToken
                sendMessageToWebhook(text,
                    userId = userId,
                    fcmToken = fcmToken,
                    onSuccess = { _responseBody ->
                        Handler(Looper.getMainLooper()).post {
                            adapter.addMessage(Message("Gửi đến n8n thành công", false))
                            rv.scrollToPosition(adapter.itemCount - 1)
                        }
                    },
                    onFailure = { err ->
                        Handler(Looper.getMainLooper()).post {
                            adapter.addMessage(Message("Gửi thất bại: $err", false))
                            rv.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(newMessageReceiver, IntentFilter("com.example.garapro.NEW_CHAT_MESSAGE"))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(newMessageReceiver)
        super.onPause()
    }

    private fun sendMessageToWebhook(
        text: String,
        userId: String?,
        fcmToken: String?,
        onSuccess: (String?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("text", text)
            put("source", "android_app")
            put("timestamp", System.currentTimeMillis())
            userId?.let { put("userId", it) }
            fcmToken?.let { put("fcmToken", it) }
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toString().toRequestBody(mediaType)

        val requestBuilder = Request.Builder()
            .url(WEBHOOK_URL)
            .post(body)
            .header("Accept", "application/json")

        AUTH_TOKEN?.let { token -> requestBuilder.header("Authorization", token) }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = onFailure(e.message ?: "Network error")
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) onFailure("HTTP ${it.code}: ${it.message}")
                    else onSuccess(it.body?.string())
                }
            }
        })
    }
}
