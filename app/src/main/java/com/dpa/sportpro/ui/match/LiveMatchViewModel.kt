package com.dpa.sportpro.ui.match

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.local.PendingMatchEvent
import com.dpa.sportpro.data.repository.*
import com.dpa.sportpro.domain.match.*
import com.dpa.sportpro.model.UserRole
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class LiveMatchUiState(
    val matches: List<LiveMatch> = emptyList(),
    val selectedId: String? = null,
    val match: LiveMatch? = null,
    val events: List<LiveEvent> = emptyList(),
    val pending: List<PendingMatchEvent> = emptyList(),
    val catalog: RemoteCatalog = RemoteCatalog(),
    val seconds: Long = 0,
    val calibrated: Boolean = false,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val completedAction: Int = 0,
    val history: List<LiveEvent>? = null,
)

class LiveMatchViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val repository = LiveMatchRepository(application)
    private val mutable = MutableStateFlow(LiveMatchUiState(selectedId = saved["matchId"]))
    val state = mutable.asStateFlow()
    val user = requireNotNull(AuthSession.profile.value)
    val canCreate get() = user.authorizedRole in setOf(UserRole.ADMIN, UserRole.COACH)
    private var selectedJob: Job? = null
    private var clockAnchor: Pair<Long, Long>? = repository.cachedClockAnchor()
    private fun serverNow(): Long = clockAnchor?.let { it.first + SystemClock.elapsedRealtime() - it.second } ?: System.currentTimeMillis()

    init {
        viewModelScope.launch {
            repository.observeMatches().catch { failure(it) }.collect { matches ->
                mutable.update { it.copy(matches = matches, loading = false) }
            }
        }
        state.value.selectedId?.let(::select)
        viewModelScope.launch {
            while (isActive) {
                mutable.update { it.copy(seconds = it.match?.clock?.elapsed(serverNow()) ?: 0) }
                delay(250)
            }
        }
    }

    fun select(id: String) {
        selectedJob?.cancel()
        saved["matchId"] = id
        mutable.update { it.copy(selectedId = id, match = null, events = emptyList(), pending = emptyList(),
            loading = true, error = null, history = null, catalog = RemoteCatalog(emptySet(), 0)) }
        selectedJob = viewModelScope.launch {
            launch { calibrate() }
            launch {
                repository.observeMatch(id).catch { failure(it) }.collect { match ->
                    mutable.update { it.copy(match = match, loading = false,
                        error = if (match == null) "No se encontró el partido o no tienes acceso." else it.error) }
                }
            }
            launch { repository.observeEvents(id).catch { failure(it) }.collect { events -> mutable.update { it.copy(events = events) } } }
            launch {
                repository.pending(id).collect { rows ->
                    mutable.update { previous ->
                        val confirmed = previous.pending.isNotEmpty() && rows.isEmpty()
                        previous.copy(pending = rows, notice = if (confirmed)
                            "Los eventos pendientes fueron confirmados por el servidor." else previous.notice)
                    }
                }
            }
            launch {
                state.map { it.match?.let { m -> m.ownerUid to m.category } }.filterNotNull().distinctUntilChanged()
                    .collectLatest { (owner, category) ->
                        repository.observeCatalog(owner, category).catch { failure(it) }
                            .collect { catalog -> mutable.update { it.copy(catalog = catalog) } }
                    }
            }
        }
    }

    fun backToList() {
        selectedJob?.cancel()
        saved["matchId"] = null
        mutable.update { it.copy(selectedId = null, match = null, history = null, error = null, notice = null) }
    }
    fun dismissHistory() { mutable.update { it.copy(history = null) } }

    private suspend fun calibrate() {
        try {
            clockAnchor = withTimeout(10_000) { repository.calibrateClock() }
            mutable.update { it.copy(calibrated = true) }
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            mutable.update { it.copy(calibrated = clockAnchor != null,
                notice = "Sin calibración nueva: el reloj puede ser aproximado. El registro local sigue disponible.") }
        }
    }

    fun create(home: String, away: String, category: String, minutes: String, viewers: String, operators: String) = action {
        val requestId = saved.get<String>("createId") ?: java.util.UUID.randomUUID().toString().also { saved["createId"] = it }
        val id = repository.create(home, away, category, minutes.toIntOrNull() ?: 0, parseUids(viewers), parseUids(operators), requestId)
        saved.remove<String>("createId")
        select(id)
        mutable.update { it.copy(completedAction = it.completedAction + 1, notice = "Partido guardado y confirmado por el servidor.") }
    }
    private fun parseUids(value: String) = value.split(',', ' ', '\n').map(String::trim).filter(String::isNotEmpty).distinct()

    fun changeClock(action: ClockAction) = action {
        calibrate()
        require(clockAnchor != null) { "Se necesita conexión para controlar el reloj." }
        val match = requireNotNull(state.value.match)
        repository.clock(match, action, serverNow())
        mutable.update { it.copy(notice = "Cambio de reloj confirmado por el servidor.") }
    }

    fun toggleEvent(type: String, enabled: Boolean) = action {
        val s = state.value
        val ids = s.catalog.enabled.toMutableSet().apply { if (enabled) add(type) else remove(type) }
        repository.setCatalog(requireNotNull(s.match), ids, s.catalog.version)
    }

    fun record(type: String, minutes: String, seconds: String, side: String, player: String, other: String,
        detail: String, observations: String, existing: LiveEvent?, voided: Boolean, reason: String) = action {
        val min = minutes.toIntOrNull()
        val sec = seconds.toIntOrNull()
        require(min != null && min in 0..240 && sec != null && sec in 0..59) { "Indica minuto (0–240) y segundo (0–59)." }
        val s = state.value
        repository.enqueue(requireNotNull(s.match), type, min * 60 + sec, side, player, other, detail,
            observations, s.catalog.version, existing, voided, reason)
        mutable.update { it.copy(completedAction = it.completedAction + 1,
            notice = "Guardado en el teléfono. Pendiente hasta recibir confirmación del servidor.") }
        sync()
    }

    fun sync() {
        viewModelScope.launch {
            try { repository.flushPending(user.uid) } catch (e: Exception) { failure(e) }
        }
    }
    fun showHistory(event: LiveEvent) = action {
        val rows = withTimeout(15_000) { repository.history(requireNotNull(state.value.selectedId), event.id) }
        mutable.update { it.copy(history = rows) }
    }

    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try { block() } catch (e: Exception) { failure(e) }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    private fun failure(error: Throwable) {
        if (error is CancellationException && error !is TimeoutCancellationException) throw error
        mutable.update { it.copy(loading = false, error = when (error) {
            is TimeoutCancellationException -> "Sin confirmación de red. Revisa el estado antes de repetir."
            is IllegalArgumentException, is IllegalStateException -> error.message
            else -> "No se pudo acceder al partido. Revisa conexión y permisos."
        }) }
    }
}
