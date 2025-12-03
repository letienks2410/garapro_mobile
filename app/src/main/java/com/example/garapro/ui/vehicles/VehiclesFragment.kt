package com.example.garapro.ui.vehicles

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.Vehicles.*
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.data.repository.ApiResponse
import com.example.garapro.data.repository.VehicleRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class VehiclesFragment : Fragment() {

    companion object {
        private const val PREFS_AUTH = "auth_prefs"
        private const val KEY_USER_ID = "user_id"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var emptyView: TextView
    private lateinit var adapter: VehicleAdapter
    private lateinit var viewModel: VehicleViewModel

    private val vehicleList = mutableListOf<Vehicle>()
    private var isListLoading = false
    private var isActionLoading = false
    private var isVehicleFormDialogVisible = false
    private var lastAction: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_vehicles, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewVehicles)
        fabAdd = view.findViewById(R.id.fabAddVehicle)
        progressIndicator = view.findViewById(R.id.progressCircular)
        emptyView = view.findViewById(R.id.tvEmptyState)

        setupViewModel()
        setupRecyclerView()
        observeViewModel()

        viewModel.fetchVehicles()
        viewModel.fetchAllDropdownData()

        fabAdd.setOnClickListener { showVehicleFormDialog() }
    }

    private fun setupViewModel() {
        val repository = VehicleRepository(RetrofitInstance.vehicleService)
        val factory = VehicleViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[VehicleViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = VehicleAdapter(
            vehicles = vehicleList,
            onItemClick = { vehicle -> showVehicleDetailDialog(vehicle) },
            onEditClick = { vehicle -> showVehicleFormDialog(vehicle) },
            onDeleteClick = { vehicle -> showDeleteConfirmation(vehicle) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.vehicleListStatus.observe(viewLifecycleOwner) { response ->
            when (response) {
                is ApiResponse.Loading -> { isListLoading = true; updateLoadingState() }
                is ApiResponse.Success -> {
                    isListLoading = false
                    updateLoadingState()
                    vehicleList.clear()
                    vehicleList.addAll(response.data)
                    adapter.notifyDataSetChanged()
                    emptyView.isVisible = response.data.isEmpty()
                }
                is ApiResponse.Error -> {
                    isListLoading = false
                    updateLoadingState()
                    emptyView.isVisible = vehicleList.isEmpty()
                    Toast.makeText(
                        requireContext(),
                        response.message ?: "Failed to load vehicles",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewModel.actionStatus.observe(viewLifecycleOwner) { response ->
            when (response) {
                is ApiResponse.Loading -> { isActionLoading = true; updateLoadingState() }
                is ApiResponse.Success -> {
                    isActionLoading = false
                    updateLoadingState()
                    if (!isVehicleFormDialogVisible) {
                        Toast.makeText(requireContext(), "Action succeeded", Toast.LENGTH_SHORT).show()
                    }
                    lastAction = null
                }
                is ApiResponse.Error -> {
                    isActionLoading = false
                    updateLoadingState()
                    if (!isVehicleFormDialogVisible && lastAction != null) {
                        val actionableMsg = extractActionableMessage(response.message.orEmpty())
                        val friendly = actionableMsg ?: if (lastAction == "delete") mapDeleteErrorMessage(response.message.orEmpty()) else (response.message ?: "Action failed")
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(if (lastAction == "delete") "Delete Failed" else "Action Failed")
                            .setMessage(friendly)
                            .setPositiveButton("Close", null)
                            .show()
                        lastAction = null
                    }
                }
            }
        }
    }

    private fun extractActionableMessage(message: String): String? {
        return try {
            val root = JSONObject(message)
            val actionable = root.optBoolean("actionable", false)
            if (actionable && root.has("message")) root.getString("message") else null
        } catch (_: Exception) {
            null
        }
    }

    private fun mapDeleteErrorMessage(message: String): String {
        val m = message.lowercase()
        return when {
            m.contains("409") || m.contains("constraint") || m.contains("foreign key") || m.contains("linked") || m.contains("in use") || m.contains("đang sử dụng") ->
                "Cannot delete vehicle because it's linked to active orders"
            else -> "Delete vehicle failed"
        }
    }

    private fun updateLoadingState() {
        progressIndicator.isVisible = isListLoading || isActionLoading
        fabAdd.isEnabled = !isActionLoading
    }

    private fun showVehicleDetailDialog(vehicle: Vehicle) {
        val message = """
            License Plate: ${vehicle.licensePlate.orEmpty()}
            Brand: ${vehicle.brandName.orEmpty()}
            Model: ${vehicle.modelName.orEmpty()}
            Year: ${vehicle.year ?: "-"}
            Color: ${vehicle.colorName.orEmpty()}
            VIN: ${vehicle.vin.orEmpty()}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Vehicle Details")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .setNeutralButton("Edit") { _, _ -> showVehicleFormDialog(vehicle) }
            .show()
    }

    private fun showDeleteConfirmation(vehicle: Vehicle) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Delete vehicle ${vehicle.licensePlate.orEmpty()}?")
            .setPositiveButton("Delete") { _, _ -> lastAction = "delete"; viewModel.deleteVehicle(vehicle.vehicleID) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVehicleFormDialog(existingVehicle: Vehicle? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_edit_vehicle, null)

        val tilLicensePlate = dialogView.findViewById<TextInputLayout>(R.id.tilLicensePlate)
        val tilBrand = dialogView.findViewById<TextInputLayout>(R.id.tilBrand)
        val tilModel = dialogView.findViewById<TextInputLayout>(R.id.tilModel)
        val tilColor = dialogView.findViewById<TextInputLayout>(R.id.tilColor)
        val tilYear = dialogView.findViewById<TextInputLayout>(R.id.tilYear)
        val tilVin = dialogView.findViewById<TextInputLayout>(R.id.tilVin)

        val etLicensePlate = dialogView.findViewById<TextInputEditText>(R.id.etLicensePlate)
        val etYear = dialogView.findViewById<TextInputEditText>(R.id.etYear)
        val etVin = dialogView.findViewById<TextInputEditText>(R.id.etVin)
        val actBrand = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.actBrand)
        val actModel = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.actModel)
        val actColor = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.actColor)

        existingVehicle?.let {
            etLicensePlate.setText(it.licensePlate.orEmpty())
            etYear.setText(it.year?.toString().orEmpty())
            etVin.setText(it.vin.orEmpty())

            if (it.brandID.isNotBlank()) viewModel.fetchModelsByBrand(it.brandID)
            if (!it.modelID.isNullOrBlank()) viewModel.fetchColorsByModel(it.modelID)
        }

        var currentBrands = viewModel.brands.value.orEmpty()
        var currentModels: List<Model> = emptyList()
        var currentColors: List<ModelColor> = emptyList()

        var selectedBrand: Brand? = null
        var selectedModel: Model? = null
        var selectedColor: ModelColor? = null

        val preSelectedBrandId = existingVehicle?.brandID
        val preSelectedModelId = existingVehicle?.modelID
        val preSelectedColorId = existingVehicle?.colorID

        var hasPrefetchedModels = false
        var hasPrefetchedColors = false

        val brandObserver = Observer<List<Brand>> { brands ->
            currentBrands = brands
            actBrand.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, brands.map { it.name ?: "Unknown" }))
            if (selectedBrand == null && preSelectedBrandId != null) selectedBrand = brands.firstOrNull { it.id == preSelectedBrandId }
            selectedBrand?.let { actBrand.setText(it.name, false); if (!hasPrefetchedModels) { hasPrefetchedModels = true; viewModel.fetchModelsByBrand(it.id) } }
        }

        val modelObserver = Observer<List<Model>> { models ->
            currentModels = models
            actModel.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, models.map { it.name ?: "Unknown" }))
            if (selectedModel == null && preSelectedModelId != null) selectedModel = models.firstOrNull { it.id == preSelectedModelId }
            selectedModel?.let { actModel.setText(it.name, false); if (!hasPrefetchedColors) { hasPrefetchedColors = true; viewModel.fetchColorsByModel(it.id) } }
        }

        val colorObserver = Observer<List<ModelColor>> { colors ->
            currentColors = colors
            actColor.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, colors.map { it.name ?: "Unknown" }))
            if (selectedColor == null && preSelectedColorId != null) selectedColor = colors.firstOrNull { it.id == preSelectedColorId }
            selectedColor?.let { actColor.setText(it.name, false) }
        }

        viewModel.brands.observe(viewLifecycleOwner, brandObserver)
        viewModel.models.observe(viewLifecycleOwner, modelObserver)
        viewModel.colors.observe(viewLifecycleOwner, colorObserver)

        actBrand.setOnItemClickListener { _, _, pos, _ ->
            tilBrand.error = null
            selectedBrand = currentBrands.getOrNull(pos)
            selectedModel = null; selectedColor = null
            actModel.setText(""); actColor.setText("")
            selectedBrand?.let { viewModel.fetchModelsByBrand(it.id) }
        }

        actModel.setOnItemClickListener { _, _, pos, _ ->
            tilModel.error = null
            selectedModel = currentModels.getOrNull(pos)
            selectedColor = null; actColor.setText("")
            selectedModel?.let { viewModel.fetchColorsByModel(it.id) }
        }

        actColor.setOnItemClickListener { _, _, pos, _ ->
            tilColor.error = null
            selectedColor = currentColors.getOrNull(pos)
        }

        var waitingForAction = false
        var actionObserver: Observer<ApiResponse<Unit>>? = null
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingVehicle == null) "Add Vehicle" else "Edit Vehicle")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Close", null)
            .setPositiveButton(if (existingVehicle == null) "Add" else "Save", null)
            .create()
        dialog.setCanceledOnTouchOutside(true)

        dialog.setOnShowListener {
            isVehicleFormDialogVisible = true
            val positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positive.setOnClickListener {
                tilLicensePlate.error = null; tilBrand.error = null; tilModel.error = null
                tilColor.error = null; tilYear.error = null; tilVin.error = null

                val licensePlate = etLicensePlate.text?.toString()?.trim().orEmpty()
                val yearValue = etYear.text?.toString()?.toIntOrNull()
                val vinValue = etVin.text?.toString()?.trim().orEmpty()

                val platePattern = Regex("^[A-Za-z0-9\\-\\.\\s]{5,15}$")
                when {
                    licensePlate.isBlank() -> { tilLicensePlate.error = "Please enter license plate"; return@setOnClickListener }
                    !platePattern.matches(licensePlate) -> { tilLicensePlate.error = "Invalid license format. e.g., 51A-12345"; return@setOnClickListener }
                    selectedBrand == null -> { tilBrand.error = "Please select a brand"; return@setOnClickListener }
                    selectedModel == null -> { tilModel.error = "Please select a model"; return@setOnClickListener }
                    selectedColor == null -> { tilColor.error = "Please select a color"; return@setOnClickListener }
                    yearValue == null || yearValue !in 1900..2050 -> { tilYear.error = "Invalid production year"; return@setOnClickListener }
                    vinValue.isBlank() -> { tilVin.error = "Please enter VIN"; return@setOnClickListener }
                }

                val userId = getCurrentUserId()
                if (userId.isNullOrBlank()) { Toast.makeText(requireContext(), "User info not found", Toast.LENGTH_LONG).show(); return@setOnClickListener }

                if (existingVehicle == null) {
                    val request = CreateVehicles(
                        brandID = selectedBrand!!.id,
                        modelID = selectedModel!!.id,
                        colorID = selectedColor!!.id,
                        userID = userId,
                        licensePlate = licensePlate,
                        vin = vinValue,
                        year = yearValue,
                        odometer = null
                    )
                    waitingForAction = true
                    viewModel.createVehicle(request)
                } else {
                    val request = UpdateVehicles(
                        brandID = selectedBrand!!.id,
                        modelID = selectedModel!!.id,
                        colorID = selectedColor!!.id,
                        licensePlate = licensePlate,
                        vin = vinValue,
                        year = yearValue,
                        odometer = existingVehicle.odometer
                    )
                    waitingForAction = true
                    viewModel.updateVehicle(existingVehicle.vehicleID, request)
                }
                positive.isEnabled = false
            }

            actionObserver = Observer<ApiResponse<Unit>> { resp ->
                if (!waitingForAction) return@Observer
                when (resp) {
                    is ApiResponse.Loading -> { positive.isEnabled = false }
                    is ApiResponse.Success -> { waitingForAction = false; positive.isEnabled = true; dialog.dismiss() }
                    is ApiResponse.Error -> {
                        waitingForAction = false
                        positive.isEnabled = true
                        val msg = resp.message
                        var handled = false
                        try {
                            val root = JSONObject(msg)
                            val actionable = root.optBoolean("actionable", false)
                            if (actionable && root.has("message")) {
                                val m = root.getString("message")
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Conflict")
                                    .setMessage(m)
                                    .setPositiveButton("Close", null)
                                    .show()
                                val ml = m.lowercase()
                                if (ml.contains("vin")) { tilVin.error = "VIN already exists"; handled = true }
                                else if (ml.contains("license") || ml.contains("biển")) { tilLicensePlate.error = "License plate already exists"; handled = true }
                            }
                            if (root.has("errors")) {
                                val errors = root.getJSONObject("errors")
                                fun firstError(key: String): String? = if (errors.has(key)) {
                                    val arr = errors.getJSONArray(key)
                                    if (arr.length() > 0) arr.getString(0) else null
                                } else null
                                val brandErr = firstError("BrandID")
                                val modelErr = firstError("ModelID")
                                val colorErr = firstError("ColorID")
                                val plateErr = firstError("LicensePlate")
                                val vinErr = firstError("VIN")
                                val yearErr = firstError("Year")
                                val odoErr = firstError("Odometer")
                                if (!brandErr.isNullOrBlank()) { tilBrand.error = "Please select a valid brand"; handled = true }
                                if (!modelErr.isNullOrBlank()) { tilModel.error = "Please select a valid model"; handled = true }
                                if (!colorErr.isNullOrBlank()) { tilColor.error = "Please select a valid color"; handled = true }
                                if (!plateErr.isNullOrBlank()) {
                                    val pLower = plateErr.lowercase()
                                    if (pLower.contains("exist") || pLower.contains("duplicate") || pLower.contains("tồn tại") || pLower.contains("đã tồn tại") || pLower.contains("already")) {
                                        tilLicensePlate.error = "License plate already exists"; handled = true
                                    } else {
                                        tilLicensePlate.error = "Invalid license plate"; handled = true
                                    }
                                }
                                if (!vinErr.isNullOrBlank()) {
                                    val vLower = vinErr.lowercase()
                                    if (vLower.contains("exist") || vLower.contains("duplicate") || vLower.contains("đã tồn tại") || vLower.contains("tồn tại") || vLower.contains("already")) {
                                        tilVin.error = "VIN already exists"; handled = true
                                    } else {
                                        tilVin.error = "Invalid VIN"; handled = true
                                    }
                                }
                                if (!yearErr.isNullOrBlank()) { tilYear.error = "Invalid production year"; handled = true }
                                if (!odoErr.isNullOrBlank()) { tilYear.error = "Invalid odometer"; handled = true }
                            }
                        } catch (_: Exception) {}
                        if (!handled) {
                            val lower = msg.lowercase()
                            if ((lower.contains("vin") && (lower.contains("exist") || lower.contains("duplicate") || lower.contains("đã tồn tại") || lower.contains("tồn tại") || lower.contains("already")))) {
                                tilVin.error = "VIN already exists"
                            } else if ((lower.contains("license") || lower.contains("licenseplate")) && (lower.contains("exist") || lower.contains("duplicate") || lower.contains("đã tồn tại") || lower.contains("tồn tại") || lower.contains("already") || lower.contains("409"))) {
                                tilLicensePlate.error = "License plate already exists"
                            } else if (lower.contains("brandid") || lower.contains("brand")) tilBrand.error = "Please select a valid brand"
                            else if (lower.contains("modelid") || lower.contains("model")) tilModel.error = "Please select a valid model"
                            else if (lower.contains("colorid") || lower.contains("color")) tilColor.error = "Please select a valid color"
                            else if (lower.contains("licenseplate") || lower.contains("biển")) tilLicensePlate.error = "Invalid license plate"
                            else if (lower.contains("vin")) tilVin.error = "Invalid VIN"
                            else if (lower.contains("year") || lower.contains("năm")) tilYear.error = "Invalid production year"
                            else tilLicensePlate.error = "Invalid information. Please check again"
                        }
                    }
                }
            }
            viewModel.actionStatus.observe(viewLifecycleOwner, actionObserver!!)
        }

        dialog.setOnDismissListener {
            isVehicleFormDialogVisible = false
            viewModel.brands.removeObserver(brandObserver)
            viewModel.models.removeObserver(modelObserver)
            viewModel.colors.removeObserver(colorObserver)
            actionObserver?.let { viewModel.actionStatus.removeObserver(it) }
            fabAdd.isEnabled = true
        }

        dialog.show()
    }

    private fun getCurrentUserId(): String? {
        val prefs = requireContext().getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null)
    }
}
