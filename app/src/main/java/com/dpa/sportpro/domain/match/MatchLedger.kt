package com.dpa.sportpro.domain.match

/**
 * Pure domain rules. Persistence and authorization belong to the repository/backend.
 * eventId is stable across retries; revisions must be committed with an expected-version check.
 */
enum class MatchSide { HOME, AWAY }
enum class MatchStatus { SCHEDULED, LIVE, FINISHED }
enum class EventStatus { ACTIVE, VOIDED }

data class EventPayload(
    val typeId: String,
    val elapsedSeconds: Int,
    val side: MatchSide? = null,
    val playerId: String? = null,
    val observations: String = "",
    val fields: Map<String, String> = emptyMap(),
) {
    init {
        require(typeId.isNotBlank()) { "Selecciona el tipo de evento." }
        require(elapsedSeconds >= 0) { "El tiempo no puede ser negativo." }
        require(playerId == null || playerId.isNotBlank()) { "Usa null para jugador no identificado." }
        require(typeId != "goal" || side != null) { "El gol requiere equipo." }
        require(observations.length <= 2000) { "Observaciones demasiado largas." }
    }
}

data class EventRevision(
    val eventId: String,
    val revision: Int,
    val payload: EventPayload,
    val status: EventStatus,
    val actorUid: String,
    val recordedAtMillis: Long,
    val reason: String,
)

data class Score(val home: Int = 0, val away: Int = 0)

/** Append-only history; corrections never erase the original action. */
class MatchLedger private constructor(val history: List<EventRevision>) {
    constructor() : this(emptyList())

    val currentEvents: List<EventRevision>
        get() = history.groupBy { it.eventId }.values.map { versions ->
            versions.maxBy { it.revision }
        }.sortedWith(compareByDescending<EventRevision> { it.payload.elapsedSeconds }
            .thenBy { it.eventId })

    val score: Score
        get() {
            val goals = currentEvents.filter {
                it.status == EventStatus.ACTIVE && it.payload.typeId == "goal"
            }
            return Score(
                home = goals.count { it.payload.side == MatchSide.HOME },
                away = goals.count { it.payload.side == MatchSide.AWAY },
            )
        }

    fun record(
        eventId: String,
        payload: EventPayload,
        actorUid: String,
        recordedAtMillis: Long,
    ): MatchLedger {
        validateActor(actorUid, recordedAtMillis)
        require(eventId.isNotBlank()) { "El evento requiere un ID estable." }
        val existing = history.firstOrNull { it.eventId == eventId }
        if (existing != null) {
            require(existing.payload == payload && existing.actorUid == actorUid) {
                "El ID ya corresponde a otro evento."
            }
            return this // Retry of the original command, even after a later correction.
        }
        return MatchLedger(history + EventRevision(
            eventId, 1, payload, EventStatus.ACTIVE, actorUid, recordedAtMillis, "",
        ))
    }

    fun correct(
        eventId: String,
        expectedRevision: Int,
        payload: EventPayload,
        actorUid: String,
        recordedAtMillis: Long,
        reason: String,
    ): MatchLedger = revise(
        eventId, expectedRevision, payload, EventStatus.ACTIVE,
        actorUid, recordedAtMillis, reason,
    )

    fun void(
        eventId: String,
        expectedRevision: Int,
        actorUid: String,
        recordedAtMillis: Long,
        reason: String,
    ): MatchLedger {
        val current = currentEvents.singleOrNull { it.eventId == eventId }
            ?: error("Evento no encontrado.")
        require(current.status != EventStatus.VOIDED) { "El evento ya está anulado." }
        return revise(
            eventId, expectedRevision, current.payload, EventStatus.VOIDED,
            actorUid, recordedAtMillis, reason,
        )
    }

    private fun revise(
        eventId: String,
        expectedRevision: Int,
        payload: EventPayload,
        status: EventStatus,
        actorUid: String,
        recordedAtMillis: Long,
        reason: String,
    ): MatchLedger {
        validateActor(actorUid, recordedAtMillis)
        require(reason.isNotBlank()) { "Indica el motivo de la corrección o anulación." }
        val current = currentEvents.singleOrNull { it.eventId == eventId }
            ?: error("Evento no encontrado.")
        require(current.revision == expectedRevision) {
            "El evento cambió. Revisa su última versión antes de corregirlo."
        }
        require(current.status != EventStatus.VOIDED) {
            "Un evento anulado no se puede modificar; registra uno nuevo."
        }
        return MatchLedger(history + EventRevision(
            eventId, expectedRevision + 1, payload, status,
            actorUid, recordedAtMillis, reason.trim(),
        ))
    }

    /**
     * Sanitized facts for a future server-side AI integration.
     * Free-form observations are deliberately excluded (privacy and prompt injection).
     * This is NOT generated prose and does not claim to be an AI service.
     */
    fun narrativeFacts(status: MatchStatus): List<NarrativeFact> {
        require(status == MatchStatus.FINISHED) { "El partido debe estar finalizado." }
        val active = currentEvents.filter { it.status == EventStatus.ACTIVE }
            .sortedWith(compareBy<EventRevision> { it.payload.elapsedSeconds }.thenBy { it.eventId })
        val firstStart = active.firstOrNull { it.payload.typeId == "period_start" }
        val lastEnd = active.lastOrNull { it.payload.typeId == "period_end" }
        require(firstStart != null && lastEnd != null &&
            firstStart.payload.elapsedSeconds <= lastEnd.payload.elapsedSeconds) {
            "Se requieren registros válidos de inicio y fin."
        }
        return active.map {
            NarrativeFact(
                it.eventId, it.revision, it.payload.typeId, it.payload.elapsedSeconds,
                it.payload.side, it.payload.playerId,
            )
        }
    }

    private fun validateActor(actorUid: String, recordedAtMillis: Long) {
        require(actorUid.isNotBlank()) { "Se requiere un usuario autenticado." }
        require(recordedAtMillis >= 0) { "Fecha de registro inválida." }
    }
}

data class NarrativeFact(
    val eventId: String,
    val revision: Int,
    val typeId: String,
    val elapsedSeconds: Int,
    val side: MatchSide?,
    val playerId: String?,
)
