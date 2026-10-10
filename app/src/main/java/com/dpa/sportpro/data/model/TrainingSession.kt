package com.dpa.sportpro.data.model

data class TrainingExercise(
    val name: String,
    val durationMinutes: Int,
    val intensity: String,
    val instructions: String,
)

data class TrainingSession(
    val id: String,
    val date: String,
    val startTime: String,
    val venue: String,
    val category: String,
    val objective: String,
    val exercises: List<TrainingExercise>,
) {
    val totalDurationMinutes: Int
        get() = exercises.sumOf { it.durationMinutes }
}
