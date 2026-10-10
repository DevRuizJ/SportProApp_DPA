package com.dpa.sportpro.data.repository

import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val userProfile: UserProfile) : AuthResult()

    data class Error(val message: String) : AuthResult()
}

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
}

class AuthRepositoryImpl : AuthRepository {

    private val firebaseAuth: FirebaseAuth? =
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }

    private val mockUsers =
        mapOf(
            "dt@sportpro.com" to
                UserProfile("dt_1", "dt@sportpro.com", "Carlos", "Gómez", UserRole.COACH),
            "jugador@sportpro.com" to
                UserProfile("jug_1", "jugador@sportpro.com", "Mateo", "Silva", UserRole.PLAYER),
            "padre@sportpro.com" to
                UserProfile("pad_1", "padre@sportpro.com", "Juan", "Silva", UserRole.PARENT),
            "admin@sportpro.com" to
                UserProfile("admin_1", "admin@sportpro.com", "Admin", "SportPro", UserRole.ADMIN),
        )

    override suspend fun login(email: String, password: String): AuthResult {
        if (firebaseAuth != null && firebaseAuth.app != null) {
            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val role = determineRoleFromEmail(email)
                    return AuthResult.Success(
                        UserProfile(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: email,
                            names = firebaseUser.displayName ?: "Usuario",
                            lastNames = "",
                            role = role,
                        )
                    )
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                return AuthResult.Error("Correo electrónico no registrado o cuenta deshabilitada.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                return AuthResult.Error("Correo electrónico o contraseña incorrectos.")
            } catch (e: Exception) {
                return authenticateWithMockOrFallback(email, password)
            }
        }

        return authenticateWithMockOrFallback(email, password)
    }

    private fun authenticateWithMockOrFallback(email: String, password: String): AuthResult {
        val normalizedEmail = email.trim().lowercase()
        val user = mockUsers[normalizedEmail]

        return if (user != null && password == "SportPro123") {
            AuthResult.Success(user)
        } else if (normalizedEmail.contains("sportpro.com") && password.length >= 8) {
            val role = determineRoleFromEmail(normalizedEmail)
            AuthResult.Success(
                UserProfile("user_gen", normalizedEmail, "Usuario", "SportPro", role)
            )
        } else {
            AuthResult.Error("Correo electrónico o contraseña incorrectos.")
        }
    }

    private fun determineRoleFromEmail(email: String): UserRole {
        val lower = email.lowercase()
        return when {
            lower.contains("dt") || lower.contains("entrenador") -> UserRole.COACH
            lower.contains("jug") || lower.contains("jugador") -> UserRole.PLAYER
            lower.contains("pad") || lower.contains("padre") -> UserRole.PARENT
            lower.contains("admin") || lower.contains("adm") -> UserRole.ADMIN
            else -> UserRole.PLAYER
        }
    }
}
