package com.example.garapro.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Notification(
    @SerializedName("notificationID")
    val notificationID: String,
    @SerializedName("userID")
    val userID: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("type")
    val type: Int, // 0 = Message, 1 = Warning (từ enum C#)
    @SerializedName("status")
    val status: Int, // 0 = Unread, 1 = Read (từ enum C#)
    @SerializedName("target")
    val target: String,
    @SerializedName("timeSent")
    val timeSent: String
) : Serializable {
    fun getTypeString(): String {
        return when (type) {
            0 -> "Warning"  // C# enum: Warning = 0
            1 -> "Message"  // C# enum: Message = 1
            else -> "Unknown"
        }
    }
    fun getStatusString(): String {
        return when (status) {
            0 -> "Unread"
            1 -> "Read"     // C# enum: Read = 1
            else -> "Unknown"
        }
    }
    fun isUnread(): Boolean = status == 0

    fun isWarning(): Boolean = type == 0  // Warning = 0 trong C#
}
data class UnreadCountResponse(
    @SerializedName("unreadCount")
    val unreadCount: Int
)