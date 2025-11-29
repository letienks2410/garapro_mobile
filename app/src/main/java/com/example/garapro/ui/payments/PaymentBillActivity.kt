package com.example.garapro.ui.payments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.payments.CreatePaymentRequest
import com.example.garapro.data.model.payments.RepairOrderPaymentDto
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.data.repository.PaymentRepository
import com.example.garapro.utils.MoneyUtils
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class PaymentBillActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REPAIR_ORDER_ID = "extra_repair_order_id"
    }


    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvVehicleValue: TextView
    private lateinit var tvPlateValue: TextView
    private lateinit var tvOdometerValue: TextView

    private lateinit var tvCost: TextView





    private lateinit var tvTotalToPay: TextView


    private lateinit var btnPay: Button
    private lateinit var rvServices: RecyclerView

    private lateinit var serviceAdapter: QuotationServiceAdapter
    private lateinit var paymentRepository: PaymentRepository

    private var currentBill: RepairOrderPaymentDto? = null

    // If you have a ViewModel for payment, you can use it here.
    // Replace PaymentBillViewModel with your actual ViewModel class if needed.
    // private val viewModel: PaymentBillViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_bill)

        initViews()
        initAdapters()

        paymentRepository = PaymentRepository(RetrofitInstance.paymentService)

        val repairOrderId = intent.getStringExtra(EXTRA_REPAIR_ORDER_ID)
        if (repairOrderId.isNullOrEmpty()) {
            Toast.makeText(this, "Missing repair order id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchPaymentBill(repairOrderId)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        tvVehicleValue = findViewById(R.id.tvVehicleValue)
        tvPlateValue = findViewById(R.id.tvPlateValue)
        tvOdometerValue = findViewById(R.id.tvOdometerValue)



        tvCost = findViewById(R.id.tvCost)


        tvTotalToPay = findViewById(R.id.tvTotalToPay)

        btnPay = findViewById(R.id.btnPay)
        rvServices = findViewById(R.id.rvServices)



        btnPay.setOnClickListener {
            currentBill?.let { bill ->
                showPaymentDialog(bill)
            } ?: run {
                Toast.makeText(this, "Bill is not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initAdapters() {
        serviceAdapter = QuotationServiceAdapter()
        rvServices.apply {
            layoutManager = LinearLayoutManager(this@PaymentBillActivity)
            adapter = serviceAdapter
        }
    }

    private fun fetchPaymentBill(repairOrderId: String) {
        lifecycleScope.launch {
            val result = paymentRepository.getPaymentBill(repairOrderId)
            result.onSuccess { bill ->
                currentBill = bill
                bindBill(bill)
            }.onFailure { e ->
                Toast.makeText(
                    this@PaymentBillActivity,
                    "Failed to load bill: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindBill(bill: RepairOrderPaymentDto) {
        val v = bill.vehicle

        // Vehicle info
        tvVehicleValue.text = "${v.brandName} ${v.modelName} (${v.year})"
        tvPlateValue.text = v.licensePlate
        tvOdometerValue.text = "${v.odometer ?: 0} km"
        tvCost.text = MoneyUtils.formatVietnameseCurrency(bill.cost)

        // Services
        val allServices = bill.approvedQuotations.flatMap { it.services }
        serviceAdapter.submitList(allServices)

        // Tổng tiền
        tvTotalToPay.text = "Amount Due: ${MoneyUtils.formatVietnameseCurrency(bill.cost)}"

        // Summary: "Service Name: partPrice - discount = finalPrice"

    }

    // --------------------------------------------------
    // Payment support using your 3 helper-style methods
    // --------------------------------------------------

    private fun showPaymentDialog(bill: RepairOrderPaymentDto) {
        val vehicleLabel = "${bill.vehicle.brandName} ${bill.vehicle.modelName}"
        val totalNet = bill.approvedQuotations.sumOf { it.netAmount }
//        val remaining = totalNet - bill.paidAmount
        val remaining = bill.cost

        AlertDialog.Builder(this)
            .setTitle("Confirm payment")
            .setMessage(
                "Do you want to pay for repair order of $vehicleLabel?\n\n" +
                        "Amount: ${remaining.toLong()} ₫"
            )
            .setPositiveButton("Pay") { _, _ ->
                processPayment(bill, remaining)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun processPayment(bill: RepairOrderPaymentDto, amount: Double) {
        val ctx = this
        lifecycleScope.launch {
            try {
                val body = CreatePaymentRequest(
                    repairOrderId = bill.repairOrderId,
                    amount = amount.toInt(), // adjust type if your API expects Double
//                    amount = 2000, // adjust type if your API expects Double
                    description = "Payment for ${bill.vehicle.licensePlate}"
                )

                // Option 1: via repository directly
                val result = paymentRepository.createPaymentLink(body)
                val res = result.getOrNull()

                // Option 2: via ViewModel (if you already have this):
                // val res = viewModel.createPaymentLinkDirect(body)

                if (res != null) {
                    openInAppCheckout(ctx, res.checkoutUrl)
                } else {
                    Toast.makeText(ctx, "Could not create payment link", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("Payment", "create-link failed", e)
                Toast.makeText(ctx, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openInAppCheckout(context: Context, url: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}