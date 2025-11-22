package com.example.garapro.data.model

data class Message(
    val text: String,
    val isMe: Boolean   // true = tin nhắn của mình, false = của người kia
)
