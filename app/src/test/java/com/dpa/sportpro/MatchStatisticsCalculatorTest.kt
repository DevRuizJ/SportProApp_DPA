package com.dpa.sportpro

import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchRecord
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.data.repository.MatchStatisticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchStatisticsCalculatorTest {

    @Test
    fun teamTotalsIncludeOnlyFinishedMatchesAndExcludeAnnulledGoals() {
        val matches =
            listOf(
                match(
                    id = "win",
                    events =
                        listOf(
                            goal("h1", "SportPro FC", "Mateo", 20),
                            goal("h2", "SportPro FC", "Mateo", 50),
                            goal("a1", "Inter Lima", "Rival", 25, MatchEventStatus.ANNULLED),
                        ),
                ),
                match(
                    id = "draw",
                    events =
                        listOf(
                            goal("h3", "SportPro FC", "Mateo", 10),
                            goal("a2", "Inter Lima", "Rival", 11),
                        ),
                ),
                match(
                    id = "loss",
                    events =
                        listOf(
                            goal("a3", "Inter Lima", "Rival", 30),
                            goal("a4", "Inter Lima", "Rival", 80),
                        ),
                ),
                match(
                    id = "live",
                    status = MatchStatus.IN_PROGRESS,
                    events = listOf(goal("h4", "SportPro FC", "Mateo", 15)),
                ),
            )

        val stats = MatchStatisticsCalculator.calculateTeams(matches).associateBy { it.teamName }

        assertEquals(3, stats.getValue("SportPro FC").matchesPlayed)
        assertEquals(1, stats.getValue("SportPro FC").matchesWon)
        assertEquals(1, stats.getValue("SportPro FC").matchesDrawn)
        assertEquals(1, stats.getValue("SportPro FC").matchesLost)
        assertEquals(3, stats.getValue("SportPro FC").goalsFor)
        assertEquals(3, stats.getValue("SportPro FC").goalsAgainst)
        assertEquals(0, stats.getValue("SportPro FC").goalDifference)
    }

    @Test
    fun playerTotalsAggregateAppearancesEventsAndSubstitutionMinutes() {
        val game =
            match(
                id = "game",
                duration = 90,
                starters = listOf("Mateo Silva", "Lucas Castro"),
                events =
                    listOf(
                        goal("g1", "SportPro FC", "Mateo Silva", 20, assistant = "Thiago Rossi"),
                        goal("g2", "SportPro FC", "Mateo Silva", 70),
                        MatchEvent(
                            id = "yellow",
                            minute = 42,
                            eventType = EventType.YELLOW_CARD,
                            teamName = "SportPro FC",
                            playerName = "Lucas Castro",
                        ),
                        MatchEvent(
                            id = "sub",
                            minute = 60,
                            eventType = EventType.SUBSTITUTION,
                            teamName = "SportPro FC",
                            playerName = "Lucas Castro",
                            assistantPlayerName = "Nico Torres",
                        ),
                        MatchEvent(
                            id = "red",
                            minute = 75,
                            eventType = EventType.RED_CARD,
                            teamName = "SportPro FC",
                            playerName = "Mateo Silva",
                        ),
                        goal(
                            "annulled",
                            "SportPro FC",
                            "Lucas Castro",
                            80,
                            status = MatchEventStatus.ANNULLED,
                        ),
                    ),
            )

        val stats =
            MatchStatisticsCalculator.calculatePlayers(listOf(game)).associateBy { it.playerName }

        assertEquals(2, stats.getValue("Mateo Silva").goals)
        assertEquals(75, stats.getValue("Mateo Silva").minutesPlayed)
        assertEquals(1, stats.getValue("Mateo Silva").redCards)
        assertEquals(1, stats.getValue("Thiago Rossi").assists)
        assertEquals(90, stats.getValue("Thiago Rossi").minutesPlayed)
        assertEquals(60, stats.getValue("Lucas Castro").minutesPlayed)
        assertEquals(1, stats.getValue("Lucas Castro").yellowCards)
        assertEquals(30, stats.getValue("Nico Torres").minutesPlayed)
        assertTrue(stats.values.all { it.matchesPlayed == 1 })
        assertTrue("Jugador no identificado" !in stats)
    }

    private fun match(
        id: String,
        status: MatchStatus = MatchStatus.FINISHED,
        duration: Int = 90,
        starters: List<String> = emptyList(),
        events: List<MatchEvent>,
    ) =
        MatchRecord(
            id = id,
            homeTeamName = "SportPro FC",
            awayTeamName = "Inter Lima",
            status = status,
            durationMinutes = duration,
            startingPlayers = starters,
            events = events,
            startedAtMillis = 0,
        )

    private fun goal(
        id: String,
        teamName: String,
        playerName: String,
        minute: Int,
        status: MatchEventStatus = MatchEventStatus.ACTIVE,
        assistant: String? = null,
    ) =
        MatchEvent(
            id = id,
            minute = minute,
            eventType = EventType.GOAL,
            teamName = teamName,
            playerName = playerName,
            assistantPlayerName = assistant,
            status = status,
        )
}
