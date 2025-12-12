package com.example.garapro.ui.notifications

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.Notification
import com.example.garapro.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit,
    private val onMarkAsRead: (Notification) -> Unit,
    private val onDelete: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            // Set title và content
            binding.textViewTitle.text = extractTitle(notification.content)
            binding.textViewTime.text = formatTime(notification.timeSent)

            // Set màu border theo type (0 = Warning, 1 = Message)
            when (notification.type) {
                0 -> {
                    binding.cardView.strokeColor = Color.parseColor("#F44336") // Đỏ - Warning
                }
                1 -> {
                    binding.cardView.strokeColor = Color.parseColor("#4CAF50")
                }
                else -> {
                    binding.cardView.strokeColor = Color.parseColor("#2196F3")
                }
            }

            // Hiển thị chấm xanh nếu chưa đọc (0 = Unread)
            if (notification.isUnread()) {
                binding.viewUnreadIndicator.visibility = View.VISIBLE
            } else {
                binding.viewUnreadIndicator.visibility = View.GONE
            }

            // Click vào item để xem chi tiết
            binding.root.setOnClickListener {
                onNotificationClick(notification)
            }

            // Click vào chấm xanh để đánh dấu đã đọc
            binding.viewUnreadIndicator.setOnClickListener {
                onMarkAsRead(notification)
            }

            binding.buttonDelete.setOnClickListener {
                onDelete(notification)
            }
        }

        private fun extractTitle(content: String): String {
            // Lấy phần đầu của content làm title (tối đa 50 ký tự)
            return if (content.length > 50) {
                content.substring(0, 47) + "..."
            } else {
                content
            }
        }

        private fun formatTime(timeSent: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(timeSent)

                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                timeSent
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size
}