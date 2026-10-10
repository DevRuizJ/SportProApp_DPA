package com.dpa.sportpro.ui.convocation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.ConvocationResponse
import com.dpa.sportpro.data.model.ConvokedPlayer
import com.dpa.sportpro.data.model.MatchConvocation
import com.dpa.sportpro.data.model.MatchType
import com.dpa.sportpro.data.repository.AttendanceRepository
import com.dpa.sportpro.data.repository.ConvocationRepository
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

@Composable
fun ConvocationScreen(
    viewerRole: UserRole,
    viewerName: String,
    onBackClick: () -> Unit,
    onLineupClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember(context) { ConvocationRepository.getInstance(context) }
    val rosterRepository = remember(context) { AttendanceRepository(context.applicationContext) }
    val isCoach = viewerRole == UserRole.COACH
    val visibleConvocations =
        if (isCoach) {
            repository.convocations.filter { it.isPublished }
        } else if (viewerRole == UserRole.PLAYER || viewerRole == UserRole.PARENT) {
            repository.convocations.filter { convocation ->
                convocation.isPublished &&
                    convocation.players.any { player ->
                        player.linkedAccountNames.any {
                            it.equals(viewerName.trim(), ignoreCase = true)
                        }
                    }
            }
        } else {
            emptyList()
        }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedConvocationId by
        remember(visibleConvocations) { mutableStateOf(visibleConvocations.firstOrNull()?.id) }
    var responseReason by remember { mutableStateOf("") }
    var responseMessage by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite,
                    )
                }
                Text(
                    if (isCoach) "Convocatorias" else "Mis Convocatorias",
                    modifier = Modifier.padding(start = 6.dp),
                    color = TextWhite,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (isCoach) {
                Text(
                    "Gestiona citaciones y revisa las respuestas del equipo.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp),
                )
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        "＋  Crear convocatoria",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else if (visibleConvocations.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12332F)),
                ) {
                    Text(
                        "Nueva notificación: tienes convocatorias pendientes de respuesta.",
                        color = NeonGreen,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            if (visibleConvocations.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                ) {
                    Text(
                        if (isCoach) "Aún no hay convocatorias publicadas."
                        else "No tienes convocatorias vinculadas a esta cuenta.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            } else {
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    visibleConvocations.forEach { convocation ->
                        val isSelected = selectedConvocationId == convocation.id
                        ConvocationCard(
                            convocation = convocation,
                            viewerRole = viewerRole,
                            viewerName = viewerName,
                            expanded = isSelected,
                            onToggleExpanded = {
                                selectedConvocationId = if (isSelected) null else convocation.id
                                responseMessage = null
                                responseReason = ""
                            },
                            responseReason = responseReason,
                            onReasonChange = { responseReason = it },
                            onRespond = { playerId, response ->
                                repository.respond(
                                    convocationId = convocation.id,
                                    playerId = playerId,
                                    response = response,
                                    justification = responseReason,
                                )
                                responseMessage = "Tu respuesta quedó registrada."
                            },
                            responseMessage = if (isSelected) responseMessage else null,
                            onLineupClick = { onLineupClick(convocation.id) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateConvocationDialog(
            players = rosterRepository.activePlayers,
            onDismiss = { showCreateDialog = false },
            onPublish = { category, competition, opponent, matchType, date, time, venue, players ->
                repository.createAndPublish(
                    category = category,
                    competition = competition,
                    opponent = opponent,
                    matchType = matchType,
                    date = date,
                    callTime = time,
                    venue = venue,
                    players = players,
                )
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun ConvocationCard(
    convocation: MatchConvocation,
    viewerRole: UserRole,
    viewerName: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    responseReason: String,
    onReasonChange: (String) -> Unit,
    onRespond: (String, ConvocationResponse) -> Unit,
    responseMessage: String?,
    onLineupClick: () -> Unit,
) {
    val isCoach = viewerRole == UserRole.COACH
    val linkedPlayers =
        convocation.players.filter { player ->
            player.linkedAccountNames.any { it.equals(viewerName.trim(), ignoreCase = true) }
        }
    val responsePlayer = linkedPlayers.firstOrNull()
    val statusColor =
        when (responsePlayer?.response) {
            ConvocationResponse.CONFIRMED -> NeonGreen
            ConvocationResponse.UNAVAILABLE -> ErrorRed
            else -> TextMuted
        }

    Card(
        modifier =
            Modifier.fillMaxWidth()
                .then(if (!isCoach) Modifier.clickable(onClick = onToggleExpanded) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.background(
                                if (convocation.matchType == MatchType.OFFICIAL) Color(0xFF12332F)
                                else Color(0xFF1A2D4B),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "PARTIDO ${convocation.matchType.label.uppercase(Locale.ROOT)}",
                        color =
                            if (convocation.matchType == MatchType.OFFICIAL) NeonGreen
                            else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(convocation.competition, color = TextMuted, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.size(40.dp).background(DarkBackground, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = NeonGreen)
                }
                Text(
                    "vs. ${convocation.opponent}",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(InputBorder))
            Text(
                "${formatConvocationDate(convocation.date)}  •  ${convocation.callTime} h",
                color = TextMuted,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(convocation.venue, color = TextMuted, modifier = Modifier.padding(top = 5.dp))

            if (isCoach) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(DarkBackground, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${convocation.confirmedCount} Confirmados",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Text(
                        "${convocation.pendingCount} Pendientes",
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Text(
                        "${convocation.unavailableCount} No disponibles",
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    "JUGADORES CITADOS",
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                convocation.players.forEach { player ->
                    ConvokedPlayerRow(player)
                    Spacer(Modifier.height(8.dp))
                }
                Button(
                    onClick = onLineupClick,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Alineación Táctica", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            } else if (responsePlayer != null) {
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(9.dp).background(statusColor, CircleShape))
                    Text(
                        " Tu respuesta: ${responsePlayer.response.label}",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
                if (responsePlayer.justification.isNotBlank()) {
                    Text(
                        "Justificación: ${responsePlayer.justification}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (expanded) {
                    OutlinedTextField(
                        value = responseReason,
                        onValueChange = onReasonChange,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        label = { Text("Justificación (opcional)") },
                        minLines = 2,
                        colors = convocationFieldColors(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                onRespond(responsePlayer.id, ConvocationResponse.CONFIRMED)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "Confirmo Asistencia",
                                color = DarkBackground,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Button(
                            onClick = {
                                onRespond(responsePlayer.id, ConvocationResponse.UNAVAILABLE)
                            },
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = ErrorRed.copy(alpha = 0.18f)
                                ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        ) {
                            Text(
                                "No disponible",
                                color = ErrorRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    responseMessage?.let {
                        Text(
                            it,
                            color = NeonGreen,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConvokedPlayerRow(player: ConvokedPlayer) {
    val color =
        when (player.response) {
            ConvocationResponse.CONFIRMED -> NeonGreen
            ConvocationResponse.UNAVAILABLE -> ErrorRed
            ConvocationResponse.PENDING -> TextMuted
        }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(DarkBackground, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(CardBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(21.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(player.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(player.position, color = TextMuted, fontSize = 12.sp)
            if (player.justification.isNotBlank()) {
                Text(
                    player.justification,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Text(
            player.response.label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier =
                Modifier.background(color.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun CreateConvocationDialog(
    players: List<com.dpa.sportpro.data.model.AttendancePlayer>,
    onDismiss: () -> Unit,
    onPublish:
        (String, String, String, MatchType, String, String, String, List<ConvokedPlayer>) -> Unit,
) {
    val context = LocalContext.current
    val categories = players.map { it.category }.distinct()
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var opponent by remember { mutableStateOf("") }
    var competition by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf(MatchType.OFFICIAL) }
    var date by remember { mutableStateOf(todayAsString()) }
    var callTime by remember { mutableStateOf("16:00") }
    var venue by remember { mutableStateOf("") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedPlayers = remember { mutableStateMapOf<String, Boolean>() }
    val categoryPlayers = players.filter { it.category == selectedCategory }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Crear convocatoria", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ConvocationInput(opponent, { opponent = it }, "Rival", "Ej. Academia Cantolao")
                ConvocationInput(
                    competition,
                    { competition = it },
                    "Torneo / Competencia",
                    "Ej. Torneo de Clausura",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MatchType.entries.forEach { option ->
                        val selected = matchType == option
                        Text(
                            option.label,
                            modifier =
                                Modifier.weight(1f)
                                    .background(
                                        if (selected) NeonGreen.copy(alpha = 0.15f)
                                        else DarkBackground,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) NeonGreen else InputBorder,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable { matchType = option }
                                    .padding(12.dp),
                            color = if (selected) NeonGreen else TextMuted,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickerButton(
                        label = "Fecha",
                        value = formatConvocationDate(date),
                        modifier = Modifier.weight(1f),
                    ) {
                        val calendar = parseConvocationDate(date)
                        DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    date = "%04d-%02d-%02d".format(Locale.US, year, month + 1, day)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                            )
                            .show()
                    }
                    PickerButton(
                        label = "Citación",
                        value = callTime,
                        modifier = Modifier.weight(1f),
                    ) {
                        TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    callTime = "%02d:%02d".format(Locale.US, hour, minute)
                                },
                                16,
                                0,
                                true,
                            )
                            .show()
                    }
                }
                ConvocationInput(venue, { venue = it }, "Lugar del encuentro", "Sede / cancha")
                Box {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(10.dp))
                                .clickable { categoryMenuExpanded = true }
                                .padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Categoría: $selectedCategory",
                            color = TextWhite,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextMuted,
                        )
                    }
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Text("Selecciona jugadores", color = TextWhite, fontWeight = FontWeight.SemiBold)
                categoryPlayers.forEach { player ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth().clickable {
                                selectedPlayers[player.id] = !(selectedPlayers[player.id] ?: true)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selectedPlayers[player.id] ?: true,
                            onCheckedChange = { selectedPlayers[player.id] = it },
                        )
                        Column {
                            Text(player.name, color = TextWhite, fontSize = 14.sp)
                            Text(player.category, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
                error?.let { Text(it, color = ErrorRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selected = categoryPlayers.filter { selectedPlayers[it.id] ?: true }
                    if (
                        opponent.isBlank() ||
                            competition.isBlank() ||
                            venue.isBlank() ||
                            selectedCategory.isBlank() ||
                            selected.isEmpty()
                    ) {
                        error = "Completa los datos y selecciona al menos un jugador."
                    } else {
                        onPublish(
                            selectedCategory,
                            competition,
                            opponent,
                            matchType,
                            date,
                            callTime,
                            venue,
                            selected.map { player ->
                                val (displayName, position, linkedNames) =
                                    playerInfo(player.id, player.name)
                                ConvokedPlayer(
                                    id = player.id,
                                    name = displayName,
                                    position = position,
                                    linkedAccountNames = linkedNames,
                                )
                            },
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground)
                Text(" Publicar convocatoria", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } },
    )
}

@Composable
private fun ConvocationInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextMuted) },
        singleLine = true,
        colors = convocationFieldColors(),
    )
}

@Composable
private fun PickerButton(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(
            value,
            color = TextWhite,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 5.dp)
                    .background(DarkBackground, RoundedCornerShape(10.dp))
                    .padding(12.dp),
        )
    }
}

@Composable
private fun convocationFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = NeonGreen,
        unfocusedBorderColor = InputBorder,
        focusedLabelColor = NeonGreen,
        unfocusedLabelColor = TextMuted,
        cursorColor = NeonGreen,
        focusedContainerColor = DarkBackground,
        unfocusedContainerColor = DarkBackground,
    )

private fun playerInfo(
    playerId: String,
    fallbackName: String,
): Triple<String, String, List<String>> =
    when (playerId) {
        "player_mateo" ->
            Triple("Mateo Silva", "Delantero", listOf("Mateo Silva", "Mateo Silva Rossi"))
        "player_lucas" ->
            Triple("Lucas Gomez S.", "Defensa", listOf("Lucas Gomez S.", "Lucas Castro"))
        "player_thiago" ->
            Triple(
                "Thiago Ruiz Flores",
                "Mediocampista",
                listOf("Thiago Ruiz Flores", "Thiago Rossi"),
            )
        "player_gabriel" ->
            Triple("Gabriel Mendez O.", "Defensa", listOf("Gabriel Mendez O.", "Gabriel Ruiz"))
        else -> Triple(fallbackName, "Jugador", listOf(fallbackName))
    }

private fun todayAsString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

private fun parseConvocationDate(value: String): Calendar {
    val calendar = Calendar.getInstance()
    val parsed =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
        } catch (_: java.text.ParseException) {
            null
        }
    if (parsed != null) calendar.time = parsed
    return calendar
}

private fun formatConvocationDate(value: String): String {
    val parsed =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
        } catch (_: java.text.ParseException) {
            null
        } ?: return value
    return SimpleDateFormat("EEEE dd 'de' MMMM", Locale("es", "PE")).format(parsed)
}
