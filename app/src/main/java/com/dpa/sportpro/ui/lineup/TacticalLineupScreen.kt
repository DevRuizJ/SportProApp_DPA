package com.dpa.sportpro.ui.lineup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SportsSoccer
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.dpa.sportpro.data.model.ConvocationResponse
import com.dpa.sportpro.data.model.ConvokedPlayer
import com.dpa.sportpro.data.model.LineupStatus
import com.dpa.sportpro.data.model.MatchConvocation
import com.dpa.sportpro.data.repository.ConvocationRepository
import com.dpa.sportpro.data.repository.TacticalLineupRepository
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Locale

private val presetFormations = listOf("4-3-3", "4-4-2", "3-5-2")

private data class FieldSlot(
    val key: String,
    val number: Int,
    val line: Int,
    val x: Float,
    val y: Float
)

@Composable
fun TacticalLineupScreen(
    viewerRole: UserRole,
    convocationId: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val convocationRepository = remember(context) { ConvocationRepository.getInstance(context) }
    val lineupRepository = remember(context) { TacticalLineupRepository.getInstance(context) }
    val publishedConvocations = convocationRepository.convocations.filter { it.isPublished }
    var selectedConvocationId by remember(publishedConvocations, convocationId) {
        mutableStateOf(
            convocationId?.takeIf { id -> publishedConvocations.any { it.id == id } }
                ?: publishedConvocations.firstOrNull()?.id
        )
    }
    val convocation = publishedConvocations.firstOrNull { it.id == selectedConvocationId }
    val savedLineup = convocation?.let { lineupRepository.lineupFor(it.id) }
    val availablePlayers = convocation?.players
        ?.filter { it.response == ConvocationResponse.CONFIRMED }
        .orEmpty()

    var formation by remember(selectedConvocationId, savedLineup) {
        mutableStateOf(savedLineup?.formation ?: "4-3-3")
    }
    var customFormation by remember(selectedConvocationId, savedLineup) {
        mutableStateOf(savedLineup?.formation?.takeIf { it !in presetFormations } ?: "")
    }
    var customFormationInput by remember { mutableStateOf("") }
    var showCustomFormationDialog by remember { mutableStateOf(false) }
    var formationError by remember { mutableStateOf<String?>(null) }
    var showConvocationPicker by remember { mutableStateOf(false) }
    val assigned = remember(selectedConvocationId, savedLineup) {
        mutableStateMapOf<String, String>().apply {
            putAll(savedLineup?.startersBySlot.orEmpty())
        }
    }
    var captainId by remember(selectedConvocationId, savedLineup) {
        mutableStateOf(savedLineup?.captainPlayerId.orEmpty())
    }
    var feedback by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val slots = remember(formation) { formationSlots(formation) }
    val starters = slots.mapNotNull { slot ->
        assigned[slot.key]?.let { playerId -> availablePlayers.firstOrNull { it.id == playerId } }
    }
    val starterIds = starters.map { it.id }.toSet()
    val substitutes = availablePlayers.filter { it.id !in starterIds }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextWhite)
                }
                Column(modifier = Modifier.padding(start = 6.dp)) {
                    Text("Alineación Táctica", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Formación del Equipo • ${savedLineup?.status?.label ?: "Sin guardar"}",
                        color = if (savedLineup == null) TextMuted else NeonGreen,
                        fontSize = 13.sp
                    )
                }
            }

            if (viewerRole != UserRole.COACH) {
                Text(
                    "La pizarra táctica está disponible solo para el Director Técnico.",
                    color = TextMuted,
                    modifier = Modifier.padding(top = 22.dp)
                )
            } else if (publishedConvocations.isEmpty() || convocation == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Text(
                        "Publica una convocatoria para armar la alineación con los jugadores citados.",
                        color = TextMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 12.dp)
                        .background(CardBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                        .clickable { showConvocationPicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${convocation.category} • vs. ${convocation.opponent}",
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${formatDate(convocation.date)} • ${availablePlayers.size} confirmados",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir convocatoria", tint = TextMuted)
                    DropdownMenu(
                        expanded = showConvocationPicker,
                        onDismissRequest = { showConvocationPicker = false },
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        publishedConvocations.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("${option.category} • ${option.opponent}")
                                        Text("${formatDate(option.date)} • ${option.confirmedCount} confirmados", color = TextMuted, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    selectedConvocationId = option.id
                                    showConvocationPicker = false
                                    feedback = null
                                    error = null
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (presetFormations + "Personalizada").forEach { option ->
                        val selected = if (option == "Personalizada") formation !in presetFormations else formation == option
                        Text(
                            text = if (selected && option == "Personalizada" && customFormation.isNotBlank()) customFormation else if (option == "Personalizada" && selected) "PERSONALIZADA" else option,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) Color(0xFF12332F) else CardBackground,
                                    CircleShape
                                )
                                .border(1.dp, if (selected) NeonGreen else InputBorder, CircleShape)
                                .clickable {
                                    if (option == "Personalizada") {
                                        customFormationInput = customFormation
                                        showCustomFormationDialog = true
                                    } else {
                                        formation = option
                                        customFormation = ""
                                        formationError = null
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 12.dp),
                            color = if (selected) NeonGreen else TextMuted,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FieldBoard(
                    slots = slots,
                    convocation = convocation,
                    assigned = assigned,
                    availablePlayers = availablePlayers,
                    captainId = captainId,
                    onAssign = { slotKey, playerId ->
                        assigned.entries.removeAll { it.value == playerId }
                        if (playerId.isBlank()) assigned.remove(slotKey)
                        else assigned[slotKey] = playerId
                        if (captainId !in assigned.values) captainId = ""
                        feedback = null
                        error = null
                    }
                )

                Text(
                    "SUPLENTES",
                    color = TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                )
                if (availablePlayers.isEmpty()) {
                    Text(
                        "Aún no hay jugadores confirmados. Regresa a Convocatorias para registrar respuestas.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                } else if (substitutes.isEmpty()) {
                    Text("Todos los confirmados están en el campo.", color = TextMuted, fontSize = 13.sp)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        substitutes.forEach { player ->
                            Text(
                                player.name,
                                color = TextMuted,
                                modifier = Modifier
                                    .background(CardBackground, CircleShape)
                                    .border(1.dp, InputBorder, CircleShape)
                                    .padding(horizontal = 13.dp, vertical = 9.dp),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                CaptainSelector(
                    players = starters,
                    captainId = captainId,
                    onCaptainChange = {
                        captainId = it
                        feedback = null
                    }
                )

                Spacer(Modifier.weight(1f, fill = true))
                error?.let {
                    Text(it, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                feedback?.let {
                    Text(it, color = NeonGreen, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            lineupRepository.save(
                                convocation = convocation,
                                formation = formation,
                                startersBySlot = assigned.toMap(),
                                captainPlayerId = captainId,
                                status = LineupStatus.DRAFT
                            )
                            error = null
                            feedback = "Alineación guardada como borrador."
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Guardar Borrador", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            try {
                                lineupRepository.save(
                                    convocation = convocation,
                                    formation = formation,
                                    startersBySlot = assigned.toMap(),
                                    captainPlayerId = captainId,
                                    status = LineupStatus.PUBLISHED
                                )
                                error = null
                                feedback = "Alineación publicada para el registro del partido en vivo."
                            } catch (exception: IllegalArgumentException) {
                                error = exception.message
                                feedback = null
                            }
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Publicar", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCustomFormationDialog) {
        AlertDialog(
            onDismissRequest = { showCustomFormationDialog = false },
            containerColor = CardBackground,
            title = { Text("Formación personalizada", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Escribe las líneas de defensa, medio y ataque; deben sumar 10 jugadores (ej. 4-2-3-1).", color = TextMuted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = customFormationInput,
                        onValueChange = { customFormationInput = it.filter { char -> char.isDigit() || char == '-' }.take(11) },
                        label = { Text("Formación") },
                        placeholder = { Text("4-2-3-1") },
                        singleLine = true,
                        colors = lineupFieldColors()
                    )
                    formationError?.let { Text(it, color = ErrorRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formationSlots(customFormationInput).size != 11) {
                            formationError = "La suma de líneas debe ser 10."
                        } else {
                            customFormation = customFormationInput
                            formation = customFormationInput
                            formationError = null
                            showCustomFormationDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Aplicar", color = DarkBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomFormationDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun FieldBoard(
    slots: List<FieldSlot>,
    convocation: MatchConvocation,
    assigned: Map<String, String>,
    availablePlayers: List<ConvokedPlayer>,
    captainId: String,
    onAssign: (String, String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .background(Color(0xFF0D3324), RoundedCornerShape(18.dp))
            .border(2.dp, Color(0xFF24445A), RoundedCornerShape(18.dp))
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        Canvas(Modifier.fillMaxSize().padding(2.dp)) {
            val line = Color(0x668DA79A)
            drawLine(line, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 2.dp.toPx())
            drawCircle(line, radius = size.width * 0.095f, center = Offset(size.width / 2, size.height / 2), style = Stroke(2.dp.toPx()))
            val boxWidth = size.width * 0.38f
            val boxHeight = size.height * 0.22f
            drawRect(line, topLeft = Offset((size.width - boxWidth) / 2, 0f), size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight), style = Stroke(2.dp.toPx()))
            drawRect(line, topLeft = Offset((size.width - boxWidth) / 2, size.height - boxHeight), size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight), style = Stroke(2.dp.toPx()))
        }
        slots.forEach { slot ->
            val playerId = assigned[slot.key]
            val player = availablePlayers.firstOrNull { it.id == playerId }
            FieldSlotPicker(
                slot = slot,
                player = player,
                isCaptain = player?.id == captainId,
                options = availablePlayers.filter { candidate ->
                    candidate.id !in assigned.values || candidate.id == playerId
                },
                onSelect = { onAssign(slot.key, it) },
                modifier = Modifier.offset(
                    x = (maxWidth * slot.x) - 34.dp,
                    y = (maxHeight * slot.y) - 22.dp
                )
            )
        }
    }
    if (availablePlayers.isEmpty()) {
        Text(
            "Campo de ${convocation.category}: solo los jugadores confirmados pueden asignarse.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun FieldSlotPicker(
    slot: FieldSlot,
    player: ConvokedPlayer?,
    isCaptain: Boolean,
    options: List<ConvokedPlayer>,
    onSelect: (String) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.size(width = 68.dp, height = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(if (isCaptain) Color(0xFFFFD54F) else NeonGreen, CircleShape)
                    .border(1.dp, TextWhite, CircleShape)
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: slot.number.toString(),
                    color = DarkBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Vaciar posición") },
                    onClick = {
                        onSelect("")
                        expanded = false
                    }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option.name} (${option.position})") },
                        onClick = {
                            onSelect(option.id)
                            expanded = false
                        }
                    )
                }
            }
        }
        Text(
            player?.name?.substringBefore(" ") ?: slotLabel(slot),
            modifier = Modifier
                .padding(top = 2.dp)
                .background(Color(0xDD06130D), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            color = TextWhite,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CaptainSelector(
    players: List<ConvokedPlayer>,
    captainId: String,
    onCaptainChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val captain = players.firstOrNull { it.id == captainId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
        Text("Capitán", modifier = Modifier.weight(1f).padding(start = 6.dp), color = Color(0xFFFFD54F), fontWeight = FontWeight.SemiBold)
        Box {
            Row(
                modifier = Modifier
                    .background(CardBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, InputBorder, RoundedCornerShape(10.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(captain?.let { "${it.name} (${it.position})" } ?: "Seleccionar", color = if (captain == null) TextMuted else TextWhite, fontSize = 12.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                players.forEach { player ->
                    DropdownMenuItem(
                        text = { Text("${player.name} (${player.position})") },
                        onClick = {
                            onCaptainChange(player.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun lineupFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    focusedBorderColor = NeonGreen,
    unfocusedBorderColor = InputBorder,
    focusedLabelColor = NeonGreen,
    unfocusedLabelColor = TextMuted,
    cursorColor = NeonGreen,
    focusedContainerColor = DarkBackground,
    unfocusedContainerColor = DarkBackground
)

private fun formationSlots(formation: String): List<FieldSlot> {
    val lines = formation.split('-').mapNotNull { it.toIntOrNull() }
    if (lines.isEmpty() || lines.sum() != 10 || lines.any { it !in 1..5 }) return emptyList()
    val slots = mutableListOf(FieldSlot("GK", 1, 0, 0.5f, 0.84f))
    lines.forEachIndexed { lineIndex, playersInLine ->
        val y = 0.70f - lineIndex * (0.43f / maxOf(1, lines.size - 1))
        repeat(playersInLine) { index ->
            val x = (index + 1f) / (playersInLine + 1f)
            val number = slots.size + 1
            slots += FieldSlot("L${lineIndex + 1}P${index + 1}", number, lineIndex + 1, x, y)
        }
    }
    return slots
}

private fun slotLabel(slot: FieldSlot): String = when {
    slot.line == 0 -> "ARQ"
    slot.line == 1 -> "DEF ${slot.number - 1}"
    else -> "POS ${slot.number - 1}"
}

private fun formatDate(value: String): String {
    val parsed = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
    } catch (_: java.text.ParseException) {
        null
    } ?: return value
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(parsed)
}
