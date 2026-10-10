package com.dpa.sportpro.ui.training

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsSoccer
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.AcademyCategory
import com.dpa.sportpro.data.model.TrainingExercise
import com.dpa.sportpro.data.model.TrainingSession
import com.dpa.sportpro.data.repository.AcademyRepository
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

private val exerciseIntensities = listOf("Baja", "Media", "Alta")
private val fallbackCategories =
    listOf(
        AcademyCategory("Sub-10", "Carlos Gómez"),
        AcademyCategory("Sub-12", "Carlos Gómez"),
        AcademyCategory("Sub-15", "Carlos Gómez"),
        AcademyCategory("Sub-17", "Carlos Gómez"),
        AcademyCategory("Primera", "Carlos Gómez"),
    )

@Composable
fun TrainingPlannerScreen(viewerRole: UserRole, viewerName: String, onBackClick: () -> Unit) {
    if (viewerRole != UserRole.COACH) {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Planificación disponible solo para el Director Técnico.", color = TextWhite)
                TextButton(onClick = onBackClick) { Text("Volver", color = NeonGreen) }
            }
        }
        return
    }

    val context = LocalContext.current
    val sessionRepository =
        remember(context) { TrainingSessionRepository(context.applicationContext) }
    val academyRepository = remember(context) { AcademyRepository(context.applicationContext) }
    val coachCategories =
        remember(academyRepository.academies, viewerName) {
            academyRepository.academies
                .flatMap { academy ->
                    academy.categories.filter {
                        it.coachName.equals(viewerName.trim(), ignoreCase = true)
                    }
                }
                .distinctBy { it.name.lowercase(Locale.ROOT) }
                .ifEmpty { fallbackCategories }
        }
    val categoryOptions = coachCategories.map { "${it.name} (${it.coachName})" }

    var selectedDate by remember { mutableStateOf(todayAsString()) }
    var startTime by remember { mutableStateOf("16:00") }
    var venue by remember { mutableStateOf("") }
    var category by remember(categoryOptions) { mutableStateOf(categoryOptions.first()) }
    var objective by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var showExerciseDialog by remember { mutableStateOf(false) }
    var showLibraryDialog by remember { mutableStateOf(false) }
    val selectedExercises = remember { mutableStateListOf<TrainingExercise>() }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp),
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
                    "Planificar Sesión",
                    modifier = Modifier.padding(start = 6.dp),
                    color = TextWhite,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PickerField(
                    label = "Fecha",
                    value = formatSessionDate(selectedDate),
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val calendar = parseSessionDate(selectedDate)
                        DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    selectedDate =
                                        "%04d-%02d-%02d".format(Locale.US, year, month + 1, day)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                            )
                            .show()
                    },
                )
                PickerField(
                    label = "Hora de inicio",
                    value = startTime,
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val parts = startTime.split(":")
                        TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    startTime = "%02d:%02d".format(Locale.US, hour, minute)
                                },
                                parts.getOrNull(0)?.toIntOrNull() ?: 16,
                                parts.getOrNull(1)?.toIntOrNull() ?: 0,
                                true,
                            )
                            .show()
                    },
                )
            }

            Spacer(Modifier.height(14.dp))
            PlannerInput(
                value = venue,
                onValueChange = { venue = it },
                label = "Lugar / Cancha",
                placeholder = "Cancha Principal Sintética",
                leadingIcon = Icons.Default.LocationOn,
            )
            Spacer(Modifier.height(14.dp))
            CategoryPicker(
                label = "Categoría",
                selected = category,
                options = categoryOptions,
                onSelect = { category = it },
            )
            Spacer(Modifier.height(14.dp))
            PlannerInput(
                value = objective,
                onValueChange = { objective = it },
                label = "Objetivo principal",
                placeholder = "Ej. Transición defensiva y presión alta",
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Ejercicios Agregados",
                    modifier = Modifier.weight(1f),
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = { showLibraryDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier =
                        Modifier.border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                            .background(CardBackground, RoundedCornerShape(12.dp)),
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = NeonGreen)
                    Text(" Biblioteca", color = NeonGreen, fontWeight = FontWeight.SemiBold)
                }
            }

            if (selectedExercises.isEmpty()) {
                Text(
                    "Agrega ejercicios manualmente o cárgalos desde tu biblioteca.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                selectedExercises.forEachIndexed { index, exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onRemove = { selectedExercises.removeAt(index) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            TextButton(
                onClick = { showExerciseDialog = true },
                modifier =
                    Modifier.fillMaxWidth()
                        .height(56.dp)
                        .border(1.5.dp, NeonGreen, RoundedCornerShape(14.dp)),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = NeonGreen)
                Text(
                    " Agregar ejercicio",
                    color = NeonGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            formError?.let {
                Text(
                    it,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Button(
                onClick = {
                    if (
                        venue.isBlank() ||
                            objective.isBlank() ||
                            category.isBlank() ||
                            selectedExercises.isEmpty()
                    ) {
                        formError =
                            "Completa lugar, categoría, objetivo y agrega al menos un ejercicio."
                    } else {
                        sessionRepository.saveSession(
                            date = selectedDate,
                            startTime = startTime,
                            venue = venue,
                            category = category,
                            objective = objective,
                            exercises = selectedExercises,
                        )
                        venue = ""
                        objective = ""
                        selectedExercises.clear()
                        formError = null
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = DarkBackground)
                Text(
                    "  Guardar Sesión",
                    color = DarkBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (sessionRepository.sessions.isNotEmpty()) {
                Text(
                    "Sesiones Guardadas",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 30.dp, bottom = 12.dp),
                )
                sessionRepository.sessions
                    .sortedByDescending { it.date + it.startTime }
                    .forEach { savedSession ->
                        SavedSessionCard(savedSession)
                        Spacer(Modifier.height(12.dp))
                    }
            }
        }
    }

    if (showExerciseDialog) {
        ExerciseEditorDialog(
            onDismiss = { showExerciseDialog = false },
            onSave = { exercise, saveToLibrary ->
                selectedExercises.add(exercise)
                if (saveToLibrary) sessionRepository.saveExerciseToLibrary(exercise)
                showExerciseDialog = false
            },
        )
    }

    if (showLibraryDialog) {
        ExerciseLibraryDialog(
            library = sessionRepository.exerciseLibrary,
            onDismiss = { showLibraryDialog = false },
            onAddExercise = { exercise ->
                selectedExercises.add(exercise)
                showLibraryDialog = false
            },
        )
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier = modifier) {
        Text(label, color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(CardBackground, RoundedCornerShape(15.dp))
                    .border(1.dp, InputBorder, RoundedCornerShape(15.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
            Text(
                value,
                color = TextWhite,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CategoryPicker(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors = plannerFieldColors(),
            )
            Box(Modifier.matchParentSize().clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: TrainingExercise, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise.name,
                    modifier = Modifier.weight(1f),
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${exercise.durationMinutes} min",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier =
                        Modifier.background(DarkBackground, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                )
                Text(
                    "Int. ${exercise.intensity}",
                    color = intensityColor(exercise.intensity),
                    fontSize = 12.sp,
                    modifier =
                        Modifier.padding(start = 6.dp)
                            .background(
                                intensityColor(exercise.intensity).copy(alpha = 0.12f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Quitar ejercicio",
                        tint = TextMuted,
                    )
                }
            }
            Text(
                exercise.instructions,
                color = TextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ExerciseEditorDialog(
    onDismiss: () -> Unit,
    onSave: (TrainingExercise, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf("Media") }
    var instructions by remember { mutableStateOf("") }
    var saveToLibrary by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Agregar ejercicio", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(max = 540.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlannerInput(name, { name = it }, "Nombre del ejercicio", "Ej. Rondo de posesión")
                PlannerInput(
                    duration,
                    { duration = it.filter(Char::isDigit).take(3) },
                    "Duración (minutos)",
                    "15",
                    keyboardType = KeyboardType.Number,
                )
                IntensityPicker(intensity = intensity, onSelect = { intensity = it })
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción / Instrucciones") },
                    minLines = 3,
                    colors = plannerFieldColors(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { saveToLibrary = !saveToLibrary },
                ) {
                    Checkbox(checked = saveToLibrary, onCheckedChange = { saveToLibrary = it })
                    Text("Guardar en Biblioteca de Ejercicios", color = TextWhite, fontSize = 13.sp)
                }
                error?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = duration.toIntOrNull()
                    if (
                        name.isBlank() || minutes == null || minutes <= 0 || instructions.isBlank()
                    ) {
                        error = "Completa el nombre, duración e instrucciones."
                    } else {
                        onSave(
                            TrainingExercise(name.trim(), minutes, intensity, instructions.trim()),
                            saveToLibrary,
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            ) {
                Text("Agregar", color = DarkBackground)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } },
    )
}

@Composable
private fun IntensityPicker(intensity: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = intensity,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            readOnly = true,
            label = { Text("Intensidad") },
            colors = plannerFieldColors(),
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            exerciseIntensities.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ExerciseLibraryDialog(
    library: List<TrainingExercise>,
    onDismiss: () -> Unit,
    onAddExercise: (TrainingExercise) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text("Biblioteca de Ejercicios", color = TextWhite, fontWeight = FontWeight.Bold)
        },
        text = {
            if (library.isEmpty()) {
                Text(
                    "La biblioteca está vacía. Puedes guardar ejercicios al agregarlos.",
                    color = TextMuted,
                )
            } else {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    library.forEach { exercise ->
                        Card(
                            modifier =
                                Modifier.fillMaxWidth().clickable { onAddExercise(exercise) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    exercise.name,
                                    color = TextWhite,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "${exercise.durationMinutes} min  •  Intensidad ${exercise.intensity}",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                Text(
                                    exercise.instructions,
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = NeonGreen) } },
    )
}

@Composable
private fun SavedSessionCard(session: TrainingSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${formatSessionDate(session.date)}  •  ${session.startTime}",
                color = NeonGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                session.objective,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp),
            )
            Text(
                "${session.category}  •  ${session.venue}",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "${session.exercises.size} ejercicios  •  ${session.totalDurationMinutes} min",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun PlannerInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column {
        Text(label, color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            singleLine = true,
            placeholder = { Text(placeholder, color = TextMuted) },
            leadingIcon =
                leadingIcon?.let { icon ->
                    { Icon(icon, contentDescription = null, tint = TextMuted) }
                },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(15.dp),
            colors = plannerFieldColors(),
        )
    }
}

@Composable
private fun plannerFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = NeonGreen,
        unfocusedBorderColor = InputBorder,
        focusedLabelColor = NeonGreen,
        unfocusedLabelColor = TextMuted,
        cursorColor = NeonGreen,
        focusedContainerColor = CardBackground,
        unfocusedContainerColor = CardBackground,
    )

private fun intensityColor(intensity: String) =
    when (intensity) {
        "Alta" -> ErrorRed
        "Baja" -> NeonGreen
        else -> androidx.compose.ui.graphics.Color(0xFFFFC107)
    }

private fun todayAsString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

private fun parseSessionDate(date: String): Calendar {
    val calendar = Calendar.getInstance()
    val parsed =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(date)
        } catch (_: java.text.ParseException) {
            null
        }
    if (parsed != null) calendar.time = parsed
    return calendar
}

private fun formatSessionDate(date: String): String {
    val parsed =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(date)
        } catch (_: java.text.ParseException) {
            null
        } ?: return date
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(parsed)
}
