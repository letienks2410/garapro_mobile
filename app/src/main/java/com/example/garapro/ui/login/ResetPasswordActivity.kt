package com.example.garapro.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.garapro.data.remote.ApiService
import com.example.garapro.data.repository.ForgotRepository
import com.example.garapro.databinding.ActivityResetPasswordBinding
import com.example.garapro.utils.Resource

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var viewModel: ForgotPasswordViewModel
    private var phone: String = ""
    private var resetToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        phone = intent.getStringExtra("phone") ?: ""
        resetToken = intent.getStringExtra("resetToken") ?: ""

        val api = ApiService.create(this)
        val repo = ForgotRepository(api)
        viewModel = ViewModelProvider(this, ForgotPasswordVMFactory(repo))[ForgotPasswordViewModel::class.java]

        binding.btnResetPass.setOnClickListener {
            val newPass = binding.edtNewPassword.text.toString().trim()
            val confirm = binding.edtConfirmPassword.text.toString().trim()
            if (newPass.length < 8) {
                Toast.makeText(this, "Mật khẩu ít nhất 8 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPass != confirm) {
                Toast.makeText(this, "Mật khẩu và xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (resetToken.isEmpty()) {
                Toast.makeText(this, "Reset token thiếu", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            viewModel.resetPassword(phone, resetToken, newPass)
        }

        viewModel.resetPwd.observe(this) { res ->
            when (res) {
                is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, res.data?.message ?: "Đổi mật khẩu thành công", Toast.LENGTH_LONG).show()
                    finishAffinity()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, res.message ?: "Lỗi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
