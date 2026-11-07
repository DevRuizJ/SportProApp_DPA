package com.dpa.sportpro.data.model

enum class MatchStatus {
    IN_PROGRESS,
    FINISHED,
}

data class MatchRecord(
    val id: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val status: MatchStatus,
    val durationMinutes: Int,
    val startingPlayers: List<String>,
    val events: List<MatchEvent>,
    val startedAtMillis: Long,
    val finishedAtMillis: Long? = null,
)

data class PlayerTournamentStatistics(
    val teamName: String,
    val playerName: String,
    val matchesPlayed: Int,
    val minutesPlayed: Int,
    val goals: Int,
    val assists: Int,
    val yellowCards: Int,
    val redCards: Int,
)
