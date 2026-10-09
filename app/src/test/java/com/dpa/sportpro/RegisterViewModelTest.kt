package com.dpa.sportpro

import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.register.RegisterViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class RegisterViewModelTest {

    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        viewModel = RegisterViewModel()
    }

    @Test
    fun initialRoleIsPlayerByDefault() {
        val state = viewModel.uiState.value
        assertEquals(UserRole.PLAYER, state.selectedRole)
        assertTrue(state.selectedRole!!.requiresBirthDate)
    }

    @Test
    fun roleSelectionUpdatesSelectedRole() {
        viewModel.onRoleSelected(UserRole.COACH)
        assertEquals(UserRole.COACH, viewModel.uiState.value.selectedRole)
        assertFalse(viewModel.uiState.value.selectedRole!!.requiresBirthDate)
    }

    @Test
    fun registrationFailsWhenMandatoryFieldsAreEmpty() {
        viewModel.register()
        val state = viewModel.uiState.value

        assertFalse(state.registrationSuccess)
        assertNotNull(state.namesError)
        assertNotNull(state.lastNamesError)
        assertNotNull(state.emailError)
        assertNotNull(state.birthDateError)
        assertNotNull(state.passwordError)
        assertNotNull(state.confirmPasswordError)
    }

    @Test
    fun passwordValidationEnforcesLengthUppercaseAndNumber() {
        viewModel.onNamesChanged("Mateo")
        viewModel.onLastNamesChanged("Silva")
        viewModel.onEmailChanged("mateo@test.com")
        viewModel.onRoleSelected(UserRole.COACH)

        viewModel.onPasswordChanged("Pass1")
        viewModel.onConfirmPasswordChanged("Pass1")
        viewModel.register()
        assertNotNull(viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")
        viewModel.register()
        assertNotNull(viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("PasswordTest")
        viewModel.onConfirmPasswordChanged("PasswordTest")
        viewModel.register()
        assertNotNull(viewModel.uiState.value.passwordError)

        viewModel.onPasswordChanged("Password123")
        viewModel.onConfirmPasswordChanged("Password123")
        viewModel.register()
        assertNull(viewModel.uiState.value.passwordError)
        assertTrue(viewModel.uiState.value.registrationSuccess)
    }

    @Test
    fun minorCalculationSetsIsMinorCorrectly() {
        val minorCalendar = Calendar.getInstance().apply {
            add(Calendar.YEAR, -14)
        }
        viewModel.onBirthDateSelected(minorCalendar.timeInMillis)
        assertTrue(viewModel.uiState.value.isMinor)

        val adultCalendar = Calendar.getInstance().apply {
            add(Calendar.YEAR, -20)
        }
        viewModel.onBirthDateSelected(adultCalendar.timeInMillis)
        assertFalse(viewModel.uiState.value.isMinor)
    }

    @Test
    fun successfulRegistrationSetsSuccessMessageAndFlag() {
        val minorCalendar = Calendar.getInstance().apply {
            add(Calendar.YEAR, -15)
        }

        viewModel.onRoleSelected(UserRole.PLAYER)
        viewModel.onNamesChanged("Mateo")
        viewModel.onLastNamesChanged("Silva")
        viewModel.onEmailChanged("mateo.silva@sportpro.com")
        viewModel.onBirthDateSelected(minorCalendar.timeInMillis)
        viewModel.onPasswordChanged("SportPro2026")
        viewModel.onConfirmPasswordChanged("SportPro2026")

        viewModel.register()

        val state = viewModel.uiState.value
        assertTrue(state.registrationSuccess)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("Jugador"))
        assertTrue(state.successMessage!!.contains("Menor de edad"))
    }
}
