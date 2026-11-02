package com.dpa.sportpro

import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.data.repository.*

class TestIdentity : IdentityGateway {
    val accounts = mutableMapOf<String, Pair<IdentityAccount, String>>()
    var signedIn: IdentityAccount? = null
    override suspend fun create(email: String, password: String): IdentityAccount {
        if (email in accounts) throw AccountAlreadyExistsException()
        val account = IdentityAccount("uid-${accounts.size}", email)
        accounts[email] = account to password
        signedIn = account
        return account
    }
    override suspend fun signIn(email: String, password: String): IdentityAccount {
        val entry = accounts[email]
        require(entry?.second == password) { "Correo electrónico o contraseña incorrectos." }
        return requireNotNull(entry).first.also { signedIn = it }
    }
    override suspend fun current() = signedIn
    override fun signOut() { signedIn = null }
}

class TestProfiles : UserProfileGateway {
    val entries = mutableMapOf<String, UserProfile>()
    var failWrites = false
    override suspend fun get(uid: String) = entries[uid]
    override suspend fun synchronizeEmail(uid: String, email: String) {
        check(!failWrites)
        entries[uid] = requireNotNull(entries[uid]).copy(email = email)
    }
    override suspend fun createIfAbsent(profile: UserProfile) {
        check(!failWrites) { "Network unavailable" }
        entries.putIfAbsent(profile.uid, profile)
    }
}
