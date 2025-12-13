package com.example.garapro.ui.emergencies

import com.example.garapro.ui.emergencies.EmergencyViewModel
import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.emergencies.Garage
import com.google.android.material.bottomsheet.BottomSheetDialog

class EmergencyBottomSheet(
    private val context: android.content.Context,
    private val viewModel: EmergencyViewModel
) {

    private var bottomSheetDialog: BottomSheetDialog? = null
    private lateinit var garageAdapter: GarageAdapter
    private var onConfirmClickListener: (() -> Unit)? = null
    private var onDismissClickListener: (() -> Unit)? = null
    private var lastGarage: Garage? = null
    private var onChooseAnotherListener: (() -> Unit)? = null
    private var onTrackClickListener: (() -> Unit)? = null
    private var trackingView: View? = null
    private var suppressDismiss: Boolean = false
    private var onViewMapClickListener: (() -> Unit)? = null
    private var onCloseClickListener: (() -> Unit)? = null

    fun show(
        garages: List<Garage>,
        selectedGarage: Garage? = null,
        onConfirm: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        this.onConfirmClickListener = onConfirm
        this.onDismissClickListener = onDismiss

        bottomSheetDialog?.dismiss()
        bottomSheetDialog = BottomSheetDialog(context).apply {
            setContentView(createBottomSheetView(garages, selectedGarage))
            setCancelable(true)
            setOnDismissListener {
                if (!suppressDismiss) onDismissClickListener?.invoke()
            }
            show()
        }
    }

    fun showWaitingForGarage(garage: Garage) {
        dismissSilently()
        bottomSheetDialog = null
        Log.d("EmergencyState", "🟢 WaitingForGarage triggered fo")
        val dialog = BottomSheetDialog(context as Activity)
        dialog.setContentView(createWaitingView(garage))
        dialog.setCancelable(false)
        dialog.show()

        bottomSheetDialog = dialog
        lastGarage = garage
        Log.d("EmergencyState", "✅ showWaitingForGarage displayed for ${garage.name}")
    }

    fun dismiss() {
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }

    fun dismissSilently() {
        suppressDismiss = true
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
        suppressDismiss = false
    }

    fun updateGarages(garages: List<Garage>) {
        if (!::garageAdapter.isInitialized) return
        garageAdapter.submitList(garages)
        updateTitle(garages.size)
    }

    fun updateSelectedGarage(garage: Garage?) {
        updateConfirmButton(garage)
    }

    fun showAccepted(garage: Garage, etaMinutes: Int?, arrived: Boolean = false) {
        dismissSilently()
        bottomSheetDialog = BottomSheetDialog(context).apply {
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_accepted, null)
            setContentView(view)
            setCancelable(true)
            val tvName = view.findViewById<TextView>(R.id.tvGarageNameAccepted)
            val tvAddr = view.findViewById<TextView>(R.id.tvGarageAddressAccepted)
            val tvDistance = view.findViewById<TextView>(R.id.tvDistanceAccepted)
            val tvEta = view.findViewById<TextView>(R.id.tvEtaAccepted)
            val btnTrack = view.findViewById<Button>(R.id.btnTrackTech)
            val btnCall = view.findViewById<Button>(R.id.btnCallGarage)
            tvName.text = garage.name
            tvAddr.text = garage.address
            tvDistance.text = "Distance: ${garage.distance.formatDistance()} km"
            val etaText = if (arrived) {
                "Technician has arrived"
            } else {
                etaMinutes?.let { "ETA ~ ${it} min" } ?: "ETA ~ ${((garage.distance ?: 0.0) / 30.0 * 60).toInt()} min"
            }
            tvEta.text = etaText
            btnTrack.setOnClickListener {
                dismiss()
                onTrackClickListener?.invoke()
            }
            btnCall.setOnClickListener {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                    intent.data = android.net.Uri.parse("tel:" + (garage.phone ?: ""))
                    (context as Activity).startActivity(intent)
                } catch (_: Exception) {}
            }
            show()
        }
    }

    fun showArrived(garage: Garage, techName: String? = null, techPhone: String? = null) {
        dismissSilently()
        bottomSheetDialog = BottomSheetDialog(context).apply {
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_arrived, null)
            setContentView(view)
            setCancelable(true)
            view.findViewById<TextView>(R.id.tvArrivedGarageName)?.text = garage.name
            view.findViewById<TextView>(R.id.tvArrivedGarageAddress)?.text = garage.address
            view.findViewById<TextView>(R.id.tvTechNameArrived)?.text = techName ?: "Technician"
            view.findViewById<TextView>(R.id.tvTechPhoneArrived)?.text = techPhone ?: ""
            view.findViewById<Button>(R.id.btnCloseArrived)?.setOnClickListener {
                dismiss()
                onCloseClickListener?.invoke()
            }
            show()
        }
    }

    fun setOnCloseClickListener(listener: (() -> Unit)?) {
        onCloseClickListener = listener
    }

    fun showAcceptedWaitingForTechnician(garage: Garage) {
        dismissSilently()
        bottomSheetDialog = BottomSheetDialog(context).apply {
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_waiting_technician, null)
            setContentView(view)
            setCancelable(false)
            val tvTitle = view.findViewById<TextView>(R.id.tvAcceptedTitle)
            val tvSubtitle = view.findViewById<TextView>(R.id.tvAcceptedSubtitle)
            val tvName = view.findViewById<TextView>(R.id.tvGarageNameWaiting)
            val tvAddr = view.findViewById<TextView>(R.id.tvGarageAddressWaiting)
            val tvDistance = view.findViewById<TextView>(R.id.tvGarageDistanceWaiting)
            tvTitle.text = "Garage accepted your request"
            tvSubtitle.text = "Waiting for the garage to assign a technician"
            tvName.text = garage.name
            tvAddr.text = garage.address
            tvDistance.text = "Distance: ${garage.distance.formatDistance()} km"
            show()
        }
    }

    fun setOnTrackClickListener(listener: (() -> Unit)?) {
        onTrackClickListener = listener
    }

    private fun createWaitingView(garage: Garage): View {
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_waiting_garage, null)

        val tvGarageName = view.findViewById<TextView>(R.id.tvGarageName)
        val tvGarageInfo = view.findViewById<TextView>(R.id.tvGarageInfo)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvWaitingText = view.findViewById<TextView>(R.id.tvWaitingText)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // Hiển thị thông tin gara
        tvGarageName.text = garage.name
        tvGarageInfo.text = "${garage.distance.formatDistance()} km • ${garage.rating} ⭐"
        tvWaitingText.text = "Waiting for ${garage.name} to confirm..."

        // Setup nút hủy
        btnCancel.setOnClickListener {
            viewModel.cancelEmergencyRequest()
            dismiss()
            onDismissClickListener?.invoke()
        }

        // Animation loading
        setupLoadingAnimation(progressBar)
        Log.d("EmergencyState", "🟢 WaitingForGarage done view")

        return view
    }
    private fun setupLoadingAnimation(progressBar: ProgressBar) {
        // Có thể thêm animation cho progress bar nếu muốn
        val rotateAnimation = ObjectAnimator.ofFloat(progressBar, "rotation", 0f, 360f)
        rotateAnimation.duration = 1000
        rotateAnimation.repeatCount = ObjectAnimator.INFINITE
        rotateAnimation.start()
    }
    private fun createBottomSheetView(garages: List<Garage>, selectedGarage: Garage?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_emergency_choose_garage, null)

        setupRecyclerView(view, garages)
        setupConfirmButton(view, selectedGarage)
