package com.dpa.sportpro

import com.dpa.sportpro.data.repository.AuthRepositoryImpl
import com.dpa.sportpro.data.repository.RegistrationRequest
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.login.LoginViewModel
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
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = AuthRepositoryImpl(TestIdentity(), TestProfiles())
    private lateinit var viewModel: LoginViewModel
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        viewModel = LoginViewModel(repository)
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun emptyCredentialsAreRejected() {
        viewModel.login()
        assertNotNull(viewModel.uiState.value.emailError)
        assertNotNull(viewModel.uiState.value.passwordError)
    }
    @Test fun incorrectCredentialsDoNotNavigate() = runTest {
        viewModel.onEmailChanged("new@example.com")
        viewModel.onPasswordChanged("wrong-password")
        viewModel.login()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.globalError)
        assertNull(viewModel.uiState.value.authenticatedUser)
    }
    @Test fun newlyRegisteredCoachCanLogInWithArbitraryEmail() = runTest {
        repository.register(RegistrationRequest("Lucía", "Torres", "lucia@example.com", "Password123", UserRole.COACH, null))
        viewModel.onEmailChanged("lucia@example.com")
        viewModel.onPasswordChanged("Password123")
        viewModel.login()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(UserRole.COACH, viewModel.uiState.value.authenticatedUser?.role)
        assertEquals("Lucía", viewModel.uiState.value.authenticatedUser?.names)
        assertNull(viewModel.uiState.value.globalError)
    }
}
