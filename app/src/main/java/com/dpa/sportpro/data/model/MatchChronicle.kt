package com.dpa.sportpro.data.model

enum class ChronicleStatus {
    PENDING_APPROVAL,
    APPROVED,
}

data class MatchChronicle(
    val id: String,
    val matchId: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val homeScore: Int,
    val awayScore: Int,
    val matchDateMillis: Long,
    val narrative: String,
    val status: ChronicleStatus,
    val createdAtMillis: Long,
    val technicalComment: String = "",
    val publishedAtMillis: Long? = null,
)
