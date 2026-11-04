package com.dpa.sportpro.domain.match

enum class ClockPhase { READY, FIRST, PAUSED_FIRST, HALF_TIME, SECOND, PAUSED_SECOND, FINISHED;
    val running get() = this == FIRST || this == SECOND
}
enum class ClockAction { START, PAUSE, RESUME, END_PERIOD, FINISH }

data class MatchClock(
    val phase: ClockPhase = ClockPhase.READY,
    val accumulatedSeconds: Long = 0,
    val startedAtMillis: Long? = null,
    val version: Long = 0,
) {
    fun elapsed(serverNowMillis: Long): Long = (accumulatedSeconds +
        if (phase.running && startedAtMillis != null) ((serverNowMillis - startedAtMillis).coerceAtLeast(0) / 1000) else 0)
        .coerceAtMost(14_400)

    fun transition(action: ClockAction, serverNowMillis: Long): MatchClock {
        val next = when (action) {
            ClockAction.START -> when (phase) {
                ClockPhase.READY -> ClockPhase.FIRST
                ClockPhase.HALF_TIME -> ClockPhase.SECOND
                else -> error("El periodo ya fue iniciado.")
            }
            ClockAction.PAUSE -> when (phase) {
                ClockPhase.FIRST -> ClockPhase.PAUSED_FIRST
                ClockPhase.SECOND -> ClockPhase.PAUSED_SECOND
                else -> error("Solo se puede pausar un periodo activo.")
            }
            ClockAction.RESUME -> when (phase) {
                ClockPhase.PAUSED_FIRST -> ClockPhase.FIRST
                ClockPhase.PAUSED_SECOND -> ClockPhase.SECOND
                else -> error("El partido no está pausado.")
            }
            ClockAction.END_PERIOD -> when (phase) {
                ClockPhase.FIRST, ClockPhase.PAUSED_FIRST -> ClockPhase.HALF_TIME
                else -> error("Finaliza el partido para cerrar el segundo periodo.")
            }
            ClockAction.FINISH -> when (phase) {
                ClockPhase.SECOND, ClockPhase.PAUSED_SECOND -> ClockPhase.FINISHED
                else -> error("El segundo periodo debe haber comenzado.")
            }
        }
        return MatchClock(next, elapsed(serverNowMillis), if (next.running) serverNowMillis else null, version + 1)
    }
}

data class LiveMatch(
    val id: String,
    val ownerUid: String,
    val home: String,
    val away: String,
    val category: String,
    val periodMinutes: Int,
    val viewerUids: List<String>,
    val operatorUids: List<String>,
    val clock: MatchClock,
    val fromCache: Boolean = false,
)

data class LiveEvent(
    val id: String,
    val operationId: String,
    val version: Long,
    val typeId: String,
    val seconds: Int,
    val side: String,
    val player: String,
    val otherPlayer: String,
    val detail: String,
    val observations: String,
    val voided: Boolean,
    val actorUid: String,
    val reason: String,
    val recordedAtMillis: Long?,
    val catalogVersion: Long,
)

object LiveEventTypes {
    val labels = linkedMapOf(
        "period_start" to "Inicio de tiempo", "period_end" to "Fin de tiempo",
        "goal" to "Gol", "goal_kick" to "Saque de meta", "throw_in" to "Saque lateral",
        "corner_kick" to "Tiro de esquina", "foul" to "Falta", "yellow_card" to "Tarjeta amarilla",
        "red_card" to "Tarjeta roja", "penalty" to "Penal", "offside" to "Fuera de juego",
        "substitution" to "Sustitución",
    )
    val periodTypes = setOf("period_start", "period_end")
    fun validate(type: String, seconds: Int, side: String, player: String, other: String, detail: String) {
        require(type in labels && type !in periodTypes) { "Selecciona un evento de juego." }
        require(seconds in 0..14400) { "Minuto fuera de rango (0 a 240)." }
        require(side in setOf("HOME", "AWAY")) { "Selecciona el equipo." }
        require(player.length <= 100 && other.length <= 100 && detail.length <= 200) { "Datos demasiado largos." }
        require(type !in setOf("goal", "yellow_card", "red_card") || detail.isNotBlank()) {
            "Indica el tipo de jugada o motivo."
        }
        require(type != "substitution" || other.isBlank() || player.isBlank() || other != player) {
            "El jugador que entra debe ser distinto al que sale."
        }
    }
    fun score(events: List<LiveEvent>): Score {
        val current = events.groupBy { it.id }.values.map { it.maxBy { e -> e.version } }
        return Score(current.count { !it.voided && it.typeId == "goal" && it.side == "HOME" },
            current.count { !it.voided && it.typeId == "goal" && it.side == "AWAY" })
    }
}
