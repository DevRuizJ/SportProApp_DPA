package com.dpa.sportpro

import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchRecord
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.data.repository.MatchEventsRepository
import com.dpa.sportpro.ui.livefeed.MatchLiveFeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchLiveFeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepository: FakeMatchEventsRepository
    private lateinit var viewModel: MatchLiveFeedViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = FakeMatchEventsRepository()
        viewModel = MatchLiveFeedViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun eventsAreSortedDescendingByMinute() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.events.size)
        assertTrue(state.events[0].minute >= state.events[1].minute)
        assertTrue(state.events[1].minute >= state.events[2].minute)
    }

    @Test
    fun scoreIgnoresAnnulledGoals() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(1, state.homeScore)

        mockRepository.annulEvent("evt_1", "operator_1", "VAR Revision")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(0, state.homeScore)
    }
}

class FakeMatchEventsRepository : MatchEventsRepository {
    private val initialList =
        listOf(
            MatchEvent("evt_1", 65, EventType.GOAL, "SportPro FC", "Mateo Silva"),
            MatchEvent("evt_2", 48, EventType.YELLOW_CARD, "SportPro FC", "Lucas Castro"),
            MatchEvent("evt_3", 30, EventType.GOAL, "Inter Lima", "Jugador no identificado"),
        )

    private val _events = MutableStateFlow(initialList)
    private val initialMatch =
        MatchRecord(
            id = "test_match",
            homeTeamName = "SportPro FC",
            awayTeamName = "Inter Lima",
            status = MatchStatus.IN_PROGRESS,
            durationMinutes = 0,
            startingPlayers = listOf("Mateo Silva", "Lucas Castro"),
            events = initialList,
            startedAtMillis = 0,
        )
    private val _matches = MutableStateFlow(listOf(initialMatch))
    private val _currentMatch = MutableStateFlow(initialMatch)
    override val events: StateFlow<List<MatchEvent>> = _events
    override val matches: StateFlow<List<MatchRecord>> = _matches
    override val currentMatch: StateFlow<MatchRecord?> = _currentMatch

    override fun addEvent(event: MatchEvent) {
        _events.value = listOf(event) + _events.value
        publishCurrentEvents()
    }

    override fun annulEvent(eventId: String, userId: String, reason: String) {
        _events.value =
            _events.value.map {
                if (it.id == eventId) it.copy(status = MatchEventStatus.ANNULLED) else it
            }
        publishCurrentEvents()
    }

    override fun updateEvent(event: MatchEvent, userId: String, reason: String) {
        _events.value = _events.value.map { if (it.id == event.id) event else it }
        publishCurrentEvents()
    }

    override fun finishCurrentMatch(durationMinutes: Int) {
        updateMatch(
            _currentMatch.value!!.copy(
                status = MatchStatus.FINISHED,
                durationMinutes = durationMinutes,
            )
        )
    }

    override fun startNewMatch(
        homeTeamName: String,
        awayTeamName: String,
        startingPlayers: List<String>,
    ) {
        val match =
            MatchRecord(
                id = "next_match",
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                status = MatchStatus.IN_PROGRESS,
                durationMinutes = 0,
                startingPlayers = startingPlayers,
                events = emptyList(),
                startedAtMillis = 0,
            )
        _events.value = emptyList()
        updateMatch(match)
    }

    private fun publishCurrentEvents() {
        updateMatch(_currentMatch.value!!.copy(events = _events.value))
    }

    private fun updateMatch(match: MatchRecord) {
        _currentMatch.value = match
        _matches.value = _matches.value.filterNot { it.id == match.id } + match
    }
}
