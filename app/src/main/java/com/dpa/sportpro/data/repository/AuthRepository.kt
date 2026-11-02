package com.dpa.sportpro.data.repository

import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.model.UserRole
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.util.Locale

sealed class AuthResult {
    data class Success(val userProfile: UserProfile) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

data class RegistrationRequest(
    val names: String,
    val lastNames: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val birthDateMillis: Long?,
)

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(request: RegistrationRequest): AuthResult
    suspend fun restoreSession(): AuthResult?
    fun signOut()
}

/** In-memory profile for navigation. Credentials and tokens remain managed by Firebase. */
object AuthSession {
    private val mutableProfile = MutableStateFlow<UserProfile?>(null)
    val profile = mutableProfile.asStateFlow()
    internal fun set(profile: UserProfile?) { mutableProfile.value = profile }
}

class AuthRepositoryImpl(
    private val identity: IdentityGateway = FirebaseIdentityGateway(),
    private val profiles: UserProfileGateway = FirestoreUserProfileGateway(),
) : AuthRepository {
    override suspend fun register(request: RegistrationRequest): AuthResult = requireNotNull(operation {
        require(request.names.trim().length in 1..80 && request.lastNames.trim().length in 1..80) {
            "Nombres y apellidos deben contener entre 1 y 80 caracteres."
        }
        require(request.password.length in 8..128 &&
            request.password.any(Char::isUpperCase) && request.password.any(Char::isDigit)) {
            "La contraseña requiere al menos 8 caracteres, una mayúscula y un número."
        }
        require(!request.role.requiresBirthDate || request.birthDateMillis != null) {
            "Selecciona tu fecha de nacimiento."
        }
        require(request.birthDateMillis == null ||
            request.birthDateMillis in -2208988800000L..System.currentTimeMillis()) {
            "La fecha de nacimiento no es válida."
        }
        val email = request.email.trim().lowercase(Locale.ROOT)
        require(email.length <= 254 && email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            "Ingresa un correo electrónico válido."
        }
        val account = try {
            identity.create(email, request.password)
        } catch (_: AccountAlreadyExistsException) {
            // Recovery after Auth succeeded but the Firestore profile could not be saved.
            val existing = identity.signIn(email, request.password)
            if (profiles.get(existing.uid) != null) {
                identity.signOut()
                return@operation AuthResult.Error("Este correo ya está registrado. Inicia sesión.")
            }
            existing
        }
        val profile = UserProfile(
            uid = account.uid, email = account.email,
            names = request.names.trim(), lastNames = request.lastNames.trim(),
            role = account.trustedRole ?: request.role,
            birthDateMillis = request.birthDateMillis,
        )
        profiles.createIfAbsent(profile)
        // Keep the existing UI flow: confirmation dialog, then the login screen.
        identity.signOut()
        AuthSession.set(null)
        AuthResult.Success(profile)
    })

    override suspend fun login(email: String, password: String): AuthResult = requireNotNull(operation {
        val account = identity.signIn(email.trim().lowercase(Locale.ROOT), password)
        loadProfile(account)
    })

    override suspend fun restoreSession(): AuthResult? {
        return operation {
            val account = identity.current() ?: return@operation null
            loadProfile(account)
        }
    }

    private suspend fun loadProfile(account: IdentityAccount): AuthResult {
        var profile = profiles.get(account.uid)
        if (profile == null && account.trustedRole != null) {
            // Compatibility for imported accounts: only a server-signed role, never email guessing.
            profile = UserProfile(
                uid = account.uid, email = account.email,
                names = account.displayName.ifBlank { "Usuario" }, lastNames = "",
                role = account.trustedRole,
            )
            profiles.createIfAbsent(profile)
        }
        if (profile == null) {
            signOut()
            return AuthResult.Error("Tu cuenta existe, pero falta completar el perfil. Repite el registro con el mismo correo y contraseña.")
        }
        if (!profile.isActive) {
            signOut()
            return AuthResult.Error("Tu cuenta está suspendida. Contacta al administrador.")
        }
        if (profile.email != account.email) {
            // Auth is authoritative. Rules require the new verified email in the signed token.
            profiles.synchronizeEmail(account.uid, account.email)
            profile = profile.copy(email = account.email)
        }
        AuthSession.set(profile)
        return AuthResult.Success(profile)
    }

    override fun signOut() {
        identity.signOut()
        AuthSession.set(null)
    }

    private suspend fun operation(block: suspend () -> AuthResult?): AuthResult? {
        try {
            return withTimeout(30_000) { block() }
        } catch (error: Exception) {
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            signOut()
            return AuthResult.Error(authErrorMessage(error))
        }
    }
}

internal fun authErrorMessage(error: Exception): String = when (error) {
    is TimeoutCancellationException, is FirebaseNetworkException ->
        "No se pudo completar la operación. Revisa tu conexión y vuelve a intentarlo con los mismos datos."
    is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera un momento e inténtalo de nuevo."
    is FirebaseAuthUserCollisionException -> "Este correo ya está registrado. Inicia sesión."
    is FirebaseAuthException -> when (error.errorCode) {
        "ERROR_USER_DISABLED" -> "Tu cuenta está deshabilitada. Contacta al administrador."
        "ERROR_INVALID_EMAIL" -> "Ingresa un correo electrónico válido."
        "ERROR_WEAK_PASSWORD" -> "La contraseña no cumple los requisitos de seguridad."
        else -> "Correo electrónico o contraseña incorrectos."
    }
    is FirebaseFirestoreException -> when (error.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No se pudo guardar o leer tu perfil. Revisa los permisos de la cuenta e inténtalo nuevamente."
        else -> "No se pudo acceder a tu perfil. Revisa tu conexión e inténtalo nuevamente."
    }
    is IllegalArgumentException -> error.message ?: "Revisa los datos ingresados."
    else -> "No se pudo completar la operación. Vuelve a intentarlo con los mismos datos."
}
