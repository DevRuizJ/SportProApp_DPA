package com.dpa.sportpro.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.dpa.sportpro.data.model.AttendanceStatus
import com.dpa.sportpro.data.model.TrainingExercise
import com.dpa.sportpro.data.model.TrainingSession
import com.dpa.sportpro.data.repository.AttendanceRepository
import com.dpa.sportpro.data.repository.TrainingSessionRepository
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val attendanceStatuses = listOf(
    AttendanceStatus.PRESENT,
    AttendanceStatus.LATE,
    AttendanceStatus.EXCUSED_ABSENCE,
    AttendanceStatus.UNEXCUSED_ABSENCE
)

private data class AttendanceDraft(
    val playerId: String,
    val status: AttendanceStatus,
    val observation: String
)

@Composable
fun AttendanceScreen(
    viewerRole: UserRole,
    viewerName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val attendanceRepository = remember(context) {
        AttendanceRepository(context.applicationContext)
    }
    val sessionRepository = remember(context) {
        TrainingSessionRepository(context.applicationContext)
    }
    val sessions = sessionRepository.sessions.ifEmpty { listOf(demoSession()) }
    var selectedSessionId by remember { mutableStateOf(sessions.first().id) }
    var showSessionPicker by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val selectedSession = sessions.firstOrNull { it.id == selectedSessionId } ?: sessions.first()
    val categoryName = selectedSession.category.substringBefore(" (").trim()
    val players = attendanceRepository.activePlayers.filter {
        it.isActive && it.category.equals(categoryName, ignoreCase = true)
    }
    val savedForSession = attendanceRepository.recordsForSession(selectedSession.id)
    val drafts = remember(selectedSession.id, savedForSession, players) {
        mutableStateListOf<AttendanceDraft>().apply {
            players.forEach { player ->
                val saved = savedForSession.firstOrNull { it.playerId == player.id }
                add(
                    AttendanceDraft(
                        playerId = player.id,
                        status = saved?.status ?: AttendanceStatus.PENDING,
                        observation = saved?.observation.orEmpty()
                    )
                )
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite
                    )
                }
                Text(
                    "Control de Asistencia",
                    modifier = Modifier.padding(start = 6.dp),
                    color = TextWhite,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSessionPicker = true }
                        .padding(start = 4.dp, bottom = 16.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sesión: ${formatDate(selectedSession.date)}", color = TextMuted, fontSize = 14.sp)
                    Text("  •  ", color = TextMuted)
                    Text(
                        "$categoryName (${players.size} Jugadores)",
                        color = NeonGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar sesión", tint = TextMuted)
                }
                DropdownMenu(
                    expanded = showSessionPicker,
                    onDismissRequest = { showSessionPicker = false },
                    modifier = Modifier.heightIn(max = 380.dp)
                ) {
                    sessions.forEach { session ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("${formatDate(session.date)}  •  ${session.startTime}")
                                    Text(
                                        "${session.category}  •  ${session.objective}",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            onClick = {
                                selectedSessionId = session.id
                                savedMessage = null
                                showSessionPicker = false
                            }
                        )
                    }
                }
            }

            if (viewerRole != UserRole.COACH) {
                Text("El control de asistencia está disponible solo para el Director Técnico.", color = TextMuted)
            } else if (players.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "No hay jugadores activos registrados en la categoría $categoryName.",
                        color = TextMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    players.forEachIndexed { index, player ->
                        val draftIndex = drafts.indexOfFirst { it.playerId == player.id }
                        val draft = drafts.getOrNull(draftIndex) ?: return@forEachIndexed
                        AttendancePlayerCard(
                            playerName = player.name,
                            status = draft.status,
                            observation = draft.observation,
                            onStatusChange = { newStatus ->
                                drafts[draftIndex] = draft.copy(status = newStatus)
                                savedMessage = null
                            },
                            onObservationChange = { newObservation ->
                                drafts[draftIndex] = draft.copy(observation = newObservation)
                                savedMessage = null
                            }
                        )
                    }
                }

                savedMessage?.let {
                    Text(it, color = NeonGreen, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    onClick = {
                        attendanceRepository.saveAttendance(
                            sessionId = selectedSession.id,
                            sessionDate = selectedSession.date,
                            category = categoryName,
                            entries = players.map { player ->
                                val draft = drafts.first { it.playerId == player.id }
                                AttendanceRepository.AttendanceEntry(
                                    player = player,
                                    status = draft.status,
                                    observation = draft.observation
                                )
                            }
                        )
                        savedMessage = "Asistencia guardada para ${players.size} jugadores."
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp).height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground)
                    Text("  Guardar Asistencia", color = DarkBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AttendancePlayerCard(
    playerName: String,
    status: AttendanceStatus,
    observation: String,
    onStatusChange: (AttendanceStatus) -> Unit,
    onObservationChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(DarkBackground, CircleShape)
                        .border(1.dp, InputBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NeonGreen)
                }
                Text(
                    playerName,
                    modifier = Modifier.weight(1f).padding(start = 12.dp, end = 6.dp),
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                AttendanceStatusPicker(status = status, onStatusChange = onStatusChange)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .background(Color(0xFF1A2D4B), RoundedCornerShape(10.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.padding(start = 12.dp).size(18.dp)
                )
                OutlinedTextField(
                    value = observation,
                    onValueChange = onObservationChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Agregar observación...", color = TextMuted, fontSize = 14.sp) },
                    singleLine = true,
                    colors = attendanceFieldColors()
                )
            }
        }
    }
}

@Composable
private fun AttendanceStatusPicker(
    status: AttendanceStatus,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val color = when (status) {
        AttendanceStatus.PRESENT -> NeonGreen
        AttendanceStatus.LATE -> Color(0xFFFFB300)
        AttendanceStatus.EXCUSED_ABSENCE -> Color(0xFF00B8FF)
        AttendanceStatus.UNEXCUSED_ABSENCE -> ErrorRed
        AttendanceStatus.PENDING -> TextMuted
    }
    Box {
        Row(
            modifier = Modifier
                .background(DarkBackground, RoundedCornerShape(10.dp))
                .border(1.dp, InputBorder, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(9.dp).background(color, CircleShape))
            Text(
                status.label,
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Cambiar estado",
                tint = TextMuted,
                modifier = Modifier.size(17.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            attendanceStatuses.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onStatusChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun attendanceFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    cursorColor = NeonGreen
)

private fun demoSession(): TrainingSession {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    return TrainingSession(
        id = "demo-session-sub15",
        date = date,
        startTime = "16:00",
        venue = "Cancha Principal Sintética",
        category = "Sub-15 (Carlo Ancelotti)",
        objective = "Transición defensiva y presión alta",
        exercises = listOf(
            TrainingExercise(
                name = "Rondo",
                durationMinutes = 15,
                intensity = "Media",
                instructions = "Ejercicio técnico de posesión."
            )
        )
    )
}

private fun formatDate(value: String): String {
    val date = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
    } catch (_: java.text.ParseException) {
        null
    } ?: return value
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(date)
}
