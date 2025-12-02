package com.example.garapro.ui.appointments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.garapro.R
import com.example.garapro.data.local.TokenManager
import com.example.garapro.data.repository.repairRequest.BookingRepository
import com.example.garapro.databinding.FragmentWaitingAssignmentBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WaitingAssignmentFragment : Fragment() {

    private var _binding: FragmentWaitingAssignmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private lateinit var repository: BookingRepository
    private var pollingJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWaitingAssignmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenManager = TokenManager(requireContext())
        repository = BookingRepository(requireContext(), tokenManager)

        val repairRequestId = arguments?.getString("repairRequestId")
        if (repairRequestId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Missing request id", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        startPolling(repairRequestId)
    }

    private fun startPolling(repairRequestId: String) {
        stopPolling()
        pollingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                try {
                    val detail = repository.getRepairRequestDetail(repairRequestId)
                    if (detail != null) {
                        // If repair order created or moved forward, navigate to progress
                        if (!detail.repairOrderId.isNullOrEmpty() || (detail.status ?: 0) >= 2) {
                            val bundle = Bundle().apply { putString("repairOrderId", detail.repairOrderId) }
                            findNavController().navigate(R.id.action_global_repairTrackingFromRequest, bundle)
                            break
                        }
                    }
                } catch (_: Exception) {}
                delay(5000)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling()
        _binding = null
    }
}
