package com.dpa.sportpro.ui.livefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.data.repository.MatchEventsRepository
import com.dpa.sportpro.data.repository.MatchEventsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MatchLiveFeedViewModel(
    private val repository: MatchEventsRepository = MatchEventsRepositoryImpl.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchLiveFeedUiState())
    val uiState: StateFlow<MatchLiveFeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.currentMatch.collect { match ->
                if (match == null) return@collect
                val eventList = match.events
                val sortedEvents = eventList.sortedByDescending { it.minute }

                val homeScore =
                    eventList.count {
                        it.eventType == EventType.GOAL &&
                            it.teamName == match.homeTeamName &&
                            it.status != MatchEventStatus.ANNULLED
                    }
                val awayScore =
                    eventList.count {
                        it.eventType == EventType.GOAL &&
                            it.teamName == match.awayTeamName &&
                            it.status != MatchEventStatus.ANNULLED
                    }

                _uiState.update {
                    it.copy(
                        homeTeamName = match.homeTeamName,
                        awayTeamName = match.awayTeamName,
                        events = sortedEvents,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        matchPeriodText =
                            if (match.status == MatchStatus.FINISHED) "Finalizado"
                            else "Segundo Tiempo",
                        matchStatus = match.status,
                        isTimerRunning =
                            match.status == MatchStatus.IN_PROGRESS && it.isTimerRunning,
                    )
                }
            }
        }
    }
}
