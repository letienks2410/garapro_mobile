package com.example.garapro.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.garapro.data.remote.ApiService
import com.example.garapro.data.repository.ForgotRepository
import com.example.garapro.databinding.ActivityForgotPasswordBinding
import com.example.garapro.utils.Resource

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var viewModel: ForgotPasswordViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val api = ApiService.create(this)
        val repo = ForgotRepository(api)
        viewModel = ViewModelProvider(
            this,
            ForgotPasswordVMFactory(repo)
        )[ForgotPasswordViewModel::class.java]

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        binding.btnSendOtp.setOnClickListener {
            val phone = binding.edtPhone.text.toString().trim()
            if (phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.sendOtp(phone)
        }
    }

    private fun setupObservers() {
        viewModel.sendOtp.observe(this) { res ->
            when (res) {
                is Resource.Loading -> {
                    // ⬇️ DÙNG TRỰC TIẾP progressBar
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        res.data?.message ?: "OTP sent",
                        Toast.LENGTH_SHORT
                    ).show()

                    val i = Intent(this, VerifyOtpActivity::class.java)
                    i.putExtra("phone", binding.edtPhone.text.toString().trim())
                    startActivity(i)
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, res.message ?: "Lỗi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