//        setupCloseButton(view)
        updateTitle(garages.size)

        return view
    }

    private fun setupRecyclerView(view: View, garages: List<Garage>) {
        val rvGarages = view.findViewById<RecyclerView>(R.id.rvGarages)
        garageAdapter = GarageAdapter { garage ->
            viewModel.selectGarage(garage)
            updateConfirmButton(garage)
        }
        rvGarages.layoutManager = LinearLayoutManager(context)
        rvGarages.adapter = garageAdapter
        garageAdapter.submitList(garages)
    }

    private fun setupConfirmButton(view: View, selectedGarage: Garage?) {
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        updateConfirmButton(selectedGarage)
        btnConfirm.setOnClickListener {
            onConfirmClickListener?.invoke()
        }
    }

//    private fun setupCloseButton(view: View) {
//        val btnClose = view.findViewById<Button>(R.id.btnClose)
//        btnClose?.setOnClickListener {
//            dismiss()
//            onDismissClickListener?.invoke()
//        }
//    }

    private fun updateTitle(garageCount: Int) {
        bottomSheetDialog?.findViewById<TextView>(R.id.tvSheetTitle)?.text =
            if (garageCount == 0) "No available garages"
            else "Choose a rescue garage ($garageCount results)"
    }

    private fun updateConfirmButton(garage: Garage?) {
        bottomSheetDialog?.findViewById<Button>(R.id.btnConfirm)?.apply {
            isEnabled = garage != null
            text = if (garage != null) "Confirm - ${garage.price.formatPrice()}"

            else "Select a garage to confirm"
        }
    }

    fun isShowing(): Boolean {
        return bottomSheetDialog?.isShowing == true
    }

    fun lastSelectedGarage(): Garage? {
        return lastGarage
    }

    fun setOnChooseAnotherListener(listener: (() -> Unit)?) {
        onChooseAnotherListener = listener
    }

    fun showRejected(garage: Garage, reason: String?) {
        dismissSilently()
        bottomSheetDialog = BottomSheetDialog(context).apply {
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_rejected, null)
            setContentView(view)
            setCancelable(true)
            val tvName = view.findViewById<TextView>(R.id.tvGarageNameRejected)
            val tvAddr = view.findViewById<TextView>(R.id.tvGarageAddressRejected)
            val tvReason = view.findViewById<TextView>(R.id.tvRejectedReason)
            val btnChoose = view.findViewById<Button>(R.id.btnChooseAnother)
            val btnCall = view.findViewById<Button>(R.id.btnCallGarageRejected)
            tvName.text = garage.name
            tvAddr.text = garage.address
            tvReason.text = reason ?: ""
            btnChoose.setOnClickListener {
                dismiss()
                onChooseAnotherListener?.invoke()
            }
            btnCall.setOnClickListener {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                    intent.data = android.net.Uri.parse("tel:" + (garage.phone ?: ""))
                    (context as Activity).startActivity(intent)
                } catch (_: Exception) {}
            }
            show()
        }
    }

    fun showExpired(garage: Garage) {
        dismissSilently()
        bottomSheetDialog = BottomSheetDialog(context).apply {
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_expired, null)
            setContentView(view)
            setCancelable(true)
            val tvName = view.findViewById<TextView>(R.id.tvGarageNameExpired)
            val tvAddr = view.findViewById<TextView>(R.id.tvGarageAddressExpired)
            val btnChoose = view.findViewById<Button>(R.id.btnChooseAnotherExpired)
            val btnCancel = view.findViewById<Button>(R.id.btnCancelExpired)
            tvName.text = garage.name
            tvAddr.text = garage.address
            btnChoose.setOnClickListener {
                dismiss()
                onChooseAnotherListener?.invoke()
            }
            btnCancel.setOnClickListener {
                viewModel.cancelEmergencyRequest()
                dismiss()
                onDismissClickListener?.invoke()
            }
            show()
        }
    }

    fun showTracking(garage: Garage, etaMinutes: Int?) {
        dismissSilently()
        bottomSheetDialog = BottomSheetDialog(context).apply {
            val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_tracking, null)
            setContentView(view)
            setCancelable(true)
            trackingView = view
            val tvTitle = view.findViewById<TextView>(R.id.tvTrackingTitle)
            val tvSubtitle = view.findViewById<TextView>(R.id.tvTrackingSubtitle)
            val tvTechName = view.findViewById<TextView>(R.id.tvTechNameTracking)
            val tvTechPhone = view.findViewById<TextView>(R.id.tvTechPhoneTracking)
            val btnViewMap = view.findViewById<Button>(R.id.btnViewMap)
            val btnBack = view.findViewById<Button>(R.id.btnBackTracking)
            tvTitle.text = "On the way"
            tvSubtitle.text = etaMinutes?.let { "Technician en route, ETA ~ ${it} min" } ?: "Technician is en route, please track on the map."
            tvTechName.text = "Technician"
            tvTechPhone.text = ""
            
            btnViewMap.setOnClickListener { dismiss(); onViewMapClickListener?.invoke() }
            btnBack.setOnClickListener { showAccepted(garage, etaMinutes) }
            
            show()
        }
    }

    fun updateTrackingEta(minutes: Int) {
        val v = trackingView ?: return
        v.findViewById<TextView>(R.id.tvTrackingSubtitle)?.text = "Technician en route, ETA ~ ${minutes} min"
    }

    fun updateTrackingSkeleton(show: Boolean) {
        val v = trackingView ?: return
        val sc = v.findViewById<View>(R.id.skeletonContainer)
        sc?.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun updateTrackingTechnician(name: String?, phone: String?) {
        val v = trackingView ?: return
        v.findViewById<TextView>(R.id.tvTechNameTracking)?.text = name ?: "Technician"
        val phoneView = v.findViewById<TextView>(R.id.tvTechPhoneTracking)
        phoneView?.text = phone ?: ""
        phoneView?.setOnClickListener {
            dialNumber(phone)
        }
    }

    fun setOnViewMapClickListener(listener: (() -> Unit)?) {
        onViewMapClickListener = listener
    }

    private fun dialNumber(raw: String?) {
        val number = raw?.filter { it.isDigit() || it == '+' } ?: ""
        if (number.isBlank()) {
            return
        }
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
            intent.data = android.net.Uri.parse("tel:$number")
            (context as Activity).startActivity(intent)
        } catch (_: Exception) {}
    }
}
