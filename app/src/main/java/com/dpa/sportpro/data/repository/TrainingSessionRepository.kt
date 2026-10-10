package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.TrainingExercise
import com.dpa.sportpro.data.model.TrainingSession
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class TrainingSessionRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var sessions by mutableStateOf(loadSessions())
        private set

    var exerciseLibrary by mutableStateOf(loadExerciseLibrary())
        private set

    fun saveSession(
        date: String,
        startTime: String,
        venue: String,
        category: String,
        objective: String,
        exercises: List<TrainingExercise>,
    ) {
        sessions =
            sessions +
                TrainingSession(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    startTime = startTime,
                    venue = venue.trim(),
                    category = category.trim(),
                    objective = objective.trim(),
                    exercises = exercises.toList(),
                )
        persistSessions()
    }

    fun saveExerciseToLibrary(exercise: TrainingExercise) {
        if (exerciseLibrary.any { it.name.equals(exercise.name, ignoreCase = true) }) return
        exerciseLibrary = exerciseLibrary + exercise
        persistExerciseLibrary()
    }

    private fun loadSessions(): List<TrainingSession> {
        val saved = preferences.getString(SESSIONS_KEY, null) ?: return emptyList()
        val array = JSONArray(saved)
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                add(
                    TrainingSession(
                        id = value.getString("id"),
                        date = value.getString("date"),
                        startTime = value.getString("startTime"),
                        venue = value.getString("venue"),
                        category = value.getString("category"),
                        objective = value.getString("objective"),
                        exercises = value.getJSONArray("exercises").toExerciseList(),
                    )
                )
            }
        }
    }

    private fun loadExerciseLibrary(): List<TrainingExercise> {
        val saved = preferences.getString(LIBRARY_KEY, null) ?: return demoLibrary
        return JSONArray(saved).toExerciseList()
    }

    private fun JSONArray.toExerciseList(): List<TrainingExercise> = buildList {
        for (index in 0 until length()) {
            val exercise = getJSONObject(index)
            add(
                TrainingExercise(
                    name = exercise.getString("name"),
                    durationMinutes = exercise.getInt("durationMinutes"),
                    intensity = exercise.getString("intensity"),
                    instructions = exercise.getString("instructions"),
                )
            )
        }
    }

    private fun persistSessions() {
        val array = JSONArray()
        sessions.forEach { session ->
            val exercises = JSONArray()
            session.exercises.forEach { exercises.put(it.toJson()) }
            array.put(
                JSONObject()
                    .put("id", session.id)
                    .put("date", session.date)
                    .put("startTime", session.startTime)
                    .put("venue", session.venue)
                    .put("category", session.category)
                    .put("objective", session.objective)
                    .put("exercises", exercises)
            )
        }
        preferences.edit().putString(SESSIONS_KEY, array.toString()).apply()
    }

    private fun persistExerciseLibrary() {
        val array = JSONArray()
        exerciseLibrary.forEach { array.put(it.toJson()) }
        preferences.edit().putString(LIBRARY_KEY, array.toString()).apply()
    }

    private fun TrainingExercise.toJson() =
        JSONObject()
            .put("name", name)
            .put("durationMinutes", durationMinutes)
            .put("intensity", intensity)
            .put("instructions", instructions)

    private companion object {
        const val PREFERENCES_NAME = "sportpro_training"
        const val SESSIONS_KEY = "sessions"
        const val LIBRARY_KEY = "exercise_library"

        val demoLibrary =
            listOf(
                TrainingExercise(
                    name = "Rondo: Conservación 5v2",
                    durationMinutes = 15,
                    intensity = "Media",
                    instructions =
                        "Rondo básico para incentivar el pase a un toque y la presión coordinada de dos recuperadores.",
                ),
                TrainingExercise(
                    name = "Transiciones Rápidas y Tiro",
                    durationMinutes = 25,
                    intensity = "Alta",
                    instructions =
                        "Ataque rápido por bandas finalizando con centro al área. Retorno defensivo en bloque bajo.",
                ),
            )
    }
}
