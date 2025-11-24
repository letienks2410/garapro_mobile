package com.example.garapro.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.garapro.data.model.LoginResponse
import com.example.garapro.data.repository.AuthRepository
import com.example.garapro.utils.Resource
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<LoginResponse>>()
    val loginState: LiveData<Resource<LoginResponse>> = _loginState
    private val _googleLoginState = MutableLiveData<LoginUiState>()
    val googleLoginState: LiveData<LoginUiState> = _googleLoginState
    fun login(email: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            repository.login(email, password, rememberMe).collect { result ->
                _loginState.value = result
            }
        }
    }
    fun loginWithGoogle(idToken: String) {
        _googleLoginState.value = LoginUiState(isLoading = true)

        viewModelScope.launch {
            val result = repository.loginWithGoogle(idToken)
            _googleLoginState.value = result.fold(
                onSuccess = { response ->
                    LoginUiState(
                        isLoading = false,
                        authResponse = response
                    )
                },
                onFailure = { throwable ->
                    LoginUiState(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Google login failed"
                    )
                }
            )
        }
    }

    fun validateInput(email: String, password: String): String? {
        return when {
            email.isEmpty() -> "Email không được để trống"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Email không hợp lệ"
            password.isEmpty() -> "Mật khẩu không được để trống"
            password.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự"
            else -> null
        }
    }
}
data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authResponse: LoginResponse? = null
)