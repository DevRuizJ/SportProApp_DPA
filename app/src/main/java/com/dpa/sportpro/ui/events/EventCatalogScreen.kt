package com.dpa.sportpro.ui.events

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.dpa.sportpro.data.model.MatchEventType
import com.dpa.sportpro.data.repository.AcademyRepository
import com.dpa.sportpro.data.repository.MatchEventCatalogRepository
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite
import java.util.Locale

private val defaultCategories = listOf("Sub-10", "Sub-12", "Sub-15", "Sub-17", "Primera")

@Composable
fun EventCatalogScreen(
    viewerRole: UserRole,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { MatchEventCatalogRepository(context.applicationContext) }
    val academyRepository = remember(context) { AcademyRepository(context.applicationContext) }
    val categories = remember(academyRepository.academies) {
        (academyRepository.academies.flatMap { academy -> academy.categories.map { it.name } } +
            defaultCategories).distinct()
    }
    var selectedCategory by remember(categories) { mutableStateOf(categories.first()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextWhite)
                }
                Column(modifier = Modifier.padding(start = 6.dp)) {
                    Text("Catálogo de Eventos", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Configura eventos rápidos por categoría", color = TextMuted, fontSize = 14.sp)
                }
            }

            if (viewerRole != UserRole.ADMIN && viewerRole != UserRole.COACH) {
                Text(
                    "El catálogo está disponible solo para Administración y Dirección Técnica.",
                    color = ErrorRed,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackground, RoundedCornerShape(13.dp))
                            .border(1.dp, InputBorder, RoundedCornerShape(13.dp))
                            .clickable { categoryMenuExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Categoría: $selectedCategory",
                            modifier = Modifier.weight(1f),
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar categoría", tint = TextMuted)
                    }
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repository.eventTypes.forEach { event ->
                        EventTypeCard(
                            event = event,
                            enabled = repository.isEnabled(selectedCategory, event.id),
                            onEnabledChange = {
                                repository.setEnabled(selectedCategory, event.id, it)
                            }
                        )
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 18.dp)
                        .height(54.dp)
                        .border(1.5.dp, NeonGreen, RoundedCornerShape(14.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("+ Agregar Evento", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            existingEventNames = repository.eventTypes.map { it.name },
            onDismiss = { showAddDialog = false },
            onSave = { name, description, required, optional ->
                repository.addCustomEvent(name, description, required, optional)
                repository.setEnabled(
                    selectedCategory,
                    repository.eventTypes.last().id,
                    true
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EventTypeCard(
    event: MatchEventType,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF203451), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = TextWhite, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(event.name, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    event.description,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                val schema = buildList {
                    if (event.requiredFields.isNotEmpty()) {
                        add("Obligatorios: ${event.requiredFields.joinToString()}")
                    }
                    if (event.optionalFields.isNotEmpty()) {
                        add("Opcionales: ${event.optionalFields.joinToString()}")
                    }
                }.joinToString(" • ")
                if (schema.isNotBlank()) {
                    Text(
                        schema,
                        color = if (enabled) NeonGreen.copy(alpha = 0.85f) else TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextWhite,
                    checkedTrackColor = NeonGreen,
                    uncheckedThumbColor = TextWhite,
                    uncheckedTrackColor = Color(0xFF263A53),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun AddEventDialog(
    existingEventNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var requiredFieldsText by remember { mutableStateOf("") }
    var optionalFieldsText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Agregar tipo de evento", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EventInput(name, { name = it }, "Nombre del evento", "Ej. Revisión VAR")
                EventInput(description, { description = it }, "Descripción", "Detalle para el operador")
                EventInput(requiredFieldsText, { requiredFieldsText = it }, "Campos obligatorios", "Separados por coma")
                Text(
                    "Ejemplo: Autor, Minuto, Equipo",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = -8.dp)
                )
                EventInput(optionalFieldsText, { optionalFieldsText = it }, "Campos opcionales", "Separados por coma")
                error?.let { Text(it, color = ErrorRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || description.isBlank() || requiredFieldsText.isBlank()) {
                        error = "Completa nombre, descripción y al menos un campo obligatorio."
                    } else if (existingEventNames.any { it.equals(name.trim(), ignoreCase = true) }) {
                        error = "Ya existe un evento con ese nombre."
                    } else {
                        onSave(
                            name,
                            description,
                            parseFields(requiredFieldsText),
                            parseFields(optionalFieldsText)
                        )
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
private fun EventInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextMuted) },
        singleLine = label != "Descripción",
        colors = OutlinedTextFieldDefaults.colors(
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
    )
}

private fun parseFields(value: String): List<String> =
    value.split(",").map(String::trim).filter(String::isNotBlank).distinct()
