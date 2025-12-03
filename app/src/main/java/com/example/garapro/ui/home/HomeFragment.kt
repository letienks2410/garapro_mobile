package com.example.garapro.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
//import androidx.compose.material3.Button
import androidx.fragment.app.Fragment
import com.example.garapro.R
import com.example.garapro.ui.emergencies.MapActivity
import com.example.garapro.ui.emergencies.EmergencyListActivity
import androidx.lifecycle.lifecycleScope
import com.example.garapro.data.remote.RetrofitInstance
import com.example.garapro.ui.repairRequest.BookingActivity

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val btnGoToBooking = view.findViewById<Button>(R.id.btnGoToBooking)
        btnGoToBooking.setOnClickListener {
            val intent = Intent(requireContext(), BookingActivity::class.java)
            startActivity(intent)
        }
        val btnGoToMap = view.findViewById<Button>(R.id.btnGoToMap)
        btnGoToMap.setOnClickListener {
            val userPrefs = requireContext().getSharedPreferences(com.example.garapro.utils.Constants.USER_PREFERENCES, android.content.Context.MODE_PRIVATE)
            val authPrefs = requireContext().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            val userId = userPrefs.getString("user_id", null) ?: authPrefs.getString("user_id", null)
            if (userId.isNullOrBlank()) {
                startActivity(Intent(requireContext(), MapActivity::class.java))
            } else {
                viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                    try {
                        val resp = RetrofitInstance.emergencyService.getEmergenciesByCustomer(userId)
                        if (resp.isSuccessful && (resp.body()?.isNotEmpty() == true)) {
                            startActivity(Intent(requireContext(), EmergencyListActivity::class.java))
                        } else {
                            startActivity(Intent(requireContext(), MapActivity::class.java))
                        }
                    } catch (_: Exception) {
                        startActivity(Intent(requireContext(), MapActivity::class.java))
                    }
                }
            }
        }

        return view
    }
}
