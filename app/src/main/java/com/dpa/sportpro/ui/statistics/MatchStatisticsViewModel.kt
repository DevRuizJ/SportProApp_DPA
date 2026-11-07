package com.dpa.sportpro.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.data.repository.MatchEventsRepository
import com.dpa.sportpro.data.repository.MatchEventsRepositoryImpl
import com.dpa.sportpro.data.repository.MatchStatisticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MatchStatisticsViewModel(
    private val repository: MatchEventsRepository = MatchEventsRepositoryImpl.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchStatisticsUiState())
    val uiState: StateFlow<MatchStatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.matches.collect { matches ->
                val teams = MatchStatisticsCalculator.calculateTeams(matches)
                val players = MatchStatisticsCalculator.calculatePlayers(matches)
                val selectedTeamName =
                    _uiState.value.selectedTeamName?.takeIf { name ->
                        teams.any { it.teamName == name }
                    }
                        ?: matches.firstOrNull { it.status == MatchStatus.FINISHED }?.homeTeamName
                        ?: teams.firstOrNull()?.teamName
                _uiState.update {
                    it.copy(
                        teams = teams,
                        players = players,
                        selectedTeamName = selectedTeamName,
                        finishedMatchesCount = matches.count { it.status == MatchStatus.FINISHED },
                    )
                }
            }
        }
    }

    fun selectTab(tab: StatisticsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectTeam(teamName: String) {
        _uiState.update { it.copy(selectedTeamName = teamName) }
    }
}
