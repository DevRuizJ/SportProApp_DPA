package com.dpa.sportpro.data.model

import com.dpa.sportpro.model.UserRole

data class UserProfile(
    val uid: String,
    val email: String,
    val names: String,
    val lastNames: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val birthDateMillis: Long? = null,
    /** Server-signed role; never read from selectedRole or persisted by the client. */
    val authorizedRole: UserRole? = null,
    val offlineSession: Boolean = false,
)
