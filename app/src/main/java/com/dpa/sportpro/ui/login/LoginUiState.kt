package com.dpa.sportpro.ui.login

import com.dpa.sportpro.data.model.UserProfile

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val globalError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val authenticatedUser: UserProfile? = null
)
