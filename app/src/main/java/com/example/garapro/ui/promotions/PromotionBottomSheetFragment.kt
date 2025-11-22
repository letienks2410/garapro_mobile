package com.example.garapro.ui.promotions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.garapro.data.model.quotations.CustomerPromotion
import com.example.garapro.databinding.FragmentPromotionBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale

class PromotionBottomSheetFragment : BottomSheetDialogFragment() {

    interface PromotionSelectionListener {
        fun onPromotionSelected(promotion: CustomerPromotion?, originalPrice: Double)
        fun onPromotionRemoved(originalPrice: Double)
    }

    private var listener: PromotionSelectionListener? = null

    fun setListener(listener: PromotionSelectionListener) {
        this.listener = listener
    }

    private lateinit var binding: FragmentPromotionBottomSheetBinding

    private var serviceName: String = ""
    private var originalPrice: Double = 0.0
    private var promotions: List<CustomerPromotion> = emptyList()
    private var selectedPromotionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("PromotionSheet", "onCreate, promotions size = ${promotions.size}")
        arguments?.let { args ->
            serviceName = args.getString(ARG_SERVICE_NAME, "")
            originalPrice = args.getDouble(ARG_ORIGINAL_PRICE, 0.0)
            @Suppress("UNCHECKED_CAST")
            promotions = args.getSerializable(ARG_PROMOTIONS) as? ArrayList<CustomerPromotion> ?: arrayListOf()
            selectedPromotionId = args.getString(ARG_SELECTED_PROMOTION_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPromotionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("PromotionSheet", "onViewCreated called")
        setupHeader()
        setupRecyclerView()
        setupButtons()
    }

    private fun setupHeader() {
        binding.tvTitle.text = "Choose promotion"
        binding.tvServiceName.text = serviceName

        // Default: no promotion -> original == final, discount = 0
        updatePriceSummary(originalPrice, originalPrice)
    }

    private fun setupRecyclerView() {
        val adapter = PromotionListAdapter(promotions, selectedPromotionId) { promotion ->
            // When user changes selected item, update summary preview
            val finalPrice = promotion.finalPriceAfterDiscount
            updatePriceSummary(originalPrice, finalPrice)
            selectedPromotionId = promotion.id
        }

        binding.rvPromotions.adapter = adapter
        binding.rvPromotions.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupButtons() {
        binding.btnApply.setOnClickListener {
            val selected = promotions.find { it.id == selectedPromotionId }
            listener?.onPromotionSelected(selected, originalPrice)
            dismiss()
        }

        binding.btnRemovePromotion.setOnClickListener {
            listener?.onPromotionRemoved(originalPrice)
            dismiss()
        }
    }

    private fun updatePriceSummary(original: Double, final: Double) {
        val discountAmount = original - final

        binding.tvOriginalPrice.text = "Original: ${formatCurrency(original)}"
        binding.tvFinalPrice.text = "Final: ${formatCurrency(final)}"
        binding.tvDiscountAmount.text = if (discountAmount > 0) {
            "Discount: -${formatCurrency(discountAmount)}"
        } else {
            "Discount: 0 ₫"
        }
    }

    private fun formatCurrency(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(value)
    }

    companion object {
        private const val ARG_SERVICE_NAME = "arg_service_name"
        private const val ARG_ORIGINAL_PRICE = "arg_original_price"
        private const val ARG_PROMOTIONS = "arg_promotions"
        private const val ARG_SELECTED_PROMOTION_ID = "arg_selected_promotion_id"

        fun newInstance(
            serviceName: String,
            originalPrice: Double,
            promotions: List<CustomerPromotion>,
            selectedPromotionId: String?
        ): PromotionBottomSheetFragment {
            return PromotionBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SERVICE_NAME, serviceName)
                    putDouble(ARG_ORIGINAL_PRICE, originalPrice)
                    putSerializable(ARG_PROMOTIONS, ArrayList(promotions))
                    putString(ARG_SELECTED_PROMOTION_ID, selectedPromotionId)
                }
            }
        }
    }
}