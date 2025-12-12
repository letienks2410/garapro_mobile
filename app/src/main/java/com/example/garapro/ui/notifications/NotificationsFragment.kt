package com.example.garapro.ui.notifications
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.garapro.MainActivity
import com.example.garapro.data.local.TokenManager
import com.example.garapro.data.model.Notification
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.databinding.FragmentNotificationsBinding
import com.example.garapro.hubs.NotificationSignalRService
import com.example.garapro.utils.Constants
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var signalRService: NotificationSignalRService
    private lateinit var tokenManager: TokenManager

    private val notifications = mutableListOf<Notification>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container,  false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())
        signalRService = NotificationSignalRService(
            tokenManager,
            "${Constants.BASE_URL_SIGNALR}/notificationHub"
        )

        setupRecyclerView()
        setupSwipeRefresh()
        setupMarkAllReadButton()
        loadNotifications()
        connectToSignalR()
        observeRealTimeNotifications()
        loadUnreadCount()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notifications = notifications,
            onNotificationClick = { notification ->
                showNotificationDetail(notification)
            },
            onMarkAsRead = { notification ->
                markNotificationAsRead(notification)
            },
            onDelete = { notification ->
                showDeleteConfirmation(notification)
            }
        )

        binding.recyclerViewNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadNotifications()
        }
    }
    private fun setupMarkAllReadButton() {
        binding.buttonMarkAllRead.setOnClickListener {
            showMarkAllReadConfirmation()
        }
    }

    private fun showMarkAllReadConfirmation() {
        // Đếm số thông báo chưa đọc
        val unreadNotifications = notifications.filter { it.isUnread() }

        if (unreadNotifications.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "All notifications are already read",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Mark All as Read")
            .setMessage("Are you sure you want to mark all ${unreadNotifications.size} notifications as read?")
            .setPositiveButton("Yes") { _, _ ->
                markAllAsRead()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun markAllAsRead() {
        lifecycleScope.launch {
            try {
                binding.buttonMarkAllRead.isEnabled = false

                val response = RetrofitInstance.notificationService.markAllAsRead()

                if (response.isSuccessful) {
                    notifications.forEachIndexed { index, notification ->
                        if (notification.isUnread()) {
                            notifications[index] = notification.copy(status = 1)
                        }
                    }
                    notificationAdapter.notifyDataSetChanged()
                    loadUnreadCount()
                    Toast.makeText(
                        requireContext(),
                        "All notifications marked as read!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to mark all as read",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking all as read: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.buttonMarkAllRead.isEnabled = true
            }
        }
    }

    private fun loadUnreadCount() {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.notificationService.getUnreadCount()

                if (response.isSuccessful) {
                    response.body()?.let { unreadCountResponse ->
                        val count = unreadCountResponse.unreadCount
                        updateUnreadBadge(count)
                        (activity as? MainActivity)?.updateNotificationBadge(count)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading unread count: ${e.message}", e)
            }
        }
    }

    private fun updateUnreadBadge(count: Int) {
        if (count > 0) {
            binding.textViewUnreadCount.visibility = View.VISIBLE
            binding.textViewUnreadCount.text = if (count > 99) "99+" else count.toString()
        } else {
            binding.textViewUnreadCount.visibility = View.GONE
        }
    }

    private fun loadNotifications() {
        binding.swipeRefreshLayout.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.notificationService.getMyNotifications()

                if (response.isSuccessful) {
                    response.body()?.let { notificationList ->
                        notifications.clear()
                        notifications.addAll(notificationList)
                        notificationAdapter.notifyDataSetChanged()

                        updateEmptyState(notificationList.isEmpty())

                        loadUnreadCount()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun connectToSignalR() {
        signalRService.start {
            Log.d(TAG, "SignalR connected successfully")
        }
    }

    private fun observeRealTimeNotifications() {
        lifecycleScope.launch {
            signalRService.notificationFlow.collect { payload ->
                Log.d(TAG, "Received real-time notification: ${payload.title}")

                val notification = Notification(
                    notificationID = payload.notificationId,
                    userID = "",
                    content = payload.content,
                    type = when (payload.notificationType.lowercase()) {
                        "warning" -> 0  // C# enum: Warning = 0
                        "message" -> 1  // C# enum: Message = 1
                        else -> 1
                    },
                    status = when (payload.status.lowercase()) {
                        "unread" -> 0
                        "read" -> 1
                        else -> 0
                    },
                    target = payload.target,
                    timeSent = payload.timeSent
                )

                notifications.add(0, notification)
                notificationAdapter.notifyItemInserted(0)
                binding.recyclerViewNotifications.scrollToPosition(0)

                updateEmptyState(false)
                loadUnreadCount()

                Toast.makeText(requireContext(), payload.title, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNotificationDetail(notification: Notification) {
        val dialog = NotificationDetailDialog.newInstance(notification)
        dialog.setOnDismissListener {
            if (notification.isUnread()) {
                markNotificationAsRead(notification)
            }
        }
        dialog.show(childFragmentManager, "NotificationDetail")
    }

    private fun markNotificationAsRead(notification: Notification) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.notificationService.markAsRead(notification.notificationID)

                if (response.isSuccessful) {
                    val index = notifications.indexOfFirst { it.notificationID == notification.notificationID }
                    if (index != -1) {
                        notifications[index] = notification.copy(status = 1)
                        notificationAdapter.notifyItemChanged(index)
                        loadUnreadCount()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read: ${e.message}", e)
            }
        }
    }

    private fun showDeleteConfirmation(notification: Notification) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setPositiveButton("Delete") { _, _ ->
                deleteNotification(notification)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteNotification(notification: Notification) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.notificationService.deleteNotification(notification.notificationID)

                if (response.isSuccessful) {
                    // Remove from local list
                    val index = notifications.indexOfFirst { it.notificationID == notification.notificationID }
                    if (index != -1) {
                        notifications.removeAt(index)
                        notificationAdapter.notifyItemRemoved(index)

                        updateEmptyState(notifications.isEmpty())
                        loadUnreadCount()  // ← THÊM: Cập nhật badge

                        Toast.makeText(requireContext(), "Notification deleted !", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Unable to delete notification !", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting notification: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewNotifications.visibility = View.GONE
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.buttonMarkAllRead.visibility = View.GONE
        } else {
            binding.recyclerViewNotifications.visibility = View.VISIBLE
            binding.textViewEmpty.visibility = View.GONE
            binding.buttonMarkAllRead.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        signalRService.stop()
        _binding = null
    }

    companion object {
        private const val TAG = "NotificationsFragment"
    }
}