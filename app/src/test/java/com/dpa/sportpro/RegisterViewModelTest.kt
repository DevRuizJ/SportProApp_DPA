package com.dpa.sportpro

import com.dpa.sportpro.data.repository.AuthRepositoryImpl
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.register.RegisterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val profiles = TestProfiles()
    private val identity = TestIdentity()
    private lateinit var viewModel: RegisterViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        viewModel = RegisterViewModel(AuthRepositoryImpl(identity, profiles))
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    private fun fill() {
        viewModel.onRoleSelected(UserRole.COACH)
        viewModel.onNamesChanged("Ana")
        viewModel.onLastNamesChanged("Torres")
        viewModel.onEmailChanged("ana@example.com")
        viewModel.onPasswordChanged("Password123")
        viewModel.onConfirmPasswordChanged("Password123")
    }

    @Test fun mandatoryFieldsAreValidated() {
        viewModel.register()
        assertNotNull(viewModel.uiState.value.namesError)
        assertNotNull(viewModel.uiState.value.birthDateError)
        assertFalse(viewModel.uiState.value.registrationSuccess)
    }

    @Test fun confirmationOnlyAppearsAfterRepositoryFinishes() = runTest {
        fill()
        viewModel.register()
        assertTrue(viewModel.uiState.value.isSubmitting)
        assertFalse(viewModel.uiState.value.registrationSuccess)
        viewModel.register() // A second tap must not start a second signup.
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.registrationSuccess)
        assertEquals(1, identity.accounts.size)
        assertEquals(1, profiles.entries.size)
        assertEquals("", viewModel.uiState.value.password)
    }

    @Test fun backendFailureNeverShowsSuccessfulRegistration() = runTest {
        fill()
        profiles.failWrites = true
        viewModel.register()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.registrationSuccess)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.emailError)
    }

    @Test fun weakPasswordIsRejectedBeforeBackendCall() {
        fill()
        viewModel.onPasswordChanged("password")
        viewModel.onConfirmPasswordChanged("password")
        viewModel.register()
        assertNotNull(viewModel.uiState.value.passwordError)
        assertTrue(identity.accounts.isEmpty())
    }

    @Test fun futureDateIsRejected() {
        fill()
        viewModel.onRoleSelected(UserRole.PLAYER)
        viewModel.onBirthDateSelected(System.currentTimeMillis() + 86400000L)
        viewModel.register()
        assertNotNull(viewModel.uiState.value.birthDateError)
    }
}
