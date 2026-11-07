package com.dpa.sportpro.data.model

enum class EventType(val displayName: String, val shortName: String) {
    MATCH_START("Inicio de tiempo", "Inicio"),
    MATCH_END("Fin de tiempo", "Fin"),
    GOAL("Gol", "Gol"),
    FOUL("Falta", "Falta"),
    GOAL_KICK("Saque de meta", "Saque de meta"),
    THROW_IN("Saque lateral", "Saque lateral"),
    YELLOW_CARD("Tarjeta Amarilla", "T. Amarilla"),
    RED_CARD("Tarjeta Roja", "T. Roja"),
    CORNER("Tiro de Esquina", "Esquina"),
    SHOT("Tiro", "Tiro"),
    PENALTY("Penal", "Penal"),
    OFFSIDE("Fuera de juego", "Fuera de juego"),
    SUBSTITUTION("Sustitución", "Sustitución"),
}

enum class MatchEventStatus {
    ACTIVE,
    ANNULLED,
    CORRECTED,
}

data class MatchEvent(
    val id: String,
    val minute: Int,
    val eventType: EventType,
    val teamName: String,
    val playerName: String = "Jugador no identificado",
    val assistantPlayerName: String? = null,
    val notes: String = "",
    val status: MatchEventStatus = MatchEventStatus.ACTIVE,
    val modifiedByUserId: String? = null,
    val modifiedAtMillis: Long? = null,
    val correctionReason: String? = null,
)
