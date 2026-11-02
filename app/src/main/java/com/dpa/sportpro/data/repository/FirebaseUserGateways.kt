package com.dpa.sportpro.data.repository

import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

data class IdentityAccount(
    val uid: String,
    val email: String,
    val displayName: String = "",
    val trustedRole: UserRole? = null,
)

class AccountAlreadyExistsException : Exception()
interface IdentityGateway {
    suspend fun create(email: String, password: String): IdentityAccount
    suspend fun signIn(email: String, password: String): IdentityAccount
    suspend fun current(): IdentityAccount?
    fun signOut()
}

interface UserProfileGateway {
    suspend fun get(uid: String): UserProfile?
    suspend fun createIfAbsent(profile: UserProfile)
    suspend fun synchronizeEmail(uid: String, email: String)
}

class FirebaseIdentityGateway : IdentityGateway {
    private val auth get() = FirebaseAuth.getInstance()

    override suspend fun create(email: String, password: String): IdentityAccount = try {
        checkNotNull(auth.createUserWithEmailAndPassword(email, password).await().user).account()
    } catch (_: FirebaseAuthUserCollisionException) {
        throw AccountAlreadyExistsException()
    }

    override suspend fun signIn(email: String, password: String): IdentityAccount =
        checkNotNull(auth.signInWithEmailAndPassword(email, password).await().user).account()

    override suspend fun current(): IdentityAccount? {
        val user = auth.currentUser ?: return null
        user.reload().await()
        return auth.currentUser?.account()
    }

    override fun signOut() = auth.signOut()

    private suspend fun FirebaseUser.account(): IdentityAccount {
        val claims = getIdToken(true).await().claims
        val roleCode = (claims["app_role"] ?: claims["role"]) as? String
        return IdentityAccount(uid, email.orEmpty(), displayName.orEmpty(), roleCode?.let(UserRole::fromCode))
    }
}

object SportProFirestore {
    const val DATABASE_ID = "sportpro-live"
    val instance: FirebaseFirestore
        get() = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID)
}

class FirestoreUserProfileGateway : UserProfileGateway {
    private val users get() = SportProFirestore.instance.collection("users")

    override suspend fun get(uid: String): UserProfile? {
        val document = users.document(uid).get(Source.SERVER).await()
        if (!document.exists()) return null
        return UserProfile(
            uid = document.id,
            email = requireNotNull(document.getString("email")),
            names = requireNotNull(document.getString("names")),
            lastNames = requireNotNull(document.getString("lastNames")),
            role = requireNotNull(UserRole.fromCode(document.getString("selectedRole").orEmpty())),
            isActive = document.getBoolean("isActive") == true,
            birthDateMillis = document.getLong("birthDateMillis"),
        )
    }

    override suspend fun synchronizeEmail(uid: String, email: String) {
        users.document(uid).update(mapOf(
            "email" to email,
            "updatedAt" to FieldValue.serverTimestamp(),
        )).await()
    }

    override suspend fun createIfAbsent(profile: UserProfile) {
        val reference = users.document(profile.uid)
        SportProFirestore.instance.runTransaction { transaction ->
            val existing = transaction.get(reference)
            if (!existing.exists()) {
                transaction.set(reference, mapOf(
                    "uid" to profile.uid,
                    "email" to profile.email,
                    "names" to profile.names,
                    "lastNames" to profile.lastNames,
                    // Onboarding/navigation preference, NOT a global authorization grant.
                    "selectedRole" to profile.role.code,
                    "birthDateMillis" to profile.birthDateMillis,
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ))
            }
        }.await()
    }
}
