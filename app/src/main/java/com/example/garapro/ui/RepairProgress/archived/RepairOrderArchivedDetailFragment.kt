package com.example.garapro.ui.RepairProgress.archived

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.garapro.R
import com.example.garapro.data.model.RepairProgresses.ArchivedJob
import com.example.garapro.data.model.RepairProgresses.CarPickupStatus
import com.example.garapro.data.model.RepairProgresses.Feedback
import com.example.garapro.data.model.RepairProgresses.RepairOrderArchivedDetail
import com.example.garapro.data.repository.RepairProgress.RepairProgressRepository
import com.example.garapro.databinding.FragmentRepairOrderArchivedDetailBinding
import com.example.garapro.ui.feedback.RatingActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class RepairOrderArchivedDetailFragment : Fragment() {

    private var _binding: FragmentRepairOrderArchivedDetailBinding? = null
    private val binding get() = _binding!!
    private var currentRepairOrderId: String? = null


    private lateinit var jobsAdapter: ArchivedJobAdapter

    private val viewModel: RepairOrderArchivedDetailViewModel by viewModels {
        RepairOrderArchivedDetailViewModelFactory()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRepairOrderArchivedDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupJobsRecycler()
        observeViewModel()

        val repairOrderId = arguments?.getString("repairOrderId")
        if (repairOrderId == null) {
            Toast.makeText(requireContext(), "Missing repairOrderId", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.loadDetail(repairOrderId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupJobsRecycler() {
        jobsAdapter = ArchivedJobAdapter { job ->
            showJobDetail(job)
        }
        binding.rvJobs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = jobsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.detailState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RepairProgressRepository.ApiResponse.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RepairProgressRepository.ApiResponse.Success -> {
                    binding.progressBar.visibility = View.GONE
                    bindDetail(state.data)
                }
                is RepairProgressRepository.ApiResponse.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }


        viewModel.updateStatusState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is RepairProgressRepository.ApiResponse.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RepairProgressRepository.ApiResponse.Success -> {
                    binding.progressBar.visibility = View.GONE
                    // Sau khi update status thành công -> load lại detail
                    currentRepairOrderId?.let { viewModel.loadDetail(it) }
                }
                is RepairProgressRepository.ApiResponse.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun bindDetail(detail: RepairOrderArchivedDetail) = with(binding) {
        currentRepairOrderId = detail.repairOrderId
        tvLicensePlate.text = detail.licensePlate
        tvBranchName.text = detail.branchName
        tvModelName.text = detail.modelName

        tvReceiveDate.text = formatDateTime(detail.receiveDate)
        tvArchivedAt.text = detail.archivedAt?.let { formatDateTime(it) } ?: "-"

        tvCost.text = formatCurrency(detail.cost)
        tvNote.text = detail.note ?: "-"

        val jobs = detail.jobs
        val hasJobs = !jobs.isNullOrEmpty()

        rvJobs.visibility = if (hasJobs) View.VISIBLE else View.GONE
        tvNoJobsMessage.visibility = if (hasJobs) View.GONE else View.VISIBLE

        if (hasJobs) {
            jobsAdapter.submitList(jobs)
        } else {
            jobsAdapter.submitList(emptyList())
            tvNoJobsMessage.text =
                "No repairs were performed. This order includes inspection fee only."
        }

        // Feedback UI
        if (detail.feedBacks != null) {
            feedbackCard.visibility = View.VISIBLE
            btnFeedback.visibility = View.GONE

            ratingBarFeedback.rating = detail.feedBacks.rating.toFloat()
            tvFeedbackRatingValue.text = detail.feedBacks.rating.toString()
            tvFeedbackDescription.text = detail.feedBacks.description
        } else {
            feedbackCard.visibility = View.GONE
            // visibility của btnFeedback sẽ điều khiển theo carPickupStatus bên dưới
        }

        //  CarPickupStatus UI logic
//        when (detail.carPickupStatus) {
//            CarPickupStatus.None -> {
//                // Cho phép chọn: Already picked / Not yet
//                layoutPickupActions.visibility = View.VISIBLE
//
//                // Khi chưa chọn pickup status thì không cho feedback
//                btnFeedback.visibility = View.GONE
//            }
//
//            CarPickupStatus.PickedUp-> {
//                layoutPickupActions.visibility = View.GONE
//
//                // Chỉ show nút feedback nếu chưa có feedback
//                btnFeedback.visibility = if (detail.feedBacks == null) View.VISIBLE else View.GONE
//            }
//
//            CarPickupStatus.NotPickedUp -> {
//                layoutPickupActions.visibility = View.GONE
//                // Nếu khách bảo chưa lấy → không show feedback button
//                btnFeedback.visibility = View.GONE
//            }
//        }

        // Click listeners cho 2 nút pickup
//        btnAlreadyPickedUp.setOnClickListener {
//            currentRepairOrderId?.let { id ->
//                showConfirmPickedUpDialog(
//                    onConfirm = {
//                        viewModel.updateCarPickupStatus(id, CarPickupStatus.PickedUp)
//                        showThankYouDialog()
//                    }
//                )
//            }
//        }
//
//        btnNotPickedUp.setOnClickListener {
//            currentRepairOrderId?.let { id ->
//                showConfirmNotPickedUpDialog(
//                    onConfirm = {
//                        viewModel.updateCarPickupStatus(id, CarPickupStatus.NotPickedUp)
//                        showSorryDialog()
//                    }
//                )
//            }
//        }

        // Nút Feedback (chỉ dùng khi PICKED_UP & chưa có feedback)
        btnFeedback.setOnClickListener {
            val intent = Intent(requireContext(), RatingActivity::class.java)
            intent.putExtra(RatingActivity.EXTRA_REPAIR_ORDER_ID, detail.repairOrderId)
            ratingActivityLauncher.launch(intent)
        }
    }


    private val ratingActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RatingActivity.RESULT_FEEDBACK_POSTED) {

                // Refresh lại detail hoặc load lại API
                viewModel.loadDetail(currentRepairOrderId.orEmpty());

                Toast.makeText(requireContext(), "Thank for rating!", Toast.LENGTH_SHORT).show()
            }
        }


    private fun showConfirmPickedUpDialog(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm pickup")
            .setMessage("Are you sure you have already picked up your car?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showConfirmNotPickedUpDialog(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm status")
            .setMessage("Are you sure you have not picked up your car yet?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showThankYouDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Thank you")
            .setMessage("Thank you for confirming that you have already picked up your car.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSorryDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("We are sorry")
            .setMessage("We are sorry for the inconvenience. Our staff will double–check your repair order.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun formatDateTime(value: String?): String {
        if (value.isNullOrEmpty()) return "-"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = input.parse(value)
            date?.let { output.format(it) } ?: "-"
        } catch (e: Exception) {
            value
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(amount)
    }

    private fun showJobDetail(job: ArchivedJob) {
        RepairArchivedJobDetailBottomSheet.newInstance(job)
            .show(childFragmentManager, "jobDetail")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
