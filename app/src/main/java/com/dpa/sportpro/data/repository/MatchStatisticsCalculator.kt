package com.dpa.sportpro.data.repository

import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchRecord
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.data.model.PlayerTournamentStatistics
import com.dpa.sportpro.data.model.TeamStatistics

object MatchStatisticsCalculator {

    fun calculateTeams(matches: List<MatchRecord>): List<TeamStatistics> {
        val finishedMatches = matches.filter { it.status == MatchStatus.FINISHED }
        val teamNames = finishedMatches.map { it.homeTeamName }.distinct()

        return teamNames
            .map { teamName ->
                var played = 0
                var won = 0
                var drawn = 0
                var lost = 0
                var goalsFor = 0
                var goalsAgainst = 0

                finishedMatches.forEach { match ->
                    if (teamName != match.homeTeamName) return@forEach
                    played++
                    val homeGoals = score(match, match.homeTeamName)
                    val awayGoals = score(match, match.awayTeamName)
                    goalsFor += homeGoals
                    goalsAgainst += awayGoals
                    when {
                        homeGoals > awayGoals -> won++
                        homeGoals < awayGoals -> lost++
                        else -> drawn++
                    }
                }

                TeamStatistics(
                    teamName = teamName,
                    matchesPlayed = played,
                    matchesWon = won,
                    matchesDrawn = drawn,
                    matchesLost = lost,
                    goalsFor = goalsFor,
                    goalsAgainst = goalsAgainst,
                    goalDifference = goalsFor - goalsAgainst,
                )
            }
            .sortedBy { it.teamName }
    }

    fun calculatePlayers(matches: List<MatchRecord>): List<PlayerTournamentStatistics> {
        val playerTotals = mutableMapOf<Pair<String, String>, MutablePlayerStatistics>()
        matches
            .filter { it.status == MatchStatus.FINISHED }
            .forEach { match ->
                val events =
                    match.events.filter {
                        it.status != MatchEventStatus.ANNULLED && it.teamName == match.homeTeamName
                    }
                val substitutions = events.filter { it.eventType == EventType.SUBSTITUTION }
                val playerNames =
                    (match.startingPlayers +
                            events.flatMap { event ->
                                buildList {
                                    add(event.playerName)
                                    when (event.eventType) {
                                        EventType.GOAL,
                                        EventType.SUBSTITUTION ->
                                            event.assistantPlayerName?.let(::add)
                                        else -> Unit
                                    }
                                }
                            })
                        .filter(::isIdentifiedPlayer)
                        .toSet()

                playerNames.forEach { playerName ->
                    val incomingAt =
                        substitutions
                            .filter { it.assistantPlayerName == playerName }
                            .minOfOrNull { it.minute }
                    val substitutedOutAt =
                        substitutions
                            .filter {
                                it.playerName == playerName &&
                                    (incomingAt == null || it.minute > incomingAt)
                            }
                            .minOfOrNull { it.minute }
                    val sentOffAt =
                        events
                            .filter {
                                it.eventType == EventType.RED_CARD &&
                                    it.playerName == playerName &&
                                    (incomingAt == null || it.minute > incomingAt)
                            }
                            .minOfOrNull { it.minute }
                    val startMinute = incomingAt ?: 0
                    val endMinute =
                        listOfNotNull(substitutedOutAt, sentOffAt).minOrNull()
                            ?: match.durationMinutes
                    val playerKey = match.homeTeamName to playerName
                    val statistics = playerTotals.getOrPut(playerKey) { MutablePlayerStatistics() }

                    statistics.matchesPlayed++
                    statistics.minutesPlayed += (endMinute - startMinute).coerceAtLeast(0)
                    statistics.goals +=
                        events.count {
                            it.eventType == EventType.GOAL && it.playerName == playerName
                        }
                    statistics.assists +=
                        events.count {
                            it.eventType == EventType.GOAL && it.assistantPlayerName == playerName
                        }
                    statistics.yellowCards +=
                        events.count {
                            it.eventType == EventType.YELLOW_CARD && it.playerName == playerName
                        }
                    statistics.redCards +=
                        events.count {
                            it.eventType == EventType.RED_CARD && it.playerName == playerName
                        }
                }
            }

        return playerTotals
            .map { (playerKey, statistics) ->
                PlayerTournamentStatistics(
                    teamName = playerKey.first,
                    playerName = playerKey.second,
                    matchesPlayed = statistics.matchesPlayed,
                    minutesPlayed = statistics.minutesPlayed,
                    goals = statistics.goals,
                    assists = statistics.assists,
                    yellowCards = statistics.yellowCards,
                    redCards = statistics.redCards,
                )
            }
            .sortedWith(
                compareByDescending<PlayerTournamentStatistics> { it.goals }
                    .thenByDescending { it.assists }
                    .thenByDescending { it.minutesPlayed }
                    .thenBy { it.playerName }
            )
    }

    private fun score(match: MatchRecord, teamName: String): Int =
        match.events.count {
            it.teamName == teamName &&
                it.eventType == EventType.GOAL &&
                it.status != MatchEventStatus.ANNULLED
        }

    private fun isIdentifiedPlayer(name: String): Boolean =
        name.isNotBlank() && !name.equals("Jugador no identificado", ignoreCase = true)

    private class MutablePlayerStatistics(
        var matchesPlayed: Int = 0,
        var minutesPlayed: Int = 0,
        var goals: Int = 0,
        var assists: Int = 0,
        var yellowCards: Int = 0,
        var redCards: Int = 0,
    )
}
