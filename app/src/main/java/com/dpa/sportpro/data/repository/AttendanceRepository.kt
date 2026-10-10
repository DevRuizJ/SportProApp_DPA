package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.AttendancePlayer
import com.dpa.sportpro.data.model.AttendanceStatus
import com.dpa.sportpro.data.model.PlayerAttendanceRecord
import org.json.JSONArray
import org.json.JSONObject

class AttendanceRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val activePlayers: List<AttendancePlayer> = demoPlayers

    var records by mutableStateOf(loadRecords())
        private set

    fun recordsForSession(sessionId: String): List<PlayerAttendanceRecord> =
        records.filter { it.sessionId == sessionId }

    fun saveAttendance(
        sessionId: String,
        sessionDate: String,
        category: String,
        entries: List<AttendanceEntry>,
    ) {
        require(sessionId.isNotBlank()) { "Selecciona una sesión antes de guardar asistencia." }
        require(entries.isNotEmpty()) { "No hay jugadores activos para registrar." }
        val savedAt = System.currentTimeMillis()
        val newRecords =
            entries.map { entry ->
                PlayerAttendanceRecord(
                    sessionId = sessionId,
                    playerId = entry.player.id,
                    playerName = entry.player.name,
                    category = category,
                    sessionDate = sessionDate,
                    status = entry.status,
                    observation = entry.observation.trim(),
                    updatedAt = savedAt,
                )
            }
        records = records.filterNot { it.sessionId == sessionId } + newRecords
        persistRecords()
    }

    private fun loadRecords(): List<PlayerAttendanceRecord> {
        val saved = preferences.getString(RECORDS_KEY, null) ?: return emptyList()
        val array = JSONArray(saved)
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                add(
                    PlayerAttendanceRecord(
                        sessionId = value.getString("sessionId"),
                        playerId = value.getString("playerId"),
                        playerName = value.getString("playerName"),
                        category = value.getString("category"),
                        sessionDate = value.getString("sessionDate"),
                        status = AttendanceStatus.valueOf(value.getString("status")),
                        observation = value.getString("observation"),
                        updatedAt = value.getLong("updatedAt"),
                    )
                )
            }
        }
    }

    private fun persistRecords() {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("sessionId", record.sessionId)
                    .put("playerId", record.playerId)
                    .put("playerName", record.playerName)
                    .put("category", record.category)
                    .put("sessionDate", record.sessionDate)
                    .put("status", record.status.name)
                    .put("observation", record.observation)
                    .put("updatedAt", record.updatedAt)
            )
        }
        preferences.edit().putString(RECORDS_KEY, array.toString()).apply()
    }

    data class AttendanceEntry(
        val player: AttendancePlayer,
        val status: AttendanceStatus,
        val observation: String,
    )

    private companion object {
        const val PREFERENCES_NAME = "sportpro_attendance"
        const val RECORDS_KEY = "attendance_records"

        val demoPlayers =
            listOf(
                AttendancePlayer("player_mateo", "Mateo Silva Rossi", "Sub-15"),
                AttendancePlayer("player_lucas", "Lucas Gomez S.", "Sub-15"),
                AttendancePlayer("player_thiago", "Thiago Ruiz Flores", "Sub-15"),
                AttendancePlayer("player_gabriel", "Gabriel Mendez O.", "Sub-15"),
                AttendancePlayer("player_benjamin", "Benjamín Díaz", "Sub-15"),
                AttendancePlayer("player_santiago", "Santiago Paz", "Sub-15"),
            )
    }
}
