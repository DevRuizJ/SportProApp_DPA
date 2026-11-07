package com.dpa.sportpro.ui.matchevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.data.repository.MatchEventsRepository
import com.dpa.sportpro.data.repository.MatchEventsRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MatchEventsViewModel(
    private val repository: MatchEventsRepository = MatchEventsRepositoryImpl.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchEventsUiState())
    val uiState: StateFlow<MatchEventsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.events.collect { eventList ->
                _uiState.update {
                    it.copy(
                        events = eventList,
                        homeScore =
                            eventList.count {
                                it.eventType == EventType.GOAL &&
                                    it.teamName == _uiState.value.homeTeamName &&
                                    it.status != MatchEventStatus.ANNULLED
                            },
                        awayScore =
                            eventList.count {
                                it.eventType == EventType.GOAL &&
                                    it.teamName == _uiState.value.awayTeamName &&
                                    it.status != MatchEventStatus.ANNULLED
                            },
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.currentMatch.collect { match ->
                if (match != null) {
                    _uiState.update {
                        it.copy(
                            homeTeamName = match.homeTeamName,
                            awayTeamName = match.awayTeamName,
                            selectedTeamName =
                                if (
                                    it.selectedTeamName !in
                                        listOf(match.homeTeamName, match.awayTeamName)
                                ) {
                                    match.homeTeamName
                                } else {
                                    it.selectedTeamName
                                },
                            matchStatus = match.status,
                            isTimerRunning =
                                match.status == MatchStatus.IN_PROGRESS && it.isTimerRunning,
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.isTimerRunning) {
                    _uiState.update { it.copy(timerSeconds = it.timerSeconds + 1) }
                }
            }
        }
    }

    fun toggleTimer() {
        _uiState.update { it.copy(isTimerRunning = !it.isTimerRunning) }
    }

    fun toggleExtraTime() {
        _uiState.update { it.copy(isExtraTime = !it.isExtraTime) }
    }

    fun openQuickAddEventDialog(eventType: EventType) {
        if (_uiState.value.matchStatus != MatchStatus.IN_PROGRESS) return
        val currentMinute = (_uiState.value.timerSeconds / 60).coerceAtLeast(1)
        _uiState.update {
            it.copy(
                selectedEventType = eventType,
                eventMinuteText = currentMinute.toString(),
                playerName = "",
                isUnidentifiedPlayer = false,
                assistantPlayerName = "",
                eventNotes = "",
                eventError = null,
                showAddEventDialog = true,
            )
        }
    }

    fun closeAddEventDialog() {
        _uiState.update { it.copy(showAddEventDialog = false) }
    }

    fun onMinuteTextChanged(minute: String) {
        _uiState.update { it.copy(eventMinuteText = minute, eventError = null) }
    }

    fun onTeamSelected(teamName: String) {
        _uiState.update { it.copy(selectedTeamName = teamName) }
    }

    fun onPlayerNameChanged(name: String) {
        _uiState.update { it.copy(playerName = name) }
    }

    fun onToggleUnidentifiedPlayer(unidentified: Boolean) {
        _uiState.update {
            it.copy(
                isUnidentifiedPlayer = unidentified,
                playerName = if (unidentified) "Jugador no identificado" else "",
            )
        }
    }

    fun onAssistantPlayerNameChanged(name: String) {
        _uiState.update { it.copy(assistantPlayerName = name) }
    }

    fun onEventNotesChanged(notes: String) {
        _uiState.update { it.copy(eventNotes = notes) }
    }

    fun saveEvent() {
        if (_uiState.value.matchStatus != MatchStatus.IN_PROGRESS) return
        val state = _uiState.value
        val minuteInt = state.eventMinuteText.toIntOrNull()
        if (minuteInt == null || minuteInt < 1) {
            _uiState.update { it.copy(eventError = "Ingresa un minuto válido mayor que cero.") }
            return
        }

        val finalPlayerName =
            if (state.isUnidentifiedPlayer || state.playerName.isBlank()) {
                "Jugador no identificado"
            } else {
                state.playerName
            }

        val newEvent =
            MatchEvent(
                id = "evt_${System.currentTimeMillis()}",
                minute = minuteInt,
                eventType = state.selectedEventType,
                teamName = state.selectedTeamName,
                playerName = finalPlayerName,
                assistantPlayerName = state.assistantPlayerName.ifBlank { null },
                notes = state.eventNotes,
            )

        repository.addEvent(newEvent)

        _uiState.update {
            it.copy(
                showAddEventDialog = false,
                eventError = null,
                successMessage =
                    "Evento '${state.selectedEventType.displayName}' registrado a los ${minuteInt}'",
            )
        }
    }

    fun openCorrectionDialog(event: MatchEvent, isAnnul: Boolean) {
        _uiState.update {
            it.copy(
                selectedEventForCorrection = event,
                isAnnulAction = isAnnul,
                correctionReason = "",
                showCorrectionDialog = true,
            )
        }
    }

    fun closeCorrectionDialog() {
        _uiState.update { it.copy(showCorrectionDialog = false) }
    }

    fun onCorrectionReasonChanged(reason: String) {
        _uiState.update { it.copy(correctionReason = reason) }
    }

    fun confirmCorrection(operatorUserId: String) {
        val state = _uiState.value
        val event = state.selectedEventForCorrection ?: return

        if (state.isAnnulAction) {
            repository.annulEvent(
                eventId = event.id,
                userId = operatorUserId,
                reason = state.correctionReason.ifBlank { "Anulado por operador en vivo" },
            )
        } else {
            repository.updateEvent(
                event = event.copy(notes = "${event.notes} (Corregido)"),
                userId = operatorUserId,
                reason = state.correctionReason.ifBlank { "Corregido por operador en vivo" },
            )
        }

        _uiState.update {
            it.copy(
                showCorrectionDialog = false,
                successMessage =
                    if (state.isAnnulAction) "Evento anulado conservando la trazabilidad."
                    else "Evento corregido en la bitácora.",
            )
        }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun finishMatch() {
        if (_uiState.value.matchStatus != MatchStatus.IN_PROGRESS) return
        repository.finishCurrentMatch((_uiState.value.timerSeconds / 60).coerceAtLeast(90))
        _uiState.update { it.copy(isTimerRunning = false) }
    }

    fun openNewMatchDialog() {
        _uiState.update {
            it.copy(
                showNewMatchDialog = true,
                newHomeTeamName = "",
                newAwayTeamName = "",
                newStartingPlayersText = "",
                newMatchError = null,
            )
        }
    }

    fun closeNewMatchDialog() {
        _uiState.update { it.copy(showNewMatchDialog = false, newMatchError = null) }
    }

    fun onNewHomeTeamNameChanged(name: String) {
        _uiState.update { it.copy(newHomeTeamName = name, newMatchError = null) }
    }

    fun onNewAwayTeamNameChanged(name: String) {
        _uiState.update { it.copy(newAwayTeamName = name, newMatchError = null) }
    }

    fun onNewStartingPlayersChanged(names: String) {
        _uiState.update { it.copy(newStartingPlayersText = names, newMatchError = null) }
    }

    fun createNewMatch() {
        val state = _uiState.value
        try {
            repository.startNewMatch(
                homeTeamName = state.newHomeTeamName,
                awayTeamName = state.newAwayTeamName,
                startingPlayers =
                    state.newStartingPlayersText
                        .split(",", "\n")
                        .map(String::trim)
                        .filter(String::isNotEmpty),
            )
            _uiState.update {
                it.copy(
                    showNewMatchDialog = false,
                    timerSeconds = 0,
                    isTimerRunning = true,
                    homeScore = 0,
                    awayScore = 0,
                )
            }
        } catch (error: IllegalArgumentException) {
            _uiState.update {
                it.copy(newMatchError = error.message ?: "Revisa los datos del partido.")
            }
        }
    }
}
