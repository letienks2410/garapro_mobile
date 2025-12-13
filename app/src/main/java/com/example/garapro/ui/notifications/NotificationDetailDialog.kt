package com.example.garapro.ui.notifications

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.garapro.R
import com.example.garapro.data.model.Notification
import com.example.garapro.databinding.DialogNotificationDetailBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationDetailDialog : DialogFragment() {

    private var _binding: DialogNotificationDetailBinding? = null
    private val binding get() = _binding!!

    private var onDismissListener: (() -> Unit)? = null

    private lateinit var notification: Notification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNotificationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notification = arguments?.getSerializable(ARG_NOTIFICATION) as Notification

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Set type badge (0 = Warning, 1 = Message)
        when (notification.type) {
            0 -> {
                binding.textViewType.text = "Warming Detail"
                binding.textViewType.setBackgroundColor(Color.parseColor("#F44336"))
            }
            1 -> {
                binding.textViewType.text = "Notification Detail"
                binding.textViewType.setBackgroundColor(Color.parseColor("#4CAF50"))
            }
            else -> {
                binding.textViewType.text = "Notification Detail"
                binding.textViewType.setBackgroundColor(Color.parseColor("#2196F3"))
            }
        }

        // Set content
        binding.textViewContent.text = notification.content

        // Set time
        binding.textViewTime.text = "Time sent: ${formatTime(notification.timeSent)}"

        // Set type info (hiển thị loại thông báo)
        binding.textViewTarget.visibility = View.VISIBLE
        binding.textViewTarget.text = "Type: ${notification.getTypeString()}"

        when (notification.type) {
            0 -> binding.textViewTarget.setTextColor(Color.parseColor("#F44336")) // Đỏ - Warning
            1 -> binding.textViewTarget.setTextColor(Color.parseColor("#4CAF50")) // Xanh - Message
            else -> binding.textViewTarget.setTextColor(Color.parseColor("#757575"))
        }
    }

    private fun setupClickListeners() {
        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.buttonGoto.setOnClickListener {
            // TODO: Navigate to target screen based on notification.target
            // Bạn có thể parse target để navigate đến màn hình tương ứng
            dismiss()
        }
    }

    private fun formatTime(timeSent: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(timeSent)

            val outputFormat = SimpleDateFormat("dd/MM/yyyy 'lúc' HH:mm", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            timeSent
        }
    }

    fun setOnDismissListener(listener: () -> Unit) {
        this.onDismissListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDismissListener?.invoke()
        _binding = null
    }

    companion object {
        private const val ARG_NOTIFICATION = "notification"

        fun newInstance(notification: Notification): NotificationDetailDialog {
            val dialog = NotificationDetailDialog()
            val args = Bundle()
            args.putSerializable(ARG_NOTIFICATION, notification)
            dialog.arguments = args
            return dialog
        }
    }
}