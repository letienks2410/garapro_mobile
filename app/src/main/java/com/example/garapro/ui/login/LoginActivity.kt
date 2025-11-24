package com.example.garapro.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.garapro.MainActivity
import com.example.garapro.R
import com.example.garapro.data.local.TokenManager
import com.example.garapro.ui.repairRequest.BookingActivity
import com.example.garapro.data.remote.ApiService
import com.example.garapro.data.repository.AuthRepository
import com.example.garapro.databinding.ActivityLoginBinding

import com.example.garapro.ui.home.HomeActivity
import com.example.garapro.ui.signup.SignUpActivity
import com.example.garapro.utils.Resource
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    private val RC_SIGN_IN = 1001
    companion object {
        private const val PREFS_AUTH = "auth_prefs"
        private const val KEY_USER_ID = "user_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Khởi tạo ViewModel
        val apiService = ApiService.create(this)
        val tokenManager = TokenManager(this)
        val repository = AuthRepository(apiService, tokenManager)
        viewModel = LoginViewModel(repository)

        setupGoogleSignIn()
        setupObservers()
        setupListeners()

        val btnLoginGoogle: MaterialButton = findViewById(R.id.btnLoginGoogle)
        btnLoginGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // lấy từ google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken

                Log.d("GoogleSignIn", "account = $account")
                Log.d("GoogleSignIn", "idToken = ${idToken?.take(25)}...")

                if (idToken != null) {
                    // Nếu log này hiện -> chắc chắn đã qua ViewModel
                    Log.d("GoogleSignIn", "Calling viewModel.loginWithGoogle")
                    viewModel.loginWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Failed to get Google token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "signInResult:failed code=${e.statusCode}", e)
                Toast.makeText(this, "Google sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.textPhoneLogin.text.toString().trim()
            val password = binding.textPasswordLogin.text.toString().trim()

            // Validate input
//            val validationError = viewModel.validateInput(email, password)
//            if (validationError != null) {
//                Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }

            // Thực hiện đăng nhập
            viewModel.login(email, password)
        }

        binding.textViewSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { result ->
            when (result) {
                is Resource.Loading -> {

                }

                is Resource.Success -> {

                    val loginResponse = result.data

                    if (loginResponse != null) {
                        //  Lưu userId vào SharedPreferences
                        val prefs = getSharedPreferences(PREFS_AUTH, MODE_PRIVATE)
                        prefs.edit()
                            .putString(KEY_USER_ID, loginResponse.userId)
                            .apply()
                    }
                    Toast.makeText(
                        this,
                        "Đăng nhập thành công!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Chuyển sang màn hình Home
                    navigateToHome()
                }

                is Resource.Error -> {

                    Toast.makeText(
                        this,
                        result.message ?: "Đăng nhập thất bại",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        viewModel.googleLoginState.observe(this) { state ->
            if (state.isLoading) {
                // TODO: show loading
            } else {
                // TODO: hide loading
            }

            state.errorMessage?.let { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }

            state.authResponse?.let { auth ->

                // Lưu userId giống login thường (nếu muốn)
                val prefs = getSharedPreferences(PREFS_AUTH, MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_USER_ID, auth.userId)
                    .apply()

                Toast.makeText(
                    this,
                    "Login with Google successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Điều hướng sang Home / Main
                navigateToHome()
                // Hoặc nếu bạn muốn vào HomeActivity:
                // startActivity(Intent(this, HomeActivity::class.java))
                // finish()
            }
        }
    }



    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}