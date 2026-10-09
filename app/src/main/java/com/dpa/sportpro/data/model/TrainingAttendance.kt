package com.dpa.sportpro.data.model

enum class AttendanceStatus(val label: String) {
    PENDING("Pendiente"),
    PRESENT("Presente"),
    LATE("Tardanza"),
    EXCUSED_ABSENCE("Falta Justificada"),
    UNEXCUSED_ABSENCE("Falta Injustificada")
}

data class AttendancePlayer(
    val id: String,
    val name: String,
    val category: String,
    val isActive: Boolean = true
)

data class PlayerAttendanceRecord(
    val sessionId: String,
    val playerId: String,
    val playerName: String,
    val category: String,
    val sessionDate: String,
    val status: AttendanceStatus,
    val observation: String,
    val updatedAt: Long
)
