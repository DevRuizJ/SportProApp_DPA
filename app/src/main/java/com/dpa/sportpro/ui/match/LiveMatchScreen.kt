package com.dpa.sportpro.ui.match

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.domain.match.*
import com.dpa.sportpro.ui.components.SportProInputField
import com.dpa.sportpro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveMatchScreen(onBack: () -> Unit, vm: LiveMatchViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var create by rememberSaveable { mutableStateOf(false) }
    var editingType by remember { mutableStateOf<String?>(null) }
    var editingEvent by remember { mutableStateOf<LiveEvent?>(null) }
    var voiding by remember { mutableStateOf(false) }
    var catalog by rememberSaveable { mutableStateOf(false) }
    var confirmClock by remember { mutableStateOf<ClockAction?>(null) }
    LaunchedEffect(s.completedAction) {
        if (s.completedAction > 0) { create = false; editingType = null; editingEvent = null }
    }
    BackHandler(s.selectedId != null && !create && editingType == null && s.history == null) { vm.backToList() }
    Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            item {
                TextButton(onClick = { if (s.selectedId == null) onBack() else vm.backToList() }) { Text("Volver", color = NeonGreen) }
                Text("Partidos en vivo", color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            s.error?.let { item { Text(it, color = ErrorRed) } }
            s.notice?.let { item { Text(it, color = TextMuted) } }
            if (s.loading) item { CircularProgressIndicator(color = NeonGreen) }
            if (s.selectedId == null) {
                item {
                    SelectionContainer { Text("Mi identificador: ${vm.user.uid}", color = TextMuted) }
                    Text("Comparte tu identificador con el responsable para que te dé acceso al partido.", color = TextMuted)
                    if (vm.canCreate) ActionButton("Crear partido", !s.busy) { create = true }
                    else Text("Puedes seguir los partidos a los que te hayan invitado.", color = TextMuted)
                }
                if (!s.loading && s.matches.isEmpty()) item { Text("No tienes partidos asignados.", color = TextWhite) }
                items(s.matches, key = { it.id }) { m ->
                    Card(colors = CardDefaults.cardColors(containerColor = CardBackground), onClick = { vm.select(m.id) }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("${m.home} vs ${m.away}", color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("${m.category} · ${phaseLabel(m.clock.phase)}", color = TextMuted)
                            if (m.fromCache) Text("Copia local; esperando conexión", color = TextMuted)
                        }
                    }
                }
            } else {
                val m = s.match
                if (m != null) {
                    val score = LiveEventTypes.score(s.events)
                    val operator = (vm.canCreate || vm.user.offlineSession) && vm.user.uid in m.operatorUids
                    item {
                        Text("${m.home}  ${score.home} — ${score.away}  ${m.away}", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("${clockText(s.seconds)} · ${phaseLabel(m.clock.phase)}", color = NeonGreen, fontSize = 22.sp)
                        Text("${m.category} · ${m.periodMinutes} minutos por periodo", color = TextMuted)
                        val threshold = m.periodMinutes * 60L * if (m.clock.phase in setOf(ClockPhase.SECOND, ClockPhase.PAUSED_SECOND, ClockPhase.FINISHED)) 2 else 1
                        if (s.seconds > threshold) Text("Tiempo adicional: ${clockText(s.seconds - threshold)}", color = TextMuted)
                        if (s.seconds >= 14_400 && m.clock.phase != ClockPhase.FINISHED) Text("Límite de 240 minutos alcanzado. El reloj se detuvo; cierra los periodos pendientes.", color = ErrorRed)
                        if (m.fromCache) Text("Sin confirmación del servidor: mostrando copia local.", color = TextMuted)
                        if (!s.calibrated) Text("Reloj aproximado hasta recuperar conexión.", color = TextMuted)
                    }
                    if (operator) {
                        item {
                            val actions = when (m.clock.phase) {
                                ClockPhase.READY -> listOf(ClockAction.START to "Iniciar primer tiempo")
                                ClockPhase.FIRST -> listOf(ClockAction.PAUSE to "Pausar", ClockAction.END_PERIOD to "Terminar primer tiempo")
                                ClockPhase.PAUSED_FIRST -> listOf(ClockAction.RESUME to "Reanudar", ClockAction.END_PERIOD to "Terminar primer tiempo")
                                ClockPhase.HALF_TIME -> listOf(ClockAction.START to "Iniciar segundo tiempo")
                                ClockPhase.SECOND -> listOf(ClockAction.PAUSE to "Pausar", ClockAction.FINISH to "Finalizar partido")
                                ClockPhase.PAUSED_SECOND -> listOf(ClockAction.RESUME to "Reanudar", ClockAction.FINISH to "Finalizar partido")
                                ClockPhase.FINISHED -> emptyList()
                            }
                            actions.forEach { (action, label) -> ActionButton(label, !s.busy) { confirmClock = action } }
                            Text("El control del reloj requiere conexión. Puedes registrar eventos pendientes sin conexión.", color = TextMuted)
                            if (m.clock.phase != ClockPhase.READY) {
                                Text(if (m.clock.phase == ClockPhase.FINISHED) "Agregar evento omitido" else "Registrar evento", color = TextWhite, fontWeight = FontWeight.Bold)
                                LiveEventTypes.labels.filterKeys { it !in LiveEventTypes.periodTypes && it in s.catalog.enabled }
                                    .entries.chunked(2).forEach { pair ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            pair.forEach { (type, label) ->
                                                OutlinedButton(onClick = { editingEvent = null; voiding = false; editingType = type },
                                                    enabled = !s.busy, modifier = Modifier.weight(1f)) { Text(label, color = NeonGreen) }
                                            }
                                        }
                                    }
                            }
                            if (m.ownerUid == vm.user.uid) TextButton(onClick = { catalog = !catalog }) { Text("Catálogo de ${m.category}", color = NeonGreen) }
                        }
                        if (catalog && m.ownerUid == vm.user.uid) item {
                            Text("Los cambios se comparten entre tus partidos de esta categoría.", color = TextMuted)
                            LiveEventTypes.labels.forEach { (type, label) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, color = TextWhite, modifier = Modifier.weight(1f).padding(top = 12.dp))
                                    Switch(checked = type in s.catalog.enabled,
                                        onCheckedChange = { vm.toggleEvent(type, it) },
                                        enabled = !s.busy && type !in LiveEventTypes.periodTypes)
                                }
                            }
                        }
                        if (s.pending.isNotEmpty()) {
                            item {
                                Text("Registros locales sin confirmar", color = TextWhite, fontWeight = FontWeight.Bold)
                                ActionButton("Reintentar sincronización", !s.busy) { vm.sync() }
                            }
                            items(s.pending, key = { it.operationId }) { p ->
                                Text((if (p.rejectedAtMillis != null) "Requiere revisión: " else "Pendiente: ") +
                                    (p.lastError ?: "Esperando confirmación del servidor."), color = if (p.rejectedAtMillis != null) ErrorRed else TextMuted)
                            }
                        }
                    }
                    item { Text("Cronología", color = TextWhite, fontWeight = FontWeight.Bold) }
                    if (s.events.isEmpty()) item { Text("Aún no hay eventos confirmados.", color = TextMuted) }
                    items(s.events, key = { it.id }) { e ->
                        Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${eventIcon(e.typeId)} ${clockText(e.seconds.toLong())} · ${LiveEventTypes.labels[e.typeId]}",
                                    color = if (e.voided) TextMuted else TextWhite, fontWeight = FontWeight.Bold,
                                    textDecoration = if (e.voided) TextDecoration.LineThrough else TextDecoration.None)
                                if (e.side.isNotBlank()) Text(
                                    (if (e.side == "HOME") m.home else m.away) + " · " + e.player.ifBlank { "Jugador no identificado" },
                                    color = TextMuted)
                                if (e.typeId == "substitution") Text("Entra: " + e.otherPlayer.ifBlank { "Jugador no identificado" }, color = TextMuted)
                                if (e.detail.isNotBlank()) Text(e.detail, color = TextMuted)
                                if (e.observations.isNotBlank()) Text(e.observations, color = TextMuted)
                                if (e.voided) Text("Anulado · ${e.reason}", color = ErrorRed)
                                else if (e.version > 1) Text("Corregido · versión ${e.version} · ${e.reason}", color = NeonGreen)
                                Row {
                                    TextButton(onClick = { vm.showHistory(e) }, enabled = !s.busy) { Text("Historial", color = NeonGreen) }
                                    if (operator && !e.voided && e.typeId !in LiveEventTypes.periodTypes) {
                                        TextButton(onClick = { editingEvent = e; voiding = false; editingType = e.typeId }, enabled = !s.busy) { Text("Corregir", color = NeonGreen) }
                                        TextButton(onClick = { editingEvent = e; voiding = true; editingType = e.typeId }, enabled = !s.busy) { Text("Anular", color = ErrorRed) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (create) CreateMatchDialog(s.busy, s.error, { create = false }, vm::create)
    editingType?.let { type ->
        EventDialog(type, editingEvent, voiding, s.seconds, s.busy, s.error,
            onDismiss = { editingType = null },
            onSave = { minutes, seconds, side, player, other, detail, observations, reason ->
                vm.record(type, minutes, seconds, side, player, other, detail, observations, editingEvent, voiding, reason)
            })
    }
    confirmClock?.let { action ->
        AlertDialog(onDismissRequest = { confirmClock = null },
            title = { Text("Confirmar cambio del partido") },
            text = { Text("Se guardará el cambio de reloj y, al iniciar o terminar un periodo, su evento en la cronología.") },
            confirmButton = { TextButton(onClick = { confirmClock = null; vm.changeClock(action) }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { confirmClock = null }) { Text("Cancelar") } })
    }
    s.history?.let { history ->
        AlertDialog(onDismissRequest = vm::dismissHistory, containerColor = CardBackground,
            title = { Text("Historial de cambios", color = TextWhite) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    history.forEach { e ->
                        Text("Versión ${e.version} · ${if(e.voided) "Anulado" else "Activo"}\n" +
                            "${clockText(e.seconds.toLong())} · ${e.side} · ${e.player.ifBlank { "No identificado" }}\n" +
                            "Registrado por: ${e.actorUid}\n" +
                            (e.recordedAtMillis?.let { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "Pendiente") +
                            "\n${e.reason}", color = TextWhite)
                    }
                }
            },
            confirmButton = { TextButton(onClick = vm::dismissHistory) { Text("Cerrar", color = NeonGreen) } })
    }
}

@Composable private fun ActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkBackground)) { Text(label) }
}
@Composable private fun Input(label: String, value: String, changed: (String) -> Unit) {
    SportProInputField(label = label, value = value, onValueChange = changed, placeholder = label)
}
@Composable private fun CreateMatchDialog(busy: Boolean, error: String?, dismiss: () -> Unit,
    save: (String, String, String, String, String, String) -> Unit) {
    var home by rememberSaveable { mutableStateOf("") }; var away by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }; var minutes by rememberSaveable { mutableStateOf("45") }
    var viewers by rememberSaveable { mutableStateOf("") }; var operators by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, containerColor = CardBackground,
        title = { Text("Crear partido", color = TextWhite) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            error?.let { Text(it, color = ErrorRed) }
            Input("Equipo local", home) { home = it }; Input("Equipo visitante", away) { away = it }
            Input("Categoría", category) { category = it }; Input("Minutos por periodo", minutes) { minutes = it }
            Input("Identificadores de espectadores (separados por coma)", viewers) { viewers = it }
            Input("Identificadores de otros operadores (opcional)", operators) { operators = it }
            Text("Los espectadores comparten su identificador desde Partidos en vivo. Otros operadores necesitan un rol DT/ADM aprobado. El partido solo será visible para los participantes indicados.", color = TextMuted)
        }},
        confirmButton = { TextButton(enabled = !busy, onClick = { save(home, away, category, minutes, viewers, operators) }) { Text(if(busy) "Guardando…" else "Crear", color = NeonGreen) } },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Cancelar", color = TextMuted) } })
}
@Composable private fun EventDialog(type: String, event: LiveEvent?, voiding: Boolean, now: Long, busy: Boolean,
    error: String?, onDismiss: () -> Unit, onSave: (String,String,String,String,String,String,String,String) -> Unit) {
    val initial = event?.seconds?.toLong() ?: now
    var minutes by rememberSaveable { mutableStateOf((initial / 60).toString()) }
    var seconds by rememberSaveable { mutableStateOf((initial % 60).toString()) }
    var side by rememberSaveable { mutableStateOf(event?.side ?: "HOME") }
    var player by rememberSaveable { mutableStateOf(event?.player.orEmpty()) }
    var other by rememberSaveable { mutableStateOf(event?.otherPlayer.orEmpty()) }
    var detail by rememberSaveable { mutableStateOf(event?.detail.orEmpty()) }
    var observations by rememberSaveable { mutableStateOf(event?.observations.orEmpty()) }
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, containerColor = CardBackground,
        title = { Text((if(voiding) "Anular: " else if(event != null) "Corregir: " else "") + LiveEventTypes.labels[type], color = TextWhite) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            error?.let { Text(it, color = ErrorRed) }
            if (!voiding) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { Input("Minuto", minutes) { minutes = it } }
                    Column(Modifier.weight(1f)) { Input("Segundo", seconds) { seconds = it } }
                }
                Row {
                    FilterChip(selected = side == "HOME", onClick = { side = "HOME" }, label = { Text("Local") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = side == "AWAY", onClick = { side = "AWAY" }, label = { Text("Visitante") })
                }
                Input(if(type == "substitution") "Jugador que sale (opcional)" else "Jugador (opcional)", player) { player = it }
                if(type == "substitution") Input("Jugador que entra (opcional)", other) { other = it }
                Text("Vacío significa jugador no identificado. Registra solo datos deportivos necesarios.", color = TextMuted)
                Input(if(type == "goal") "Tipo de jugada (obligatorio)" else if(type.endsWith("card")) "Motivo (obligatorio)" else "Detalle (opcional)", detail) { detail = it }
                Input("Observaciones (opcional)", observations) { observations = it }
            }
            if(event != null) Input("Motivo de ${if(voiding) "anulación" else "corrección"}", reason) { reason = it }
        }},
        confirmButton = { TextButton(enabled = !busy, onClick = { onSave(minutes, seconds, side, player, other, detail, observations, reason) }) {
            Text(if(busy) "Guardando…" else if(voiding) "Confirmar anulación" else "Guardar", color = NeonGreen) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar", color = TextMuted) } })
}
private fun clockText(seconds: Long) = "%02d:%02d".format(seconds / 60, seconds % 60)
private fun phaseLabel(phase: ClockPhase) = when(phase) {
    ClockPhase.READY -> "Programado"; ClockPhase.FIRST -> "Primer tiempo"; ClockPhase.HALF_TIME -> "Descanso"
    ClockPhase.SECOND -> "Segundo tiempo"; ClockPhase.PAUSED_FIRST, ClockPhase.PAUSED_SECOND -> "Pausado"
    ClockPhase.FINISHED -> "Finalizado"
}
private fun eventIcon(type: String) = when(type) {
    "goal" -> "⚽"; "yellow_card" -> "🟨"; "red_card" -> "🟥"; "substitution" -> "↔"; "foul" -> "⚑"; else -> "•"
}
