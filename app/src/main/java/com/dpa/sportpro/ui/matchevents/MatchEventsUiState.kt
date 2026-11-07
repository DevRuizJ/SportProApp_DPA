package com.dpa.sportpro.ui.matchevents

import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchStatus

data class MatchEventsUiState(
    val homeTeamName: String = "SportPro FC",
    val awayTeamName: String = "Inter Lima",
    val homeScore: Int = 2,
    val awayScore: Int = 1,
    val timerSeconds: Int = 4043,
    val isTimerRunning: Boolean = true,
    val isExtraTime: Boolean = false,
    val events: List<MatchEvent> = emptyList(),
    val matchStatus: MatchStatus = MatchStatus.IN_PROGRESS,
    val showAddEventDialog: Boolean = false,
    val selectedEventType: EventType = EventType.GOAL,
    val eventMinuteText: String = "67",
    val selectedTeamName: String = "SportPro FC",
    val playerName: String = "",
    val isUnidentifiedPlayer: Boolean = false,
    val assistantPlayerName: String = "",
    val eventNotes: String = "",
    val eventError: String? = null,
    val showCorrectionDialog: Boolean = false,
    val selectedEventForCorrection: MatchEvent? = null,
    val isAnnulAction: Boolean = false,
    val correctionReason: String = "",
    val showNewMatchDialog: Boolean = false,
    val newHomeTeamName: String = "",
    val newAwayTeamName: String = "",
    val newStartingPlayersText: String = "",
    val newMatchError: String? = null,
    val successMessage: String? = null,
)
