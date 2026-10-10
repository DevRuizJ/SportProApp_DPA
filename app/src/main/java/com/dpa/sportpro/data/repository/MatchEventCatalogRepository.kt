package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.MatchEventType
import org.json.JSONArray
import org.json.JSONObject

class MatchEventCatalogRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var eventTypes by mutableStateOf(loadEventTypes())
        private set

    private var categorySettings by mutableStateOf(loadCategorySettings())

    fun isEnabled(category: String, eventId: String): Boolean =
        eventId in (categorySettings[category] ?: eventTypes.map { it.id }.toSet())

    fun setEnabled(category: String, eventId: String, enabled: Boolean) {
        require(eventTypes.any { it.id == eventId }) { "Tipo de evento no encontrado." }
        require(category.isNotBlank()) { "Selecciona una categoría." }
        val enabledIds =
            (categorySettings[category] ?: eventTypes.map { it.id }.toSet()).toMutableSet()
        if (enabled) enabledIds += eventId else enabledIds -= eventId
        categorySettings = categorySettings + (category to enabledIds)
        persistCategorySettings()
    }

    fun addCustomEvent(
        name: String,
        description: String,
        requiredFields: List<String>,
        optionalFields: List<String>,
    ) {
        require(name.isNotBlank()) { "Ingresa el nombre del evento." }
        require(eventTypes.none { it.name.equals(name.trim(), ignoreCase = true) }) {
            "Ya existe un evento con ese nombre."
        }
        val eventType =
            MatchEventType(
                id = "custom_${System.currentTimeMillis()}",
                name = name.trim(),
                description = description.trim(),
                requiredFields =
                    requiredFields.map(String::trim).filter(String::isNotBlank).distinct(),
                optionalFields =
                    optionalFields.map(String::trim).filter(String::isNotBlank).distinct(),
            )
        eventTypes = eventTypes + eventType
        persistEventTypes()
        defaultCategories.forEach { category ->
            val enabledIds =
                (categorySettings[category] ?: eventTypes.map { it.id }.toSet()) + eventType.id
            categorySettings = categorySettings + (category to enabledIds)
        }
        persistCategorySettings()
    }

    private fun loadEventTypes(): List<MatchEventType> {
        val saved = preferences.getString(EVENT_TYPES_KEY, null) ?: return defaultEventTypes
        val array = JSONArray(saved)
        return buildList {
            for (index in 0 until array.length()) {
                val event = array.getJSONObject(index)
                add(
                    MatchEventType(
                        id = event.getString("id"),
                        name = event.getString("name"),
                        description = event.getString("description"),
                        requiredFields = event.getJSONArray("requiredFields").toStringList(),
                        optionalFields = event.getJSONArray("optionalFields").toStringList(),
                    )
                )
            }
        }
    }

    private fun loadCategorySettings(): Map<String, Set<String>> {
        val saved = preferences.getString(CATEGORY_SETTINGS_KEY, null) ?: return emptyMap()
        val json = JSONObject(saved)
        return buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val category = keys.next()
                put(category, json.getJSONArray(category).toStringList().toSet())
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) add(getString(index))
    }

    private fun persistEventTypes() {
        val array = JSONArray()
        eventTypes.forEach { event ->
            array.put(
                JSONObject()
                    .put("id", event.id)
                    .put("name", event.name)
                    .put("description", event.description)
                    .put("requiredFields", JSONArray(event.requiredFields))
                    .put("optionalFields", JSONArray(event.optionalFields))
            )
        }
        preferences.edit().putString(EVENT_TYPES_KEY, array.toString()).apply()
    }

    private fun persistCategorySettings() {
        val json = JSONObject()
        categorySettings.forEach { (category, enabledIds) ->
            json.put(category, JSONArray(enabledIds.toList()))
        }
        preferences.edit().putString(CATEGORY_SETTINGS_KEY, json.toString()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "sportpro_match_event_catalog"
        const val EVENT_TYPES_KEY = "event_types"
        const val CATEGORY_SETTINGS_KEY = "category_settings"

        val defaultCategories = listOf("Sub-10", "Sub-12", "Sub-15", "Sub-17", "Primera")

        val defaultEventTypes =
            listOf(
                MatchEventType(
                    "period_start",
                    "Inicio de tiempo",
                    "Inicio del primer o segundo tiempo",
                    listOf("Tiempo"),
                    emptyList(),
                ),
                MatchEventType(
                    "period_end",
                    "Fin de tiempo",
                    "Finalización de un tiempo de juego",
                    listOf("Tiempo"),
                    emptyList(),
                ),
                MatchEventType(
                    "goal",
                    "Gol",
                    "Registro de anotaciones válidas",
                    listOf("Autor", "Tipo de jugada"),
                    listOf("Asistencia"),
                ),
                MatchEventType(
                    "goal_kick",
                    "Saque de meta",
                    "Reanudación desde el área de meta",
                    listOf("Equipo ejecutor"),
                    emptyList(),
                ),
                MatchEventType(
                    "throw_in",
                    "Saque lateral",
                    "Reanudación desde la línea lateral",
                    listOf("Equipo ejecutor"),
                    emptyList(),
                ),
                MatchEventType(
                    "corner_kick",
                    "Tiro de esquina",
                    "Saque de esquina cobrado",
                    listOf("Equipo ejecutor"),
                    emptyList(),
                ),
                MatchEventType(
                    "foul",
                    "Falta",
                    "Infracción cometida en campo",
                    listOf("Jugador infractor", "Equipo"),
                    listOf("Jugador afectado"),
                ),
                MatchEventType(
                    "yellow_card",
                    "Tarjeta amarilla",
                    "Amonestación disciplinaria",
                    listOf("Jugador", "Motivo"),
                    emptyList(),
                ),
                MatchEventType(
                    "red_card",
                    "Tarjeta roja",
                    "Expulsión del terreno de juego",
                    listOf("Jugador", "Motivo"),
                    emptyList(),
                ),
                MatchEventType(
                    "penalty",
                    "Penal",
                    "Cobro desde los doce pasos",
                    listOf("Equipo ejecutor", "Jugador ejecutor"),
                    emptyList(),
                ),
                MatchEventType(
                    "offside",
                    "Fuera de juego",
                    "Posición adelantada sancionada",
                    listOf("Jugador"),
                    emptyList(),
                ),
                MatchEventType(
                    "substitution",
                    "Sustitución",
                    "Cambio de jugadores tácticos",
                    listOf("Jugador que sale", "Jugador que entra"),
                    emptyList(),
                ),
            )
    }
}
