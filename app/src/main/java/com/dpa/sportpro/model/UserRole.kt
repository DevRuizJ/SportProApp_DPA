package com.dpa.sportpro.model

enum class UserRole(val code: String, val displayName: String, val requiresBirthDate: Boolean) {
    COACH("DT", "Entrenador", false),
    PLAYER("JUG", "Jugador", true),
    PARENT("PAD", "Padre de Familia", true),
    ADMIN("ADM", "Administrador", false);

    companion object {
        fun fromCode(code: String): UserRole? = entries.find { it.code == code }
    }
}
