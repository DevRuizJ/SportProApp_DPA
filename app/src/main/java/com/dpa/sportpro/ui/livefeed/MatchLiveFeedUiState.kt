package com.dpa.sportpro.ui.livefeed

import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchStatus

data class MatchLiveFeedUiState(
    val matchDateText: String = "Sábado 24 de Octubre",
    val homeTeamName: String = "SportPro FC",
    val awayTeamName: String = "Inter Lima",
    val homeScore: Int = 2,
    val awayScore: Int = 1,
    val matchPeriodText: String = "Segundo Tiempo",
    val timerSeconds: Int = 4043,
    val isTimerRunning: Boolean = true,
    val matchStatus: MatchStatus = MatchStatus.IN_PROGRESS,
    val events: List<MatchEvent> = emptyList(),
)
