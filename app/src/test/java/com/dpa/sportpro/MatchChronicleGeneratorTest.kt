package com.dpa.sportpro

import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchRecord
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.data.repository.MatchChronicleEligibility
import com.dpa.sportpro.data.repository.MatchChronicleNarrativeGenerator
import com.dpa.sportpro.data.repository.MatchChroniclePrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchChronicleGeneratorTest {

    @Test
    fun promptContainsOnlyChronologicalActiveEventsWithAuthorsAndDetails() {
        val match =
            match(
                status = MatchStatus.FINISHED,
                events =
                    listOf(
                        event("end", 90, EventType.MATCH_END, "SportPro FC", "Sistema"),
                        event(
                            "annulled",
                            42,
                            EventType.GOAL,
                            "Rival FC",
                            "Jugador no identificado",
                            status = MatchEventStatus.ANNULLED,
                        ),
                        event(
                            "goal",
                            23,
                            EventType.GOAL,
                            "SportPro FC",
                            "Mateo Silva",
                            assistant = "Thiago Rossi",
                            notes = "Tiro libre",
                        ),
                        event("start", 0, EventType.MATCH_START, "SportPro FC", "Sistema"),
                    ),
            )

        val prompt = MatchChroniclePrompt.fromMatch(match).toStructuredText()
        val narrative = MatchChronicleNarrativeGenerator.generate(match)

        assertTrue(prompt.startsWith("["))
        assertTrue(prompt.indexOf("\"minuto\":0") < prompt.indexOf("\"minuto\":23"))
        assertTrue(prompt.indexOf("\"minuto\":23") < prompt.indexOf("\"minuto\":90"))
        assertTrue(prompt.contains("\"autor\":\"Mateo Silva\""))
        assertTrue(prompt.contains("\"asistencia\":\"Thiago Rossi\""))
        assertTrue(prompt.contains("\"detalle\":\"Tiro libre\""))
        assertFalse(prompt.contains("annulled"))
        assertFalse(prompt.contains("TAREA"))
        assertTrue(narrative.contains("Mateo Silva anotó para SportPro FC"))
        assertTrue(narrative.contains("La asistencia fue de Thiago Rossi"))
    }

    @Test
    fun eligibilityRequiresFinishedMatchAndNonAnnulledStartAndEndEvents() {
        val valid =
            match(
                status = MatchStatus.FINISHED,
                events =
                    listOf(
                        event("start", 0, EventType.MATCH_START, "SportPro FC", "Sistema"),
                        event("end", 90, EventType.MATCH_END, "SportPro FC", "Sistema"),
                    ),
            )

        assertTrue(MatchChronicleEligibility.canGenerate(valid))
        assertFalse(
            MatchChronicleEligibility.canGenerate(valid.copy(status = MatchStatus.IN_PROGRESS))
        )
        assertFalse(
            MatchChronicleEligibility.canGenerate(valid.copy(events = valid.events.drop(1)))
        )
        assertFalse(
            MatchChronicleEligibility.canGenerate(
                valid.copy(
                    events = valid.events.map { it.copy(status = MatchEventStatus.ANNULLED) }
                )
            )
        )
        assertFalse(MatchChronicleEligibility.canGenerate(null))
    }

    @Test
    fun generatorRejectsFinishedMatchWithoutStartOrEnd() {
        val withoutEnd =
            match(
                status = MatchStatus.FINISHED,
                events = listOf(event("start", 0, EventType.MATCH_START, "SportPro FC", "Sistema")),
            )

        val error =
            runCatching { MatchChronicleNarrativeGenerator.generate(withoutEnd) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    private fun match(status: MatchStatus, events: List<MatchEvent>) =
        MatchRecord(
            id = "match-1",
            homeTeamName = "SportPro FC",
            awayTeamName = "Rival FC",
            status = status,
            durationMinutes = 90,
            startingPlayers = listOf("Mateo Silva"),
            events = events,
            startedAtMillis = 1L,
            finishedAtMillis = 2L,
        )

    private fun event(
        id: String,
        minute: Int,
        type: EventType,
        teamName: String,
        author: String,
        assistant: String? = null,
        notes: String = "",
        status: MatchEventStatus = MatchEventStatus.ACTIVE,
    ) =
        MatchEvent(
            id = id,
            minute = minute,
            eventType = type,
            teamName = teamName,
            playerName = author,
            assistantPlayerName = assistant,
            notes = notes,
            status = status,
        )
}
