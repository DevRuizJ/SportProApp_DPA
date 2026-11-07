package com.dpa.sportpro.ui.matchevents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.ui.components.SportProInputField
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite

@Composable
fun MatchEventsScreen(
    viewModel: MatchEventsViewModel = viewModel(),
    operatorUserId: String = "dt_operator_1",
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(ErrorRed, CircleShape))
                            Text(
                                text = " EN VIVO",
                                color = ErrorRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.toggleTimer() },
                        ) {
                            val minutes = uiState.timerSeconds / 60
                            val seconds = uiState.timerSeconds % 60
                            val timerFormatted =
                                java.lang.String.format(
                                    java.util.Locale.US,
                                    "%02d:%02d",
                                    minutes,
                                    seconds,
                                )

                            Text(
                                text = timerFormatted,
                                color = NeonGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector =
                                    if (uiState.isTimerRunning) Icons.Default.Pause
                                    else Icons.Default.PlayArrow,
                                contentDescription = "Timer toggle",
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.homeTeamName,
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.homeScore}",
                                color = TextWhite,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }

                        Text(
                            text = "VS",
                            color = TextMuted,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.awayTeamName,
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.awayScore}",
                                color = TextWhite,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.matchStatus == MatchStatus.IN_PROGRESS) {
                Button(
                    onClick = viewModel::finishMatch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                ) {
                    Text(
                        "Finalizar partido y actualizar estadísticas",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("PARTIDO FINALIZADO", color = NeonGreen, fontWeight = FontWeight.Bold)
                    TextButton(onClick = viewModel::openNewMatchDialog) {
                        Text("Nuevo partido", color = NeonGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.matchStatus == MatchStatus.IN_PROGRESS) {
                Text(
                    text = "REGISTRAR EVENTO RÁPIDO",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickEventButton(
                        title = "Gol",
                        icon = Icons.Default.Cancel,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.GOAL) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickEventButton(
                        title = "Falta",
                        icon = Icons.Default.Cancel,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.FOUL) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickEventButton(
                        title = "T. Amarilla",
                        icon = Icons.Default.Warning,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.YELLOW_CARD) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickEventButton(
                        title = "T. Roja",
                        icon = Icons.Default.Warning,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.RED_CARD) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickEventButton(
                        title = "Esquina",
                        icon = Icons.Default.Flag,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.CORNER) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickEventButton(
                        title = "Sustitución",
                        icon = Icons.Default.SwapHoriz,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.SUBSTITUTION) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickEventButton(
                        title = "Tiro",
                        icon = Icons.Default.Flag,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.SHOT) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickEventButton(
                        title = "Saque de meta",
                        icon = Icons.Default.Flag,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.GOAL_KICK) },
                        modifier = Modifier.weight(1f),
                    )
                    QuickEventButton(
                        title = "Saque lateral",
                        icon = Icons.Default.Flag,
                        onClick = { viewModel.openQuickAddEventDialog(EventType.THROW_IN) },
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Text(
                    "La bitácora está cerrada. Puedes iniciar otro partido para continuar registrando eventos.",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "LÍNEA DE TIEMPO DEL PARTIDO",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(14.dp))

            uiState.events.forEach { event ->
                MatchEventCard(
                    event = event,
                    onEditClick = { viewModel.openCorrectionDialog(event, isAnnul = false) },
                    onAnnulClick = { viewModel.openCorrectionDialog(event, isAnnul = true) },
                )
            }
        }
    }

    if (uiState.showAddEventDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAddEventDialog() },
            containerColor = CardBackground,
            title = {
                Text(
                    text = "Registrar ${uiState.selectedEventType.displayName}",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                Column {
                    SportProInputField(
                        label = "Minuto Exacto",
                        value = uiState.eventMinuteText,
                        onValueChange = viewModel::onMinuteTextChanged,
                        placeholder = "Ej. 65",
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Equipo Involucrado",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.onTeamSelected(uiState.homeTeamName) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (uiState.selectedTeamName == uiState.homeTeamName)
                                            NeonGreen
                                        else DarkBackground
                                ),
                        ) {
                            Text(
                                text = uiState.homeTeamName,
                                color =
                                    if (uiState.selectedTeamName == uiState.homeTeamName)
                                        DarkBackground
                                    else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Button(
                            onClick = { viewModel.onTeamSelected(uiState.awayTeamName) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (uiState.selectedTeamName == uiState.awayTeamName)
                                            NeonGreen
                                        else DarkBackground
                                ),
                        ) {
                            Text(
                                text = uiState.awayTeamName,
                                color =
                                    if (uiState.selectedTeamName == uiState.awayTeamName)
                                        DarkBackground
                                    else TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SportProInputField(
                        label =
                            if (uiState.selectedEventType == EventType.SUBSTITUTION) {
                                "Jugador que sale"
                            } else {
                                "Jugador Principal"
                            },
                        value = uiState.playerName,
                        onValueChange = viewModel::onPlayerNameChanged,
                        placeholder = "Nombre del jugador",
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Checkbox(
                            checked = uiState.isUnidentifiedPlayer,
                            onCheckedChange = viewModel::onToggleUnidentifiedPlayer,
                            colors = CheckboxDefaults.colors(checkedColor = NeonGreen),
                        )
                        Text(text = "Jugador no identificado", color = TextMuted, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SportProInputField(
                        label =
                            if (uiState.selectedEventType == EventType.SUBSTITUTION) {
                                "Jugador que entra"
                            } else {
                                "Asistencia / Segundo Jugador (Opcional)"
                            },
                        value = uiState.assistantPlayerName,
                        onValueChange = viewModel::onAssistantPlayerNameChanged,
                        placeholder = "Ej. Thiago Rossi",
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SportProInputField(
                        label = "Observaciones",
                        value = uiState.eventNotes,
                        onValueChange = viewModel::onEventNotesChanged,
                        placeholder = "Notas adicionales...",
                    )
                    uiState.eventError?.let { error ->
                        Text(
                            text = error,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveEvent() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                ) {
                    Text("Registrar Evento", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddEventDialog() }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
        )
    }

    if (uiState.showCorrectionDialog) {
        val actionTitle = if (uiState.isAnnulAction) "Anular Evento" else "Corregir Evento"
        AlertDialog(
            onDismissRequest = { viewModel.closeCorrectionDialog() },
            containerColor = CardBackground,
            title = {
                Text(actionTitle, color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column {
                    Text(
                        text =
                            "Esta acción mantendrá la trazabilidad indicando tu usuario ($operatorUserId) y la fecha de modificación.",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SportProInputField(
                        label = "Motivo de la Modificación",
                        value = uiState.correctionReason,
                        onValueChange = viewModel::onCorrectionReasonChanged,
                        placeholder = "Ej. Corrección por cobro arbitral VAR",
                    )
                }

                if (uiState.showNewMatchDialog) {
                    AlertDialog(
                        onDismissRequest = viewModel::closeNewMatchDialog,
                        containerColor = CardBackground,
                        title = {
                            Text("Nuevo partido", color = NeonGreen, fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Column {
                                SportProInputField(
                                    label = "Equipo del club (local)",
                                    value = uiState.newHomeTeamName,
                                    onValueChange = viewModel::onNewHomeTeamNameChanged,
                                    placeholder = "Ej. SportPro FC",
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                SportProInputField(
                                    label = "Rival",
                                    value = uiState.newAwayTeamName,
                                    onValueChange = viewModel::onNewAwayTeamNameChanged,
                                    placeholder = "Ej. Inter Lima",
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                SportProInputField(
                                    label = "Jugadores titulares (separados por coma)",
                                    value = uiState.newStartingPlayersText,
                                    onValueChange = viewModel::onNewStartingPlayersChanged,
                                    placeholder = "Ej. Mateo Silva, Thiago Rossi",
                                )
                                uiState.newMatchError?.let { error ->
                                    Text(error, color = ErrorRed, fontSize = 12.sp)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = viewModel::createNewMatch,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            ) {
                                Text(
                                    "Crear partido",
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::closeNewMatchDialog) {
                                Text("Cancelar", color = TextMuted)
                            }
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmCorrection(operatorUserId) },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                ) {
                    Text("Confirmar", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeCorrectionDialog() }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
        )
    }

    uiState.successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessMessage() },
            containerColor = CardBackground,
            title = {
                Text("Bitácora Actualizada", color = NeonGreen, fontWeight = FontWeight.Bold)
            },
            text = { Text(message, color = TextWhite) },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSuccessMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                ) {
                    Text("Aceptar", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
fun QuickEventButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(84.dp).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NeonGreen,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun MatchEventCard(event: MatchEvent, onEditClick: () -> Unit, onAnnulClick: () -> Unit) {
    val isAnnulled = event.status == MatchEventStatus.ANNULLED

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isAnnulled) CardBackground.copy(alpha = 0.5f) else CardBackground
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = "${event.minute}'",
                    color = if (isAnnulled) TextMuted else NeonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(36.dp),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    val eventTitleText =
                        when (event.eventType) {
                            EventType.GOAL -> "Gol de ${event.playerName} (${event.teamName})"
                            EventType.YELLOW_CARD -> "Tarjeta Amarilla a ${event.playerName}"
                            EventType.RED_CARD -> "Tarjeta Roja a ${event.playerName}"
                            EventType.FOUL -> "Falta de ${event.playerName} (${event.teamName})"
                            EventType.CORNER -> "Tiro de esquina para ${event.teamName}"
                            EventType.SUBSTITUTION ->
                                "Sustitución en ${event.teamName}: ${event.playerName}"
                            else -> "${event.eventType.displayName} para ${event.teamName}"
                        }

                    Text(
                        text = if (isAnnulled) "[ANULADO] $eventTitleText" else eventTitleText,
                        color = if (isAnnulled) ErrorRed else TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    val subtitleText = buildString {
                        if (!event.assistantPlayerName.isNullOrBlank()) {
                            append("Asistencia de ${event.assistantPlayerName}")
                        }
                        if (event.notes.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append(event.notes)
                        }
                    }

                    if (subtitleText.isNotEmpty()) {
                        Text(
                            text = subtitleText,
                            color = TextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }

                    if (event.correctionReason != null) {
                        Text(
                            text = "Motivo: ${event.correctionReason}",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar evento",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(onClick = onAnnulClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Anular evento",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
