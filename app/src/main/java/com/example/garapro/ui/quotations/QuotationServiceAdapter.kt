package com.example.garapro.ui.quotations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.data.model.quotations.QuotationServicePart
import com.example.garapro.data.model.quotations.QuotationService
import com.example.garapro.databinding.ItemQuotationServiceBinding
import java.text.NumberFormat
import java.util.Locale

class QuotationServiceAdapter(
    private var services: List<QuotationService>,
    private var onCheckChanged: (String, Boolean) -> Unit,
    private var isEditable: Boolean = true // 🔥 THÊM: Biến kiểm tra có được chỉnh sửa không
) : RecyclerView.Adapter<QuotationServiceAdapter.ViewHolder>() {

    /**
     * 🔥 HÀM MỚI: Cập nhật trạng thái chỉnh sửa
     */
    fun updateEditable(editable: Boolean) {
        this.isEditable = editable
        notifyDataSetChanged() // Refresh toàn bộ để áp dụng trạng thái mới
    }

    /**
     * 🔥 HÀM MỚI: Cập nhật callback cho checkbox
     */
    fun updateOnCheckChanged(newOnCheckChanged: (String, Boolean) -> Unit) {
        this.onCheckChanged = newOnCheckChanged
        notifyDataSetChanged()
    }

    fun updateServices(newServices: List<QuotationService>) {
        services = newServices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemQuotationServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(services[position])
    }

    override fun getItemCount() = services.size

    inner class ViewHolder(private val binding: ItemQuotationServiceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(service: QuotationService) {
            binding.tvServiceName.text = service.serviceName
            binding.tvServiceDescription.text = service.serviceDescription
            binding.tvServicePrice.text = formatCurrency(service.totalPrice)

            // 🔥 THAY ĐỔI: Vô hiệu hóa checkbox khi không được chỉnh sửa
            binding.cbService.isEnabled = isEditable

            binding.cbService.setOnCheckedChangeListener(null)
            binding.cbService.isChecked = service.isSelected

            // THAY ĐỔI: Chỉ set listener khi được phép chỉnh sửa
            if (isEditable) {
                binding.cbService.setOnCheckedChangeListener { _, isChecked ->
                    onCheckChanged(service.quotationServiceId, isChecked)
                }
            } else {
                binding.cbService.setOnCheckedChangeListener(null)
            }

            setupPartsInfo(service.quotationServiceParts)
        }

        private fun setupPartsInfo(parts: List<QuotationServicePart>) {
            parts.joinToString("\n") {
                "• ${it.partName} - ${formatCurrency(it.totalPrice)}${if (it.isRecommended) "" else ""}"
            }.takeIf { it.isNotEmpty() }?.let { info ->
                binding.tvPartsInfo.visibility = View.VISIBLE
                binding.tvPartsInfo.text = "Phụ tùng kèm theo:\n$info"
            } ?: run {
                binding.tvPartsInfo.visibility = View.GONE
            }
        }


        private fun formatCurrency(amount: Double) =
            NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
    }
}