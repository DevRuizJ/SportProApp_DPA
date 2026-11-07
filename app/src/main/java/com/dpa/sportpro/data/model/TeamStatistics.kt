package com.dpa.sportpro.data.model

data class TeamStatistics(
    val teamName: String,
    val matchesPlayed: Int,
    val matchesWon: Int,
    val matchesDrawn: Int,
    val matchesLost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
)
