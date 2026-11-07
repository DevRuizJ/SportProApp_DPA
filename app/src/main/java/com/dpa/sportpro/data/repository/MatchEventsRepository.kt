package com.dpa.sportpro.data.repository

import android.content.Context
import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchRecord
import com.dpa.sportpro.data.model.MatchStatus
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

interface MatchEventsRepository {
    val events: StateFlow<List<MatchEvent>>
    val matches: StateFlow<List<MatchRecord>>
    val currentMatch: StateFlow<MatchRecord?>

    fun addEvent(event: MatchEvent)

    fun annulEvent(eventId: String, userId: String, reason: String)

    fun updateEvent(event: MatchEvent, userId: String, reason: String)

    fun finishCurrentMatch(durationMinutes: Int)

    fun startNewMatch(homeTeamName: String, awayTeamName: String, startingPlayers: List<String>)
}

class MatchEventsRepositoryImpl(context: Context? = null) : MatchEventsRepository {

    private val preferences = context?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val initialMatches = loadMatches()
    private val _matches = MutableStateFlow(initialMatches)
    private var currentMatchId =
        preferences?.getString(CURRENT_MATCH_KEY, null)?.takeIf { id ->
            initialMatches.any { it.id == id }
        }
            ?: initialMatches.lastOrNull { it.status == MatchStatus.IN_PROGRESS }?.id
            ?: initialMatches.lastOrNull()?.id

    private val _currentMatch =
        MutableStateFlow(initialMatches.firstOrNull { it.id == currentMatchId })
    private val _events = MutableStateFlow(_currentMatch.value?.events.orEmpty())

    override val events: StateFlow<List<MatchEvent>> = _events.asStateFlow()
    override val matches: StateFlow<List<MatchRecord>> = _matches.asStateFlow()
    override val currentMatch: StateFlow<MatchRecord?> = _currentMatch.asStateFlow()

    override fun addEvent(event: MatchEvent) {
        val match = _currentMatch.value ?: error("No hay un partido activo.")
        check(match.status == MatchStatus.IN_PROGRESS) {
            "No se pueden registrar eventos en un partido finalizado."
        }
        require(event.minute > 0) { "El minuto del evento debe ser mayor que cero." }

        updateCurrentMatch(match.copy(events = listOf(event) + match.events))
    }

    override fun annulEvent(eventId: String, userId: String, reason: String) {
        updateEventStatus(eventId) { event ->
            event.copy(
                status = MatchEventStatus.ANNULLED,
                modifiedByUserId = userId,
                modifiedAtMillis = System.currentTimeMillis(),
                correctionReason = reason,
            )
        }
    }

    override fun updateEvent(event: MatchEvent, userId: String, reason: String) {
        updateEventStatus(event.id) {
            event.copy(
                status = MatchEventStatus.CORRECTED,
                modifiedByUserId = userId,
                modifiedAtMillis = System.currentTimeMillis(),
                correctionReason = reason,
            )
        }
    }

    override fun finishCurrentMatch(durationMinutes: Int) {
        val match = _currentMatch.value ?: error("No hay un partido activo.")
        check(match.status == MatchStatus.IN_PROGRESS) { "El partido ya está finalizado." }
        val actualDuration = maxOf(durationMinutes, match.events.maxOfOrNull { it.minute } ?: 0)
        require(actualDuration > 0) { "La duración del partido debe ser mayor que cero." }
        val eventsWithStart =
            if (
                match.events.any {
                    it.eventType == EventType.MATCH_START && it.status != MatchEventStatus.ANNULLED
                }
            ) {
                match.events
            } else {
                listOf(
                    MatchEvent(
                        id = UUID.randomUUID().toString(),
                        minute = 0,
                        eventType = EventType.MATCH_START,
                        teamName = match.homeTeamName,
                        playerName = "Registro automático",
                    )
                ) + match.events
            }
        val finishedEvents =
            if (
                eventsWithStart.any {
                    it.eventType == EventType.MATCH_END && it.status != MatchEventStatus.ANNULLED
                }
            ) {
                eventsWithStart
            } else {
                listOf(
                    MatchEvent(
                        id = UUID.randomUUID().toString(),
                        minute = actualDuration,
                        eventType = EventType.MATCH_END,
                        teamName = match.homeTeamName,
                        playerName = "Registro automático",
                    )
                ) + eventsWithStart
            }

        updateCurrentMatch(
            match.copy(
                status = MatchStatus.FINISHED,
                durationMinutes = actualDuration,
                events = finishedEvents,
                finishedAtMillis = System.currentTimeMillis(),
            )
        )
    }

