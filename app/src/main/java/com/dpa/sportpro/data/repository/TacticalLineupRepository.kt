package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.ConvocationResponse
import com.dpa.sportpro.data.model.LineupStatus
import com.dpa.sportpro.data.model.MatchConvocation
import com.dpa.sportpro.data.model.TacticalLineup
import org.json.JSONArray
import org.json.JSONObject

class TacticalLineupRepository private constructor(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var lineups by mutableStateOf(loadLineups())
        private set

    fun lineupFor(convocationId: String): TacticalLineup? =
        lineups.firstOrNull { it.convocationId == convocationId }

    fun save(
        convocation: MatchConvocation,
        formation: String,
        startersBySlot: Map<String, String>,
        captainPlayerId: String,
        status: LineupStatus,
    ) {
        val confirmedPlayers =
            convocation.players
                .filter { it.response == ConvocationResponse.CONFIRMED }
                .map { it.id }
                .toSet()
        require(startersBySlot.values.all { it in confirmedPlayers }) {
            "Solo se pueden alinear jugadores convocados que confirmaron asistencia."
        }
        require(startersBySlot.values.distinct().size == startersBySlot.size) {
            "Un jugador no puede ocupar más de una posición."
        }
        if (captainPlayerId.isNotBlank()) {
            require(captainPlayerId in startersBySlot.values) {
                "El capitán debe formar parte del once titular."
            }
        }
        if (status == LineupStatus.PUBLISHED) {
            require(startersBySlot.size == STARTER_COUNT) {
                "Asigna los 11 titulares antes de publicar la alineación."
            }
            require(captainPlayerId.isNotBlank()) {
                "Selecciona al capitán antes de publicar la alineación."
            }
        }
        val lineup =
            TacticalLineup(
                convocationId = convocation.id,
                formation = formation,
                startersBySlot = startersBySlot.toMap(),
                captainPlayerId = captainPlayerId,
                status = status,
                updatedAt = System.currentTimeMillis(),
            )
        lineups = lineups.filterNot { it.convocationId == convocation.id } + lineup
        persist()
    }

    private fun loadLineups(): List<TacticalLineup> {
        val saved = preferences.getString(LINEUPS_KEY, null) ?: return emptyList()
        val array = JSONArray(saved)
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                val assignmentsObject = value.getJSONObject("startersBySlot")
                val assignments = buildMap {
                    val keys = assignmentsObject.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        put(key, assignmentsObject.getString(key))
                    }
                }
                add(
                    TacticalLineup(
                        convocationId = value.getString("convocationId"),
                        formation = value.getString("formation"),
                        startersBySlot = assignments,
                        captainPlayerId = value.getString("captainPlayerId"),
                        status = LineupStatus.valueOf(value.getString("status")),
                        updatedAt = value.getLong("updatedAt"),
                    )
                )
            }
        }
    }

    private fun persist() {
        val array = JSONArray()
        lineups.forEach { lineup ->
            val assignments = JSONObject()
            lineup.startersBySlot.forEach(assignments::put)
            array.put(
                JSONObject()
                    .put("convocationId", lineup.convocationId)
                    .put("formation", lineup.formation)
                    .put("startersBySlot", assignments)
                    .put("captainPlayerId", lineup.captainPlayerId)
                    .put("status", lineup.status.name)
                    .put("updatedAt", lineup.updatedAt)
            )
        }
        preferences.edit().putString(LINEUPS_KEY, array.toString()).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "sportpro_tactical_lineups"
        private const val LINEUPS_KEY = "lineups"
        private const val STARTER_COUNT = 11

        @Volatile private var instance: TacticalLineupRepository? = null

        fun getInstance(context: Context): TacticalLineupRepository =
            instance
                ?: synchronized(this) {
                    instance
                        ?: TacticalLineupRepository(context.applicationContext).also {
                            instance = it
                        }
                }
    }
}
