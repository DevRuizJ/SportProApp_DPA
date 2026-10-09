package com.dpa.sportpro.ui.fees

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.dpa.sportpro.data.model.FeePlayer
import com.dpa.sportpro.data.model.FeeStatus
import com.dpa.sportpro.data.model.MonthlyFee
import com.dpa.sportpro.data.repository.MonthlyFeeRepository
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite
import java.util.Calendar
import java.util.Locale

private val spanishMonths = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)
private val schoolYears = (Calendar.getInstance().get(Calendar.YEAR) - 2..Calendar.getInstance().get(Calendar.YEAR) + 1).toList()

@Composable
fun MonthlyFeesScreen(
    viewerRole: UserRole,
    viewerName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { MonthlyFeeRepository(context.applicationContext) }
    val isAdministrator = viewerRole == UserRole.ADMIN
    val authorizedViewer = viewerRole == UserRole.PARENT || viewerRole == UserRole.PLAYER
    val visiblePlayers = repository.visiblePlayers(viewerRole, viewerName)
    var adminView by remember { mutableStateOf(true) }
    var selectedPlayerId by remember { mutableStateOf(repository.players.firstOrNull()?.id.orEmpty()) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var showPlayerPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var editingFee by remember { mutableStateOf<MonthlyFee?>(null) }

    val canEdit = isAdministrator && adminView
    val currentPlayers = if (isAdministrator && adminView) {
        repository.players
    } else {
        visiblePlayers
    }
    val selectedPlayer = currentPlayers.firstOrNull { it.id == selectedPlayerId }
        ?: currentPlayers.firstOrNull()
    val months = Calendar.JANUARY..Calendar.DECEMBER

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
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
                    "Mensualidades",
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    color = TextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isAdministrator && !authorizedViewer) {
                Text(
                    "Esta sección está disponible para Administración y Familia.",
                    color = TextMuted,
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                if (isAdministrator) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 18.dp)
                            .background(CardBackground, RoundedCornerShape(14.dp))
                            .border(1.dp, InputBorder, RoundedCornerShape(14.dp))
                            .padding(4.dp)
                    ) {
                        ViewTab(
                            title = "Vista Admin (Editar)",
                            selected = adminView,
                            modifier = Modifier.weight(1f),
                            onClick = { adminView = true }
                        )
                        ViewTab(
                            title = "Vista Familiar",
                            selected = !adminView,
                            modifier = Modifier.weight(1f),
                            onClick = { adminView = false }
                        )
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                }

                if (currentPlayers.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SelectorField(
                                value = selectedPlayer?.let { "${it.name} (${it.category})" } ?: "Seleccionar jugador",
                                onClick = { showPlayerPicker = true }
                            )
                            DropdownMenu(
                                expanded = showPlayerPicker,
                                onDismissRequest = { showPlayerPicker = false }
                            ) {
                                currentPlayers.forEach { player ->
                                    DropdownMenuItem(
                                        text = { Text("${player.name} (${player.category})") },
                                        onClick = {
                                            selectedPlayerId = player.id
                                            showPlayerPicker = false
                                        }
                                    )
                                }
                            }
                        }
                        if (isAdministrator) {
                            Box(modifier = Modifier.weight(0.55f)) {
                                SelectorField(
                                    value = selectedYear.toString(),
                                    onClick = { showYearPicker = true }
                                )
                                DropdownMenu(
                                    expanded = showYearPicker,
                                    onDismissRequest = { showYearPicker = false }
                                ) {
                                    schoolYears.forEach { year ->
                                        DropdownMenuItem(
                                            text = { Text(year.toString()) },
                                            onClick = {
                                                selectedYear = year
                                                showYearPicker = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1A2D4B))
                                        .padding(horizontal = 18.dp, vertical = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableHeader("MES", Modifier.weight(1f))
                                    TableHeader("MONTO", Modifier.weight(1f), centered = true)
                                    TableHeader("ESTADO", Modifier.weight(1f), centered = true)
                                }
                                months.forEach { month ->
                                    val fee = repository.feeFor(selectedPlayer!!.id, selectedYear, month)
                                    FeeRow(
                                        monthName = spanishMonths[month],
                                        fee = fee,
                                        editable = canEdit,
                                        onEdit = { editingFee = fee }
                                    )
                                }
                            }
                        }
                        Text(
                            "Montos de referencia para control interno. Esta vista no procesa pagos.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground)
                    ) {
                        Text(
                            "No hay mensualidades vinculadas a esta cuenta. Contacta al administrador para asociar al jugador.",
                            color = TextMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            }
        }
    }

    editingFee?.let { fee ->
        EditFeeDialog(
            monthName = spanishMonths[fee.month],
            initialAmount = fee.amount,
            initialStatus = fee.status,
            onDismiss = { editingFee = null },
            onSave = { amount, status ->
                repository.updateFee(
                    playerId = fee.playerId,
                    year = fee.year,
                    month = fee.month,
                    amount = amount,
                    status = status
                )
                editingFee = null
            }
        )
    }
}

@Composable
private fun ViewTab(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                if (selected) Color(0xFF1A2D4B) else Color.Transparent,
                RoundedCornerShape(11.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            color = if (selected) NeonGreen else TextMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SelectorField(value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(15.dp))
            .border(1.dp, InputBorder, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Text(
            value,
            color = TextWhite,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp
        )
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier, centered: Boolean = false) {
    Text(
        text,
        modifier = modifier,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = if (centered) androidx.compose.ui.text.style.TextAlign.Center
        else androidx.compose.ui.text.style.TextAlign.Start
    )
}

@Composable
private fun FeeRow(
    monthName: String,
    fee: MonthlyFee,
    editable: Boolean,
    onEdit: () -> Unit
) {
    val statusColor = when (fee.status) {
        FeeStatus.PENDING -> Color(0xFFFFB300)
        FeeStatus.PAID -> NeonGreen
        FeeStatus.EXEMPT -> Color(0xFF00B8FF)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (editable) Modifier.clickable(onClick = onEdit) else Modifier)
            .border(width = 0.5.dp, color = InputBorder)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            monthName,
            modifier = Modifier.weight(1f),
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Text(
            "S/ ${"%.2f".format(Locale.US, fee.amount)}",
            modifier = Modifier.weight(1f),
            color = TextWhite,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    fee.status.label,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (editable) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar mensualidad",
                        tint = statusColor,
                        modifier = Modifier.size(13.dp).padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditFeeDialog(
    monthName: String,
    initialAmount: Double,
    initialStatus: FeeStatus,
    onDismiss: () -> Unit,
    onSave: (Double, FeeStatus) -> Unit
) {
    var amountText by remember(initialAmount) {
        mutableStateOf("%.2f".format(Locale.US, initialAmount))
    }
    var status by remember(initialStatus) { mutableStateOf(initialStatus) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text("Mensualidad de $monthName", color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { character -> character.isDigit() || character == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monto asignado (S/)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = feeFieldColors()
                )
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                            .clickable { statusMenuExpanded = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Estado: ${status.label}", modifier = Modifier.weight(1f), color = TextWhite)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                    }
                    DropdownMenu(
                        expanded = statusMenuExpanded,
                        onDismissRequest = { statusMenuExpanded = false }
                    ) {
                        FeeStatus.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    status = option
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount < 0.0) {
                        error = "Ingresa un monto válido, igual o mayor a cero."
                    } else {
                        onSave(amount, status)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Text("Guardar", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        }
    )
}

@Composable
private fun feeFieldColors() = OutlinedTextFieldDefaults.colors(
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
