package com.dpa.sportpro.data.model

enum class LineupStatus(val label: String) {
    DRAFT("Borrador"),
    PUBLISHED("Publicada"),
}

data class TacticalLineup(
    val convocationId: String,
    val formation: String,
    val startersBySlot: Map<String, String>,
    val captainPlayerId: String,
    val status: LineupStatus,
    val updatedAt: Long,
)