    override fun startNewMatch(
        homeTeamName: String,
        awayTeamName: String,
        startingPlayers: List<String>,
    ) {
        check(_currentMatch.value?.status != MatchStatus.IN_PROGRESS) {
            "Finaliza el partido actual antes de iniciar otro."
        }
        val home = homeTeamName.trim()
        val away = awayTeamName.trim()
        val starters = startingPlayers.map(String::trim).filter(String::isNotEmpty).distinct()
        require(home.isNotEmpty() && away.isNotEmpty()) { "Ingresa los nombres de ambos equipos." }
        require(!home.equals(away, ignoreCase = true)) {
            "Los equipos deben tener nombres distintos."
        }
        require(starters.isNotEmpty()) { "Ingresa al menos un jugador titular." }

        val match =
            MatchRecord(
                id = UUID.randomUUID().toString(),
                homeTeamName = home,
                awayTeamName = away,
                status = MatchStatus.IN_PROGRESS,
                durationMinutes = 0,
                startingPlayers = starters,
                events =
                    listOf(
                        MatchEvent(
                            id = UUID.randomUUID().toString(),
                            minute = 0,
                            eventType = EventType.MATCH_START,
                            teamName = home,
                            playerName = "Registro automático",
                        )
                    ),
                startedAtMillis = System.currentTimeMillis(),
            )
        currentMatchId = match.id
        publishMatches(_matches.value + match)
    }

    private fun updateEventStatus(eventId: String, transform: (MatchEvent) -> MatchEvent) {
        val match = _currentMatch.value ?: error("No hay un partido activo.")
        check(match.events.any { it.id == eventId }) { "No se encontró el evento." }
        updateCurrentMatch(
            match.copy(
                events =
                    match.events.map { event ->
                        if (event.id == eventId) transform(event) else event
                    }
            )
        )
    }

    private fun updateCurrentMatch(updated: MatchRecord) {
        publishMatches(
            _matches.value.map { match -> if (match.id == updated.id) updated else match }
        )
    }

    private fun publishMatches(updated: List<MatchRecord>) {
        _matches.value = updated
        _currentMatch.value = updated.firstOrNull { it.id == currentMatchId }
        _events.value = _currentMatch.value?.events.orEmpty()
        persistMatches(updated)
    }

    private fun loadMatches(): List<MatchRecord> {
        val saved = preferences?.getString(MATCHES_KEY, null) ?: return listOf(sampleMatch())
        val json = JSONArray(saved)
        return List(json.length()) { index -> json.getJSONObject(index).toMatchRecord() }
    }

    private fun persistMatches(matches: List<MatchRecord>) {
        val json = JSONArray()
        matches.forEach { match -> json.put(match.toJson()) }
        preferences
            ?.edit()
            ?.putString(MATCHES_KEY, json.toString())
            ?.putString(CURRENT_MATCH_KEY, currentMatchId)
            ?.apply()
    }

