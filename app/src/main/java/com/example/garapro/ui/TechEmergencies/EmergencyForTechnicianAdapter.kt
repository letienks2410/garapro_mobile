package com.example.garapro.ui.TechEmergencies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.techEmergencies.EmergencyForTechnicianDto
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class EmergencyForTechnicianAdapter(
    private val onItemClick: (EmergencyForTechnicianDto) -> Unit
) : ListAdapter<EmergencyForTechnicianDto, EmergencyForTechnicianAdapter.EmergencyViewHolder>(DIFF) {

    companion object {
        // TODO: thay "id" bằng field định danh thật của bạn (emergencyId / requestId / ...)
        private val DIFF = object : DiffUtil.ItemCallback<EmergencyForTechnicianDto>() {
            override fun areItemsTheSame(
                oldItem: EmergencyForTechnicianDto,
                newItem: EmergencyForTechnicianDto
            ): Boolean = oldItem.emergencyRequestId == newItem.emergencyRequestId

            override fun areContentsTheSame(
                oldItem: EmergencyForTechnicianDto,
                newItem: EmergencyForTechnicianDto
            ): Boolean = oldItem == newItem
        }
    }

    inner class EmergencyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(item: EmergencyForTechnicianDto) {
            tvCustomerName.text = item.customerName ?: "Unknown"
            tvPhone.text = item.phoneNumber ?: "-"
            tvIssue.text = item.issueDescription
            tvStatus.text = item.emergencyStatus.name

            tvTime.text = DateTimePretty.formatForBangkok(item.requestTime)

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emergency_for_technician, parent, false)
        return EmergencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmergencyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/**
 * Formatter tối ưu:
 * - Cache formatter
 * - Parse nhiều kiểu input
 * - Convert về Asia/Bangkok
 */
private object DateTimePretty {
    private val BKK: ZoneId = ZoneId.of("Asia/Bangkok")
    private val OUT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    // các input hay gặp
    private val ISO_OFFSET: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME   // 2025-12-13T10:15:30+07:00
    private val ISO_INSTANT: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT          // 2025-12-13T03:15:30Z
    private val ISO_LOCAL: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME    // 2025-12-13T10:15:30
    private val SPACE_LOCAL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // 2025-12-13 10:15:30

    fun formatForBangkok(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"

        val zdt = parseToZonedDateTime(raw) ?: return raw
        return OUT.format(zdt.withZoneSameInstant(BKK))
    }

    private fun parseToZonedDateTime(raw: String): ZonedDateTime? {
        // 1) Có Z (instant)
        try {
            val instant = Instant.from(ISO_INSTANT.parse(raw))
            return instant.atZone(BKK)
        } catch (_: DateTimeParseException) {}

        // 2) Có offset +hh:mm
        try {
            val odt = OffsetDateTime.parse(raw, ISO_OFFSET)
            return odt.toZonedDateTime()
        } catch (_: DateTimeParseException) {}

        // 3) LocalDateTime (không zone) -> assume Bangkok (hoặc đổi thành UTC tuỳ backend bạn)
        try {
            val ldt = LocalDateTime.parse(raw, ISO_LOCAL)
            return ldt.atZone(BKK)
        } catch (_: DateTimeParseException) {}

        // 4) "yyyy-MM-dd HH:mm:ss"
        try {
            val ldt = LocalDateTime.parse(raw, SPACE_LOCAL)
            return ldt.atZone(BKK)
        } catch (_: DateTimeParseException) {}

        return null
    }
}
