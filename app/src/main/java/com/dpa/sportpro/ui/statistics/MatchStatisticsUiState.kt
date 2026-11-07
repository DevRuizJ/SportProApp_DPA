package com.dpa.sportpro.ui.statistics

import com.dpa.sportpro.data.model.PlayerTournamentStatistics
import com.dpa.sportpro.data.model.TeamStatistics

enum class StatisticsTab {
    TEAM,
    PLAYERS,
}

data class MatchStatisticsUiState(
    val selectedTab: StatisticsTab = StatisticsTab.TEAM,
    val teams: List<TeamStatistics> = emptyList(),
    val players: List<PlayerTournamentStatistics> = emptyList(),
    val selectedTeamName: String? = null,
    val finishedMatchesCount: Int = 0,
)