    private fun JSONObject.toMatchRecord(): MatchRecord {
        val eventArray = getJSONArray("events")
        val eventList =
            List(eventArray.length()) { index ->
                val value = eventArray.getJSONObject(index)
                MatchEvent(
                    id = value.getString("id"),
                    minute = value.getInt("minute"),
                    eventType = EventType.valueOf(value.getString("eventType")),
                    teamName = value.getString("teamName"),
                    playerName = value.optString("playerName"),
                    assistantPlayerName = value.optString("assistantPlayerName").ifBlank { null },
                    notes = value.optString("notes"),
                    status = MatchEventStatus.valueOf(value.getString("status")),
                    modifiedByUserId = value.optString("modifiedByUserId").ifBlank { null },
                    modifiedAtMillis =
                        value.takeIf { has("modifiedAtMillis") }?.getLong("modifiedAtMillis"),
                    correctionReason = value.optString("correctionReason").ifBlank { null },
                )
            }
        return MatchRecord(
            id = getString("id"),
            homeTeamName = getString("homeTeamName"),
            awayTeamName = getString("awayTeamName"),
            status = MatchStatus.valueOf(getString("status")),
            durationMinutes = getInt("durationMinutes"),
            startingPlayers =
                getJSONArray("startingPlayers").let { players ->
                    List(players.length()) { index -> players.getString(index) }
                },
            events = eventList,
            startedAtMillis = getLong("startedAtMillis"),
            finishedAtMillis = takeIf { has("finishedAtMillis") }?.getLong("finishedAtMillis"),
        )
    }

    private fun MatchRecord.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("homeTeamName", homeTeamName)
            .put("awayTeamName", awayTeamName)
            .put("status", status.name)
            .put("durationMinutes", durationMinutes)
            .put("startingPlayers", JSONArray(startingPlayers))
            .put("startedAtMillis", startedAtMillis)
            .apply {
                finishedAtMillis?.let { put("finishedAtMillis", it) }
                put(
                    "events",
                    JSONArray().apply {
                        events.forEach { event ->
                            put(
                                JSONObject()
                                    .put("id", event.id)
                                    .put("minute", event.minute)
                                    .put("eventType", event.eventType.name)
                                    .put("teamName", event.teamName)
                                    .put("playerName", event.playerName)
                                    .put("assistantPlayerName", event.assistantPlayerName)
                                    .put("notes", event.notes)
                                    .put("status", event.status.name)
                                    .put("modifiedByUserId", event.modifiedByUserId)
                                    .put("modifiedAtMillis", event.modifiedAtMillis)
                                    .put("correctionReason", event.correctionReason)
                            )
                        }
                    },
                )
            }

    private fun sampleMatch() =
        MatchRecord(
            id = "demo_match_1",
            homeTeamName = "SportPro FC",
            awayTeamName = "Inter Lima",
            status = MatchStatus.IN_PROGRESS,
            durationMinutes = 0,
            startingPlayers = listOf("Mateo Silva", "Thiago Rossi", "Lucas Castro"),
            events =
                listOf(
                    MatchEvent(
                        id = "evt_1",
                        minute = 65,
                        eventType = EventType.GOAL,
                        teamName = "SportPro FC",
                        playerName = "Mateo Silva",
                        assistantPlayerName = "Thiago Rossi",
                        notes = "Gol de jugada colectiva",
                    ),
                    MatchEvent(
                        id = "evt_2",
                        minute = 48,
                        eventType = EventType.YELLOW_CARD,
                        teamName = "SportPro FC",
                        playerName = "Lucas Castro",
                        notes = "Falta táctica en mediocampo",
                    ),
                    MatchEvent(
                        id = "evt_3",
                        minute = 30,
                        eventType = EventType.GOAL,
                        teamName = "Inter Lima",
                        playerName = "Jugador no identificado",
                        notes = "Anotación de tiro libre",
                    ),
                    MatchEvent(
                        id = "evt_start",
                        minute = 0,
                        eventType = EventType.MATCH_START,
                        teamName = "SportPro FC",
                        playerName = "Registro automático",
                    ),
                ),
            startedAtMillis = System.currentTimeMillis(),
        )

    companion object {
        private const val PREFERENCES_NAME = "sportpro_match_events"
        private const val MATCHES_KEY = "matches"
        private const val CURRENT_MATCH_KEY = "current_match_id"

        @Volatile private var instance: MatchEventsRepositoryImpl? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = MatchEventsRepositoryImpl(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): MatchEventsRepositoryImpl =
            instance
                ?: synchronized(this) {
                    instance ?: MatchEventsRepositoryImpl().also { instance = it }
                }
    }
}
