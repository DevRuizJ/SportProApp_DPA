package com.dpa.sportpro.ui.register

import com.dpa.sportpro.model.UserRole

data class RegisterUiState(
    val selectedRole: UserRole? = UserRole.PLAYER,
    val names: String = "",
    val lastNames: String = "",
    val email: String = "",
    val birthDate: String = "",
    val birthDateMillis: Long? = null,
    val isMinor: Boolean = false,
    val password: String = "",
    val confirmPassword: String = "",

    val roleError: String? = null,
    val namesError: String? = null,
    val lastNamesError: String? = null,
    val emailError: String? = null,
    val birthDateError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,

    val registrationSuccess: Boolean = false,
    val successMessage: String? = null
)
