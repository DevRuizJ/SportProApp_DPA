package com.dpa.sportpro.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.repository.AuthRepository
import com.dpa.sportpro.data.repository.AuthRepositoryImpl
import com.dpa.sportpro.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository = AuthRepositoryImpl()) :
    ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, globalError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, globalError = null) }
    }

    fun login() {
        val state = _uiState.value

        var isValid = true
        var emailErr: String? = null
        var passwordErr: String? = null

        if (state.email.isBlank()) {
            isValid = false
            emailErr = "Ingrese su correo electrónico."
        } else if (!isValidEmail(state.email)) {
            isValid = false
            emailErr = "Ingrese un correo electrónico válido."
        }

        if (state.password.isBlank()) {
            isValid = false
            passwordErr = "Ingrese su contraseña."
        }

        if (!isValid) {
            _uiState.update {
                it.copy(
                    emailError = emailErr,
                    passwordError = passwordErr,
                    globalError = "Correo electrónico o contraseña incorrectos.",
                )
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, globalError = null) }

        viewModelScope.launch {
            when (val result = authRepository.login(state.email, state.password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            authenticatedUser = result.userProfile,
                            globalError = null,
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, globalError = result.message) }
                }
            }
        }
    }

    fun clearAuthenticationState() {
        _uiState.update { it.copy(authenticatedUser = null) }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\$"
        return email.matches(emailRegex.toRegex())
    }
}
