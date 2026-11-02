package com.dpa.sportpro

import com.dpa.sportpro.domain.match.*
import org.junit.Assert.*
import org.junit.Test

class MatchLedgerTest {
    private val goal = EventPayload("goal", 120, MatchSide.HOME)
    private fun ledger() = MatchLedger().record("g1", goal, "operator", 1000)

    @Test fun unidentifiedPlayerDoesNotBlockGoal() {
        assertNull(ledger().currentEvents.single().payload.playerId)
        assertEquals(Score(1, 0), ledger().score)
    }

    @Test fun retryDoesNotDuplicateGoal() {
        val ledger = ledger().record("g1", goal, "operator", 2000)
        assertEquals(1, ledger.history.size)
        assertEquals(Score(1, 0), ledger.score)
    }

    @Test fun correctionMovesGoalAndPreservesOriginal() {
        val ledger = ledger().correct("g1", 1, goal.copy(side = MatchSide.AWAY),
            "coach", 2000, "Equipo incorrecto")
        assertEquals(Score(0, 1), ledger.score)
        assertEquals(goal, ledger.history.first().payload)
        assertEquals("operator", ledger.history.first().actorUid)
        assertEquals("coach", ledger.history.last().actorUid)
        assertEquals("Equipo incorrecto", ledger.history.last().reason)
    }

    @Test fun annulmentSubtractsGoalWithoutDeletingHistory() {
        val ledger = ledger().void("g1", 1, "coach", 2000, "Gol invalidado")
        assertEquals(Score(), ledger.score)
        assertEquals(2, ledger.history.size)
        assertEquals(EventStatus.VOIDED, ledger.currentEvents.single().status)
    }

    @Test fun lateEntryUsesMatchTimeRatherThanCaptureTime() {
        val ledger = ledger()
            .record("late", goal.copy(elapsedSeconds = 60), "operator", 3000)
        assertEquals(listOf("g1", "late"), ledger.currentEvents.map { it.eventId })
    }

    @Test fun oldRetryAfterCorrectionCannotRestoreOriginalScore() {
        val ledger = ledger().correct("g1", 1, goal.copy(side = MatchSide.AWAY),
            "coach", 2000, "Equipo incorrecto").record("g1", goal, "operator", 3000)
        assertEquals(Score(0, 1), ledger.score)
        assertEquals(2, ledger.history.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun staleCorrectionIsRejected() {
        ledger().correct("g1", 1, goal, "coach", 2000, "Minuto revisado")
            .void("g1", 1, "operator", 3000, "Anular")
    }

    @Test(expected = IllegalArgumentException::class)
    fun correctionNeedsReason() {
        ledger().correct("g1", 1, goal, "coach", 2000, " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun goalNeedsTeam() { EventPayload("goal", 120) }

    @Test(expected = IllegalArgumentException::class)
    fun negativeTimeRejected() { EventPayload("foul", -1) }

    @Test(expected = IllegalArgumentException::class)
    fun idCollisionRejected() {
        ledger().record("g1", goal.copy(side = MatchSide.AWAY), "operator", 2000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun voidedEventCannotBeRestoredByCorrection() {
        ledger().void("g1", 1, "coach", 2000, "Anular")
            .correct("g1", 2, goal, "coach", 3000, "Restaurar")
    }

    private fun finishedLedger() = ledger()
        .record("start", EventPayload("period_start", 0), "operator", 500)
        .record("end", EventPayload("period_end", 5400), "operator", 6000)

    @Test fun narrativeUsesLatestActiveFactsInChronologicalOrder() {
        val ledger = finishedLedger().void("g1", 1, "coach", 7000, "Anular")
        assertEquals(listOf("start", "end"),
            ledger.narrativeFacts(MatchStatus.FINISHED).map { it.eventId })
    }

    @Test(expected = IllegalArgumentException::class)
    fun liveMatchCannotGenerateNarrative() {
        finishedLedger().narrativeFacts(MatchStatus.LIVE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingStartAndEndCannotGenerateNarrative() {
        ledger().narrativeFacts(MatchStatus.FINISHED)
    }

    @Test(expected = IllegalArgumentException::class)
    fun annulledEndCannotEnableNarrative() {
        finishedLedger().void("end", 1, "coach", 7000, "Aún no finalizó")
            .narrativeFacts(MatchStatus.FINISHED)
    }
}
