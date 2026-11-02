package com.dpa.sportpro.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.repository.AccountActionResult
import com.dpa.sportpro.data.repository.AccountSecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AccountAction { RESET_PASSWORD, CHANGE_PASSWORD, CHANGE_EMAIL }

data class AccountSecurityUiState(
    val action: AccountAction = AccountAction.CHANGE_PASSWORD,
    val email: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    val submitting: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class AccountSecurityViewModel(
    private val repository: AccountSecurityRepository = AccountSecurityRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountSecurityUiState())
    val uiState = mutableState.asStateFlow()

    fun select(action: AccountAction) {
        if (!mutableState.value.submitting) mutableState.value = AccountSecurityUiState(action = action)
    }
    fun email(value: String) = edit { copy(email = value) }
    fun currentPassword(value: String) = edit { copy(currentPassword = value) }
    fun newPassword(value: String) = edit { copy(newPassword = value) }
    fun confirmation(value: String) = edit { copy(confirmation = value) }
    private fun edit(transform: AccountSecurityUiState.() -> AccountSecurityUiState) {
        if (!mutableState.value.submitting && mutableState.value.message == null) {
            mutableState.value = mutableState.value.transform().copy(error = null)
        }
    }

    fun submit() {
        val snapshot = mutableState.value
        if (snapshot.submitting || snapshot.message != null) return
        mutableState.value = snapshot.copy(submitting = true, error = null)
        viewModelScope.launch {
            try {
                val result = when (snapshot.action) {
                    AccountAction.RESET_PASSWORD -> repository.resetPassword(snapshot.email)
                    AccountAction.CHANGE_PASSWORD -> repository.changePassword(
                        snapshot.currentPassword, snapshot.newPassword, snapshot.confirmation)
                    AccountAction.CHANGE_EMAIL -> repository.changeEmail(snapshot.currentPassword, snapshot.email)
                }
                // Never persist credentials in SavedStateHandle, disk or analytics.
                mutableState.value = mutableState.value.copy(
                    currentPassword = "", newPassword = "", confirmation = "",
                    message = (result as? AccountActionResult.Success)?.message,
                    error = (result as? AccountActionResult.Error)?.message,
                )
            } finally {
                mutableState.value = mutableState.value.copy(submitting = false)
            }
        }
    }
}
