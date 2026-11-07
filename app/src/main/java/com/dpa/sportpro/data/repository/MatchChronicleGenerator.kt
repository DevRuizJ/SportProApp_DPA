package com.dpa.sportpro.data.repository

import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchRecord
import com.dpa.sportpro.data.model.MatchStatus
import java.util.Locale

data class ChronicleEventInput(
    val minute: Int,
    val eventName: String,
    val teamName: String,
    val author: String,
    val assistant: String?,
    val notes: String,
)

data class MatchChroniclePrompt(val chronologicalEvents: List<ChronicleEventInput>) {
    fun toStructuredText(): String = buildString {
        append("[")
        chronologicalEvents.forEachIndexed { index, event ->
            val comma = if (index < chronologicalEvents.lastIndex) "," else ""
            append(
                """{"minuto":${event.minute},"evento":"${event.eventName.jsonEscape()}","equipo":"${event.teamName.jsonEscape()}","autor":"${event.author.jsonEscape()}","asistencia":${event.assistant?.let { "\"${it.jsonEscape()}\"" } ?: "null"},"detalle":"${event.notes.jsonEscape()}"}$comma"""
            )
        }
        append("]")
    }

    companion object {
        fun fromMatch(match: MatchRecord): MatchChroniclePrompt {
            require(match.status == MatchStatus.FINISHED) {
                "La crónica solo puede generarse para partidos finalizados."
            }
            val activeEvents = match.events.filter { it.status != MatchEventStatus.ANNULLED }
            require(activeEvents.any { it.eventType == EventType.MATCH_START }) {
                "Registra el inicio del partido antes de generar la crónica."
            }
            require(activeEvents.any { it.eventType == EventType.MATCH_END }) {
                "Registra el fin del partido antes de generar la crónica."
            }

            return MatchChroniclePrompt(
                chronologicalEvents =
                    activeEvents
                        .sortedWith(
                            compareBy<MatchEvent> { it.minute }
                                .thenBy {
                                    when (it.eventType) {
                                        EventType.MATCH_START -> 0
                                        EventType.MATCH_END -> 2
                                        else -> 1
                                    }
                                }
                                .thenBy { it.id }
                        )
                        .map { it.toChronicleInput() }
            )
        }
    }
}

object MatchChronicleNarrativeGenerator {

    fun generate(match: MatchRecord): String {
        val events = MatchChroniclePrompt.fromMatch(match).chronologicalEvents
        return buildString {
            events.forEach { event ->
                when (event.eventName) {
                    "Inicio de tiempo" ->
                        append(
                            "El registro indica el inicio del tiempo en el minuto ${event.minute}. "
                        )
                    "Fin de tiempo" ->
                        append(
                            "El tiempo concluyó según el registro en el minuto ${event.minute}. "
                        )
                    EventType.GOAL.displayName -> {
                        append(
                            "En el minuto ${event.minute}, ${event.author} anotó para ${event.teamName}. "
                        )
                        event.assistant?.takeIf(String::isNotBlank)?.let {
                            append("La asistencia fue de $it. ")
                        }
                    }
                    EventType.SUBSTITUTION.displayName -> {
                        append("En el minuto ${event.minute} se registró una sustitución para ")
                        append("${event.teamName}: ${event.author}")
                        event.assistant?.takeIf(String::isNotBlank)?.let { append(" por $it") }
                        append(". ")
                    }
                    else ->
                        append(
                            "En el minuto ${event.minute} se registró ${event.eventName.lowercase(Locale.ROOT)} " +
                                "para ${event.teamName}, asociado a ${event.author}. "
                        )
                }
                event.notes.takeIf(String::isNotBlank)?.let { append("$it ") }
            }
        }
    }
}

object MatchChronicleEligibility {
    fun canGenerate(match: MatchRecord?): Boolean =
        match != null &&
            match.status == MatchStatus.FINISHED &&
            match.events.any {
                it.eventType == EventType.MATCH_START && it.status != MatchEventStatus.ANNULLED
            } &&
            match.events.any {
                it.eventType == EventType.MATCH_END && it.status != MatchEventStatus.ANNULLED
            }
}

private fun MatchEvent.toChronicleInput(): ChronicleEventInput =
    ChronicleEventInput(
        minute = minute,
        eventName = eventType.displayName,
        teamName = teamName,
        author = playerName,
        assistant = assistantPlayerName,
        notes = notes,
    )

private fun String.jsonEscape(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
