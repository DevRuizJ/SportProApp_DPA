package com.dpa.sportpro.data.repository

import android.content.Context
import com.dpa.sportpro.data.model.ChronicleStatus
import com.dpa.sportpro.data.model.MatchChronicle
import com.dpa.sportpro.data.model.MatchRecord
import com.dpa.sportpro.model.UserRole
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class MatchChronicleRepository private constructor(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _chronicles = MutableStateFlow(loadChronicles())

    val chronicles: StateFlow<List<MatchChronicle>> = _chronicles.asStateFlow()

    fun generatePending(match: MatchRecord): MatchChronicle {
        require(MatchChronicleEligibility.canGenerate(match)) {
            "El partido debe estar finalizado y tener inicio y fin registrados."
        }
        val narrative = MatchChronicleNarrativeGenerator.generate(match)
        val existing = _chronicles.value.firstOrNull { it.matchId == match.id }
        require(existing?.status != ChronicleStatus.APPROVED) {
            "La crónica aprobada ya está publicada y no se puede regenerar."
        }
        val chronicle =
            MatchChronicle(
                id = existing?.id ?: UUID.randomUUID().toString(),
                matchId = match.id,
                homeTeamName = match.homeTeamName,
                awayTeamName = match.awayTeamName,
                homeScore =
                    match.events.count {
                        it.eventType == com.dpa.sportpro.data.model.EventType.GOAL &&
                            it.teamName == match.homeTeamName &&
                            it.status != com.dpa.sportpro.data.model.MatchEventStatus.ANNULLED
                    },
                awayScore =
                    match.events.count {
                        it.eventType == com.dpa.sportpro.data.model.EventType.GOAL &&
                            it.teamName == match.awayTeamName &&
                            it.status != com.dpa.sportpro.data.model.MatchEventStatus.ANNULLED
                    },
                matchDateMillis = match.finishedAtMillis ?: match.startedAtMillis,
                narrative = narrative.trim(),
                status = ChronicleStatus.PENDING_APPROVAL,
                createdAtMillis = System.currentTimeMillis(),
            )
        val updated = listOf(chronicle) + _chronicles.value.filterNot { it.matchId == match.id }
        persist(updated)
        return chronicle
    }

    fun forMatch(matchId: String): MatchChronicle? =
        _chronicles.value.firstOrNull { it.matchId == matchId }

    fun saveDraft(
        matchId: String,
        narrative: String,
        technicalComment: String,
        reviewerRole: UserRole,
    ): MatchChronicle {
        require(reviewerRole == UserRole.COACH) {
            "Solo el Director Técnico puede editar la crónica."
        }
        require(narrative.isNotBlank()) { "La narración no puede estar vacía." }
        val existing = forMatch(matchId) ?: error("No existe un borrador para este partido.")
        require(existing.status == ChronicleStatus.PENDING_APPROVAL) {
            "La crónica ya fue aprobada y publicada."
        }
        val updated =
            existing.copy(
                narrative = narrative.trim(),
                technicalComment = technicalComment.trim(),
            )
        persist(_chronicles.value.map { if (it.id == updated.id) updated else it })
        return updated
    }

    fun rejectAndRegenerate(
        match: MatchRecord,
        reviewerRole: UserRole,
    ): MatchChronicle {
        require(reviewerRole == UserRole.COACH) {
            "Solo el Director Técnico puede regenerar la crónica."
        }
        require(MatchChronicleEligibility.canGenerate(match)) {
            "El partido debe estar finalizado y tener inicio y fin registrados."
        }
        val existing = forMatch(match.id) ?: error("No existe un borrador para este partido.")
        require(existing.status == ChronicleStatus.PENDING_APPROVAL) {
            "La crónica ya fue aprobada y publicada."
        }
        val updated =
            existing.copy(
                narrative = MatchChronicleNarrativeGenerator.generate(match).trim(),
                technicalComment = "",
                createdAtMillis = System.currentTimeMillis(),
            )
        persist(_chronicles.value.map { if (it.id == updated.id) updated else it })
        return updated
    }

    fun approveAndPublish(
        matchId: String,
        narrative: String,
        technicalComment: String,
        reviewerRole: UserRole,
    ): MatchChronicle {
        require(reviewerRole == UserRole.COACH) {
            "Solo el Director Técnico puede aprobar la crónica."
        }
        require(narrative.isNotBlank()) { "La narración no puede estar vacía." }
        val existing = forMatch(matchId) ?: error("No existe un borrador para este partido.")
        require(existing.status == ChronicleStatus.PENDING_APPROVAL) {
            "La crónica ya fue aprobada y publicada."
        }
        val updated =
            existing.copy(
                narrative = narrative.trim(),
                technicalComment = technicalComment.trim(),
                status = ChronicleStatus.APPROVED,
                publishedAtMillis = System.currentTimeMillis(),
            )
        persist(_chronicles.value.map { if (it.id == updated.id) updated else it })
        return updated
    }

    fun publishedChronicles(): List<MatchChronicle> =
        _chronicles.value.filter { it.status == ChronicleStatus.APPROVED }

    private fun persist(chronicles: List<MatchChronicle>) {
        val json = JSONArray()
        chronicles.forEach { json.put(it.toJson()) }
        preferences.edit().putString(CHRONICLES_KEY, json.toString()).apply()
        _chronicles.value = chronicles
    }

    private fun loadChronicles(): List<MatchChronicle> {
        val saved = preferences.getString(CHRONICLES_KEY, null) ?: return emptyList()
        val json = JSONArray(saved)
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            MatchChronicle(
                id = item.getString("id"),
                matchId = item.getString("matchId"),
                homeTeamName = item.getString("homeTeamName"),
                awayTeamName = item.getString("awayTeamName"),
                homeScore = item.getInt("homeScore"),
                awayScore = item.getInt("awayScore"),
                matchDateMillis = item.getLong("matchDateMillis"),
                narrative = item.getString("narrative"),
                status = ChronicleStatus.valueOf(item.getString("status")),
                createdAtMillis = item.getLong("createdAtMillis"),
                technicalComment = item.optString("technicalComment"),
                publishedAtMillis =
                    if (item.has("publishedAtMillis")) item.getLong("publishedAtMillis") else null,
            )
        }
    }

    private fun MatchChronicle.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("matchId", matchId)
            .put("homeTeamName", homeTeamName)
            .put("awayTeamName", awayTeamName)
            .put("homeScore", homeScore)
            .put("awayScore", awayScore)
            .put("matchDateMillis", matchDateMillis)
            .put("narrative", narrative)
            .put("status", status.name)
            .put("createdAtMillis", createdAtMillis)
            .put("technicalComment", technicalComment)
            .apply { publishedAtMillis?.let { put("publishedAtMillis", it) } }

    companion object {
        private const val PREFERENCES_NAME = "sportpro_match_chronicles"
        private const val CHRONICLES_KEY = "match_chronicles"

        @Volatile private var instance: MatchChronicleRepository? = null

        fun getInstance(context: Context): MatchChronicleRepository =
            instance
                ?: synchronized(this) {
                    instance ?: MatchChronicleRepository(context).also { instance = it }
                }
    }
}
