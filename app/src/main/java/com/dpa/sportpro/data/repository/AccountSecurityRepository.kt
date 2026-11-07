package com.dpa.sportpro.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.Locale

sealed interface AccountActionResult {
    data class Success(val message: String) : AccountActionResult
    data class Error(val message: String) : AccountActionResult
}

interface AccountSecurityGateway {
    suspend fun sendReset(email: String)
    suspend fun changePassword(currentPassword: String, newPassword: String)
    suspend fun requestEmailChange(currentPassword: String, newEmail: String)
}

class FirebaseAccountSecurityGateway : AccountSecurityGateway {
    private val auth get() = FirebaseAuth.getInstance()

    override suspend fun sendReset(email: String) {
        auth.setLanguageCode("es")
        auth.sendPasswordResetEmail(email).await()
    }

    private suspend fun reauthenticate(password: String): com.google.firebase.auth.FirebaseUser {
        val user = requireNotNull(auth.currentUser) { "Vuelve a iniciar sesión para continuar." }
        val email = requireNotNull(user.email) { "La cuenta no tiene un correo asociado." }
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        check(auth.currentUser?.uid == user.uid) { "La sesión cambió. Vuelve a iniciar sesión." }
        return user
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        reauthenticate(currentPassword).updatePassword(newPassword).await()
    }

    override suspend fun requestEmailChange(currentPassword: String, newEmail: String) {
        val user = reauthenticate(currentPassword)
        require(!newEmail.equals(user.email, ignoreCase = true)) { "Ingresa un correo diferente al actual." }
        auth.setLanguageCode("es")
        // Firebase changes the address only after the recipient opens the verification link.
        user.verifyBeforeUpdateEmail(newEmail).await()
    }
}

class AccountSecurityRepository(
    private val gateway: AccountSecurityGateway = FirebaseAccountSecurityGateway(),
) {
    suspend fun resetPassword(email: String): AccountActionResult = action {
        val normalized = validEmail(email)
        try {
            gateway.sendReset(normalized)
        } catch (error: FirebaseAuthException) {
            // Match Firebase's email enumeration protection even on legacy configurations.
            if (error.errorCode != "ERROR_USER_NOT_FOUND") throw error
        }
        AccountActionResult.Success(
            "Si existe una cuenta con ese correo, recibirás un enlace para restablecer la contraseña. Revisa también Spam."
        )
    }

    suspend fun changePassword(current: String, password: String, confirmation: String): AccountActionResult = action {
        require(current.isNotBlank()) { "Ingresa tu contraseña actual." }
        require(password.length in 8..128 && password.any(Char::isUpperCase) && password.any(Char::isDigit)) {
            "La nueva contraseña requiere entre 8 y 128 caracteres, una mayúscula y un número."
        }
        require(password == confirmation) { "Las contraseñas nuevas no coinciden." }
        require(password != current) { "La nueva contraseña debe ser diferente de la actual." }
        gateway.changePassword(current, password)
        AccountActionResult.Success("Contraseña actualizada. Vuelve a iniciar sesión con tu nueva contraseña.")
    }

    suspend fun changeEmail(current: String, email: String): AccountActionResult = action {
        require(current.isNotBlank()) { "Ingresa tu contraseña actual." }
        gateway.requestEmailChange(current, validEmail(email))
        AccountActionResult.Success(
            "Solicitud enviada. Revisa el nuevo correo y abre el enlace de verificación. " +
                "Hasta verificarlo, tu correo actual sigue vigente. Después, inicia sesión con el nuevo correo."
        )
    }

    private fun validEmail(value: String): String {
        val email = value.trim().lowercase(Locale.ROOT)
        require(email.length <= 254 && email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            "Ingresa un correo electrónico válido."
        }
        return email
    }

    private suspend fun action(block: suspend () -> AccountActionResult): AccountActionResult = try {
        withTimeout(30_000) { block() }
    } catch (error: Exception) {
        if (error is CancellationException && error !is TimeoutCancellationException) throw error
        // A typo or failed reauthentication must not silently sign out the current user.
        val message = if (error is FirebaseAuthException) when (error.errorCode) {
            "ERROR_REQUIRES_RECENT_LOGIN", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" ->
                "Tu sesión venció. Cierra sesión y vuelve a ingresar."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "No se puede usar ese correo. Prueba con otro."
            "ERROR_OPERATION_NOT_ALLOWED" -> "La operación no está habilitada. Contacta al administrador."
            else -> authErrorMessage(error)
        } else authErrorMessage(error)
        AccountActionResult.Error(message)
    }
}
