package com.dpa.sportpro

import com.dpa.sportpro.domain.match.*
import org.junit.Assert.*
import org.junit.Test

class LiveMatchTest {
    @Test fun forgottenClockStopsAtSupportedLimitAndCanStillFinish() {
        val running = MatchClock().transition(ClockAction.START, 1000)
        assertEquals(14_400L, running.elapsed(18_001_000))
        val half = running.transition(ClockAction.END_PERIOD, 18_001_000)
        val second = half.transition(ClockAction.START, 18_002_000)
        val finished = second.transition(ClockAction.FINISH, 18_003_000)
        assertEquals(ClockPhase.FINISHED, finished.phase)
        assertEquals(14_400L, finished.accumulatedSeconds)
    }
    @Test fun clockSurvivesReconstructionAndDoesNotDependOnTickCount() {
        val running = MatchClock().transition(ClockAction.START, 100_000)
        assertEquals(90, running.elapsed(190_000))
        val restored = running.copy()
        assertEquals(390, restored.elapsed(490_000))
        assertEquals(0, restored.elapsed(1))
    }
    @Test fun pausesExcludeBreakAndSecondPeriodContinuesAccumulatedTime() {
        var clock = MatchClock().transition(ClockAction.START, 100_000)
        clock = clock.transition(ClockAction.PAUSE, 160_000)
        assertEquals(60, clock.elapsed(500_000))
        clock = clock.transition(ClockAction.RESUME, 500_000)
        clock = clock.transition(ClockAction.END_PERIOD, 560_000)
        assertEquals(120, clock.elapsed(900_000))
        clock = clock.transition(ClockAction.START, 900_000)
        clock = clock.transition(ClockAction.FINISH, 960_000)
        assertEquals(180, clock.elapsed(999_999_999))
    }
    @Test fun incompatiblePeriodTransitionsAreRejected() {
        for (action in listOf(ClockAction.RESUME, ClockAction.FINISH, ClockAction.END_PERIOD, ClockAction.PAUSE)) {
            try { MatchClock().transition(action, 1000); fail("Illegal transition") } catch (_: IllegalStateException) {}
        }
    }
    @Test fun duplicateSnapshotsAndVoidedGoalsDoNotInflateScore() {
        val original = LiveEvent("g", "op1", 1, "goal", 20, "HOME", "", "", "Jugada", "",
            false, "coach", "", 100L, 1)
        val corrected = original.copy(version=2, operationId="op2", side="AWAY")
        assertEquals(Score(0,1), LiveEventTypes.score(listOf(original,original,corrected,corrected)))
        assertEquals(Score(), LiveEventTypes.score(listOf(original,corrected,corrected.copy(version=3,voided=true))))
    }
    @Test fun unidentifiedPlayersAcceptedButKnownIdenticalSubstitutionRejected() {
        LiveEventTypes.validate("substitution", 70, "HOME", "", "", "")
        LiveEventTypes.validate("yellow_card", 10, "AWAY", "", "", "Falta")
        try { LiveEventTypes.validate("substitution", 70,"HOME","p1","p1",""); fail() }
        catch (_: IllegalArgumentException) {}
    }
    @Test fun goalRequiresTeamAndDetail() {
        for (side in listOf("", "UNKNOWN")) {
            try { LiveEventTypes.validate("goal",20,side,"","","Jugada"); fail() } catch (_: IllegalArgumentException) {}
        }
        try { LiveEventTypes.validate("goal",20,"HOME","","",""); fail() } catch (_: IllegalArgumentException) {}
    }
}
