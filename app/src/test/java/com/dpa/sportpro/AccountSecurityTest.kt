package com.dpa.sportpro

import com.dpa.sportpro.data.repository.*
import com.dpa.sportpro.ui.account.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSecurityTest {
    private val dispatcher = StandardTestDispatcher()
    private val gateway = FakeSecurityGateway()
    private val repository = AccountSecurityRepository(gateway)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun invalidEmailDoesNotSendAnything() = runTest {
        assertTrue(repository.resetPassword("bad") is AccountActionResult.Error)
        assertEquals(0, gateway.calls)
    }
    @Test fun resetNormalizesEmailAndDoesNotExposeAccountExistence() = runTest {
        val result = repository.resetPassword("  Ana@Example.com ") as AccountActionResult.Success
        assertEquals("ana@example.com", gateway.email)
        assertTrue(result.message.startsWith("Si existe una cuenta"))
    }
    @Test fun mismatchedOrWeakPasswordNeverReachesFirebase() = runTest {
        assertTrue(repository.changePassword("Actual123", "Nueva1234", "Otra1234") is AccountActionResult.Error)
        assertTrue(repository.changePassword("Actual123", "weak", "weak") is AccountActionResult.Error)
        assertTrue(repository.changePassword("", "Nueva1234", "Nueva1234") is AccountActionResult.Error)
        assertEquals(0, gateway.calls)
    }
    @Test fun emailChangeRequiresCurrentPassword() = runTest {
        assertTrue(repository.changeEmail("", "new@example.com") is AccountActionResult.Error)
        assertEquals(0, gateway.calls)
    }
    @Test fun emailRequestDoesNotPromiseAnImmediateChange() = runTest {
        val result = repository.changeEmail("Actual123", " new@example.com ") as AccountActionResult.Success
        assertTrue(result.message.contains("Hasta verificarlo"))
        assertEquals("new@example.com", gateway.email)
    }
    @Test fun failedBackendDoesNotShowSuccessAndClearsPasswords() = runTest {
        gateway.failure = IllegalArgumentException("Contraseña actual incorrecta.")
        val vm = AccountSecurityViewModel(repository)
        vm.currentPassword("Wrong123")
        vm.newPassword("Nueva1234")
        vm.confirmation("Nueva1234")
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.uiState.value.message)
        assertNotNull(vm.uiState.value.error)
        assertEquals("", vm.uiState.value.currentPassword)
        assertEquals("", vm.uiState.value.newPassword)
        assertFalse(vm.uiState.value.submitting)
    }
    @Test fun doubleTapOnlySendsOneRequestAndSuccessWaitsForBackend() = runTest {
        val vm = AccountSecurityViewModel(repository)
        vm.select(AccountAction.RESET_PASSWORD)
        vm.email("ana@example.com")
        vm.submit()
        vm.submit()
        assertTrue(vm.uiState.value.submitting)
        assertNull(vm.uiState.value.message)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, gateway.calls)
        assertNotNull(vm.uiState.value.message)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, gateway.calls)
    }
    @Test fun successfulPasswordChangeClearsAllSecrets() = runTest {
        val vm = AccountSecurityViewModel(repository)
        vm.currentPassword("Actual123")
        vm.newPassword("Nueva1234")
        vm.confirmation("Nueva1234")
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.message)
        assertEquals("", vm.uiState.value.currentPassword)
        assertEquals("", vm.uiState.value.newPassword)
        assertEquals("", vm.uiState.value.confirmation)
    }
    @Test fun cancellationIsNotConvertedToSuccessOrGenericError() = runTest {
        gateway.failure = CancellationException("cancelled")
        try {
            repository.resetPassword("ana@example.com")
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    private class FakeSecurityGateway : AccountSecurityGateway {
        var calls = 0
        var email = ""
        var failure: Exception? = null
        private fun attempt() { calls++; failure?.let { throw it } }
        override suspend fun sendReset(email: String) { attempt(); this.email = email }
        override suspend fun changePassword(currentPassword: String, newPassword: String) { attempt() }
        override suspend fun requestEmailChange(currentPassword: String, newEmail: String) { attempt(); email = newEmail }
    }
}
