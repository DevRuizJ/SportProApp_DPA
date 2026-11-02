package com.dpa.sportpro

import com.dpa.sportpro.data.repository.*
import com.dpa.sportpro.model.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryTest {
    private val identity = TestIdentity()
    private val profiles = TestProfiles()
    private val repository = AuthRepositoryImpl(identity, profiles)
    private fun request(role: UserRole, email: String = "someone.admin@example.com") =
        RegistrationRequest("Ana", "Torres", email, "ClaveSegura123", role, if (role.requiresBirthDate) 946684800000L else null)

    @Test fun newUsersCanRegisterAndLogInWithEverySelectedRole() = runTest {
        UserRole.entries.forEach { role ->
            val data = request(role, "new-${role.code}@example.com")
            val created = repository.register(data) as AuthResult.Success
            assertNull(identity.signedIn)
            val logged = repository.login(data.email, data.password) as AuthResult.Success
            assertEquals(created.userProfile.uid, logged.userProfile.uid)
            assertEquals(role, logged.userProfile.role)
            assertEquals("Ana", logged.userProfile.names)
            assertEquals(data.birthDateMillis, logged.userProfile.birthDateMillis)
            repository.signOut()
        }
        assertEquals(4, profiles.entries.size)
    }

    @Test fun roleComesFromSavedProfileNotEmail() = runTest {
        repository.register(request(UserRole.PLAYER))
        val result = repository.login("someone.admin@example.com", "ClaveSegura123") as AuthResult.Success
        assertEquals(UserRole.PLAYER, result.userProfile.role)
    }

    @Test fun failedProfileWriteDoesNotReportSuccessAndCanBeRetried() = runTest {
        profiles.failWrites = true
        val data = request(UserRole.COACH)
        assertTrue(repository.register(data) is AuthResult.Error)
        assertEquals(1, identity.accounts.size)
        assertTrue(profiles.entries.isEmpty())
        profiles.failWrites = false
        assertTrue(repository.register(data) is AuthResult.Success)
        assertEquals(1, identity.accounts.size)
        assertEquals(1, profiles.entries.size)
    }

    @Test fun duplicateRegistrationCannotChangeExistingRole() = runTest {
        repository.register(request(UserRole.PLAYER))
        assertTrue(repository.register(request(UserRole.ADMIN)) is AuthResult.Error)
        assertEquals(UserRole.PLAYER, profiles.entries.values.single().role)
    }

    @Test fun suspendedAccountCannotEnterEvenWithCorrectPassword() = runTest {
        val data = request(UserRole.PLAYER)
        val user = (repository.register(data) as AuthResult.Success).userProfile
        profiles.entries[user.uid] = user.copy(isActive = false)
        assertTrue(repository.login(data.email, data.password) is AuthResult.Error)
        assertNull(AuthSession.profile.value)
        assertNull(identity.signedIn)
    }

    @Test fun newRepositoryRestoresPersistedIdentityAndStoredProfile() = runTest {
        val data = request(UserRole.PARENT)
        repository.register(data)
        repository.login(data.email, data.password)
        val restored = AuthRepositoryImpl(identity, profiles).restoreSession() as AuthResult.Success
        assertEquals(UserRole.PARENT, restored.userProfile.role)
    }

    @Test fun profileMissingDoesNotFabricateUserOrRole() = runTest {
        identity.create("admin@example.com", "ClaveSegura123")
        assertTrue(repository.login("admin@example.com", "ClaveSegura123") is AuthResult.Error)
        assertNull(AuthSession.profile.value)
    }

    @Test fun changedAuthEmailKeepsUidRoleAndUpdatesStoredProfileOnLogin() = runTest {
        val data = request(UserRole.PLAYER)
        val original = (repository.register(data) as AuthResult.Success).userProfile
        val account = identity.accounts.remove(data.email)!!.first.copy(email = "verified@example.com")
        identity.accounts[account.email] = account to data.password
        val logged = repository.login(account.email, data.password) as AuthResult.Success
        assertEquals(original.uid, logged.userProfile.uid)
        assertEquals(original.role, logged.userProfile.role)
        assertEquals(account.email, profiles.entries[original.uid]?.email)
        assertEquals(1, profiles.entries.size)
    }

    @Test fun futureBirthDateDoesNotCreateAccount() = runTest {
        assertTrue(repository.register(request(UserRole.PLAYER).copy(birthDateMillis = Long.MAX_VALUE)) is AuthResult.Error)
        assertTrue(identity.accounts.isEmpty())
    }
}
