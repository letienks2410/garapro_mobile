package com.example.garapro.ui.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.garapro.R
import com.example.garapro.data.local.TokenManager
import com.example.garapro.data.model.repairRequest.RepairRequestDetail
import com.example.garapro.data.model.repairRequest.RequestServiceDetail
import com.example.garapro.data.repository.repairRequest.BookingRepository
import com.example.garapro.databinding.BottomSheetRepairRequestDetailBinding
import com.example.garapro.utils.MoneyUtils
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class RepairRequestDetailFragment : Fragment() {

    private var _binding: BottomSheetRepairRequestDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private val repository by lazy { BookingRepository(requireContext(), tokenManager) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRepairRequestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        val repairRequestId = arguments?.getString("repairRequestId")
        if (repairRequestId == null) {
            // Không có id -> có thể show error / back
            return
        }

        binding.toolbarDetail.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            // Nếu layout có progressBar riêng, bạn có thể dùng:
            // binding.progressBar.visibility = View.VISIBLE

            try {
                val detail = repository.getRepairRequestDetail(repairRequestId)
                if (detail != null) {
                    setupUI(detail)
                } else {
                    // TODO: show error UI
                }
            } catch (e: Exception) {
                // TODO: show error UI
            } finally {
                // binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI(detail: RepairRequestDetail) {
        // Basic info
        binding.tvRequestId.text = "ID: ${detail.repairRequestID.take(8)}..."
        binding.tvDescription.text = detail.description ?: "No description provided"
        binding.tvRequestDate.text = formatDate(detail.requestDate)
        binding.tvStatus.text = getStatusText(detail.status)
        binding.tvStatus.setBackgroundResource(getStatusBackground(detail.status))
//        binding.tvEstimatedCost.visibility = View.GONE

        // Vehicle info
        val brandName = detail.vehicle.brandName ?: "Unknown Brand"
        val modelName = detail.vehicle.modelName ?: "Unknown Model"
        binding.tvVehicle.text = "$brandName $modelName"
        binding.tvLicensePlate.text = detail.vehicle.licensePlate ?: "Not provided"
        binding.tvVin.text = detail.vehicle.vin ?: "Not provided"
        binding.tvYear.text = detail.vehicle.year?.toString() ?: "Not provided"
        binding.tvOdometer.text =
            if (detail.vehicle.odometer != null) "${detail.vehicle.odometer} km" else "Not provided"

        // Images
        loadImages(
            detail.imageUrls,
            binding.ivImage1,
            binding.ivImage2,
            binding.ivImage3,
            binding.ivImage4
        )

        // Services
        setupServicesList(binding.rvServices, detail.requestServices)

        // Total cost
        val totalCost = detail.requestServices?.sumOf { it.price ?: 0.0 } ?: 0.0
        binding.tvTotalCost.text = MoneyUtils.formatVietnameseCurrency(totalCost)

        val isCompleted = detail.status == 4  // COMPLETED
        val repairOrderId = detail.repairOrderId   // đổi cho đúng field model của bạn

        if (isCompleted && !repairOrderId.isNullOrEmpty()) {
            binding.btnViewRepairProgress.visibility = View.VISIBLE

            binding.btnViewRepairProgress.setOnClickListener {
                // Tự quyết trong code: đi graph thường hay archived
                val isArchieved = detail.isArchived == true   // hoặc logic khác của bạn

                val actionId = if (isArchieved) {
                    R.id.action_global_repairArchivedFromRequest
                } else {
                    R.id.action_global_repairTrackingFromRequest
                }

                val bundle = bundleOf(
                    "repairOrderId" to repairOrderId
                )

                findNavController().navigate(actionId, bundle)
            }
        } else {
            binding.btnViewRepairProgress.visibility = View.GONE
        }
    }

    private fun loadImages(
        imageUrls: List<String>?,
        image1: ShapeableImageView?,
        image2: ShapeableImageView?,
        image3: ShapeableImageView?,
        image4: ShapeableImageView?
    ) {
        val imageViews = listOf(image1, image2, image3, image4)

        imageViews.forEach { it?.visibility = View.GONE }

        imageUrls?.take(4)?.forEachIndexed { index, imageUrl ->
            imageViews.getOrNull(index)?.let { imageView ->
                imageView.visibility = View.VISIBLE
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_camera)
                    .error(R.drawable.ic_camera)
                    .centerCrop()
                    .into(imageView)
            }
        }

        if (imageUrls != null && imageUrls.size > 4) {
            image4?.setOnClickListener {
                showFullScreenImages(imageUrls)
            }
        }
    }

    private fun showFullScreenImages(imageUrls: List<String>) {
        // TODO: Implement fullscreen image viewer
    }

    private fun setupServicesList(
        recyclerView: RecyclerView,
        services: List<RequestServiceDetail>?
    ) {
        val serviceList = services ?: emptyList()
        val adapter = RepairRequestServicesAdapter(serviceList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "Date not available"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: "Invalid date"
        } catch (e: Exception) {
            "Date format error"
        }
    }

    private fun getStatusText(status: Int?): String {
        return when (status) {
            0 -> "Pending"
            1 -> "Accepted"
            2 -> "Arrived"
            3 -> "Cancelled"
            4 -> "COMPLETED"
            else -> "Unknown"
        }
    }

    private fun getStatusBackground(status: Int?): Int {
        return when (status) {
            0 -> R.drawable.bg_status_pending
            1 -> R.drawable.bg_status_accept
            2 -> R.drawable.bg_status_arrived
            3 -> R.drawable.bg_status_cancelled
            4 -> R.drawable.bg_status_arrived
            else -> R.drawable.bg_status_pending
        }
    }
}
