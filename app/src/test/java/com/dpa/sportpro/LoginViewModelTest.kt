package com.dpa.sportpro

import com.dpa.sportpro.data.repository.AuthRepository
import com.dpa.sportpro.data.repository.AuthRepositoryImpl
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.login.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = AuthRepositoryImpl()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginWithEmptyFieldsSetsErrorMessages() {
        viewModel.login()
        val state = viewModel.uiState.value

        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertNotNull(state.globalError)
    }

    @Test
    fun loginWithInvalidEmailFormatSetsError() {
        viewModel.onEmailChanged("invalid-email")
        viewModel.onPasswordChanged("SportPro123")
        viewModel.login()

        val state = viewModel.uiState.value
        assertNotNull(state.emailError)
    }

    @Test
    fun loginWithIncorrectCredentialsSetsGlobalErrorBannerMessage() = runTest {
        viewModel.onEmailChanged("wrong@sportpro.com")
        viewModel.onPasswordChanged("wrongpassword")
        viewModel.login()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.globalError)
        assertEquals("Correo electrónico o contraseña incorrectos.", state.globalError)
        assertNull(state.authenticatedUser)
    }

    @Test
    fun loginWithValidCoachCredentialsNavigatesToCoachRoleHome() = runTest {
        viewModel.onEmailChanged("dt@sportpro.com")
        viewModel.onPasswordChanged("SportPro123")
        viewModel.login()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.globalError)
        assertNotNull(state.authenticatedUser)
        assertEquals(UserRole.COACH, state.authenticatedUser?.role)
    }
}
