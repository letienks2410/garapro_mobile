package com.example.garapro.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.garapro.data.remote.ApiService
import com.example.garapro.data.repository.ForgotRepository
import com.example.garapro.databinding.ActivityVerifyOtpBinding
import com.example.garapro.utils.Resource

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyOtpBinding
    private lateinit var viewModel: ForgotPasswordViewModel
    private var phone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        phone = intent.getStringExtra("phone") ?: ""

        val api = ApiService.create(this)
        val repo = ForgotRepository(api)
        viewModel = ViewModelProvider(this, ForgotPasswordVMFactory(repo))[ForgotPasswordViewModel::class.java]

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.edtOtp.text.toString().trim()
            if (otp.isEmpty()) {
                Toast.makeText(this, "Nhập OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.verifyOtp(phone, otp)
        }

        viewModel.verifyOtp.observe(this) { res ->
            when (res) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val resetToken = res.data?.resetToken
                    if (resetToken.isNullOrEmpty()) {
                        // Nếu backend trả token trong message, kiểm tra logs / server response
                        Toast.makeText(this, res.data?.message ?: "Không nhận được reset token", Toast.LENGTH_LONG).show()
                        return@observe
                    }
                    val i = Intent(this, ResetPasswordActivity::class.java)
                    i.putExtra("phone", phone)
                    i.putExtra("resetToken", resetToken)
                    startActivity(i)
                    finish()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, res.message ?: "Xác thực thất bại", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
