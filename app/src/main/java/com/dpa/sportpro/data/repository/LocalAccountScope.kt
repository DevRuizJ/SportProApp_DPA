package com.dpa.sportpro.data.repository

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

/** Temporary isolation for legacy repositories while each domain moves to scoped Firestore data. */
object LocalAccountScope {
    fun uid(): String = requireNotNull(AuthSession.profile.value?.uid) { "Se requiere una sesión válida." }

    fun preferences(context: Context, namespace: String, ownerUid: String = uid()): SharedPreferences =
        context.getSharedPreferences(fileName(namespace, ownerUid), Context.MODE_PRIVATE)

    internal fun fileName(namespace: String, ownerUid: String): String {
        require(ownerUid.isNotBlank())
        val suffix = MessageDigest.getInstance("SHA-256").digest(ownerUid.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        // Do not import unowned legacy files into the first account that signs in.
        return "${namespace}_account_$suffix"
    }
}
