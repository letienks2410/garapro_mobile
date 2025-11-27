package com.example.garapro.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.garapro.data.local.TokenManager
import com.example.garapro.data.remote.ApiService
import com.example.garapro.data.repository.UserRepository
import com.example.garapro.databinding.ActivityChangePasswordBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var repository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tokenManager = TokenManager(this)
        val apiService = ApiService.create(this, tokenManager)
        repository = UserRepository(apiService)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnChangePassword.setOnClickListener { changePassword() }
    }

    private fun changePassword() {
        val current = binding.edtOldPassword.text.toString().trim()
        val newPass = binding.edtNewPassword.text.toString().trim()
        val confirm = binding.edtConfirmPassword.text.toString().trim()

        // Validation
        if (current.isEmpty()) {
            binding.oldPasswordLayout.error = "Enter current password"
            return
        } else binding.oldPasswordLayout.error = null

        if (newPass.isEmpty()) {
            binding.newPasswordLayout.error = "Enter new password"
            return
        } else binding.newPasswordLayout.error = null

        if (newPass.length < 6) {
            binding.newPasswordLayout.error = "Password must be at least 6 characters"
            return
        }

        if (newPass != confirm) {
            binding.confirmPasswordLayout.error = "Passwords do not match"
            return
        } else binding.confirmPasswordLayout.error = null

        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        binding.btnChangePassword.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val response = repository.changePassword(current, newPass, confirm)

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.btnChangePassword.isEnabled = true

                if (response.data != null) {
                    Toast.makeText(this@ChangePasswordActivity, "Password changed successfully", Toast.LENGTH_LONG).show()
                    finish()
                } else {

                    Toast.makeText(this@ChangePasswordActivity, response.message ?: "Failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
