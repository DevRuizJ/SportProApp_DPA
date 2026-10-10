package com.dpa.sportpro.ui.register

import androidx.lifecycle.ViewModel
import com.dpa.sportpro.model.UserRole
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onRoleSelected(role: UserRole) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedRole = role,
                roleError = null,
                // Clear birth date error if switching to a role that doesn't require it
                birthDateError = if (!role.requiresBirthDate) null else currentState.birthDateError,
            )
        }
    }

    fun onNamesChanged(names: String) {
        _uiState.update { it.copy(names = names, namesError = null) }
    }

    fun onLastNamesChanged(lastNames: String) {
        _uiState.update { it.copy(lastNames = lastNames, lastNamesError = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onBirthDateSelected(timeInMillis: Long) {
        val dateFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
        val formattedDate = dateFormat.format(Date(timeInMillis))

        val birthCalendar = Calendar.getInstance().apply { setTimeInMillis(timeInMillis) }
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        val isMinor = age < 18

        _uiState.update {
            it.copy(
                birthDate = formattedDate,
                birthDateMillis = timeInMillis,
                isMinor = isMinor,
                birthDateError = null,
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun register() {
        val state = _uiState.value

        var isValid = true

        val roleError =
            if (state.selectedRole == null) {
                isValid = false
                "Debe seleccionar un rol."
            } else null

        val namesError =
            if (state.names.isBlank()) {
                isValid = false
                "Ingrese sus nombres."
            } else null

        val lastNamesError =
            if (state.lastNames.isBlank()) {
                isValid = false
                "Ingrese sus apellidos."
            } else null

        val emailError =
            if (state.email.isBlank()) {
                isValid = false
                "Ingrese un correo electrónico."
            } else if (!isValidEmail(state.email)) {
                isValid = false
                "Ingrese un correo electrónico válido."
            } else null

        val birthDateError =
            if (state.selectedRole?.requiresBirthDate == true && state.birthDate.isBlank()) {
                isValid = false
                "Debe seleccionar su fecha de nacimiento."
            } else null

        val passwordError =
            if (state.password.isBlank()) {
                isValid = false
                "Ingrese una contraseña."
            } else if (!isValidPassword(state.password)) {
                isValid = false
                "Mínimo 8 caracteres, al menos una mayúscula y un número."
            } else null

        val confirmPasswordError =
            if (state.confirmPassword.isBlank()) {
                isValid = false
                "Confirme su contraseña."
            } else if (state.password != state.confirmPassword) {
                isValid = false
                "Las contraseñas no coinciden."
            } else null

        _uiState.update {
            it.copy(
                roleError = roleError,
                namesError = namesError,
                lastNamesError = lastNamesError,
                emailError = emailError,
                birthDateError = birthDateError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError,
            )
        }

        if (isValid) {
            val roleInfo = state.selectedRole?.displayName ?: ""
            val minorDetail =
                if (state.selectedRole?.requiresBirthDate == true && state.isMinor) {
                    " (Menor de edad - Se habilitará la vinculación con apoderado)"
                } else ""

            val successMsg =
                "¡Registro exitoso para $roleInfo! Bienvenido(a), ${state.names}.$minorDetail"

            _uiState.update { it.copy(registrationSuccess = true, successMessage = successMsg) }
        }
    }

    fun dismissSuccessDialog() {
        _uiState.update { it.copy(registrationSuccess = false, successMessage = null) }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\$"
        return email.matches(emailRegex.toRegex())
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isDigit() }) return false
        return true
    }
}
