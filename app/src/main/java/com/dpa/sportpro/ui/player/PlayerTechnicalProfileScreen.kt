package com.dpa.sportpro.ui.player

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.PlayerAttendanceRecord
import com.dpa.sportpro.data.model.PlayerTechnicalProfile
import com.dpa.sportpro.data.repository.AcademyRepository
import com.dpa.sportpro.data.repository.AttendanceRepository
import com.dpa.sportpro.data.repository.PlayerTechnicalProfileRepository
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite
import java.io.File
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val positions =
    listOf("Arquero", "Defensa central", "Lateral", "Mediocampista", "Extremo", "Delantero")
private val dominantFeet = listOf("Derecha (Diestro)", "Izquierda (Zurdo)", "Ambas (Ambidiestro)")
private val emergencyRelationships = listOf("Madre", "Padre", "Tutor", "Otro")

@Composable
fun PlayerTechnicalProfileScreen(
    viewerRole: UserRole,
    viewerName: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val academyRepository = remember(context) { AcademyRepository(context.applicationContext) }
    val repository =
        remember(context) { PlayerTechnicalProfileRepository(context.applicationContext) }
    val attendanceRepository =
        remember(context) { AttendanceRepository(context.applicationContext) }
    val playerAttendance =
        attendanceRepository.records
            .filter { it.playerId == "player_mateo" }
            .sortedByDescending { it.sessionDate }
    val profile = repository.profile
    val isAssignedCoach =
        viewerRole == UserRole.COACH &&
            academyRepository.academies.any { academy ->
                academy.categories.any { category ->
                    category.name.equals(profile.category, ignoreCase = true) &&
                        category.coachName.equals(viewerName.trim(), ignoreCase = true)
                }
            }
    val canViewPrivateData = viewerRole == UserRole.ADMIN || isAssignedCoach
    val canManageProfile = viewerRole == UserRole.ADMIN || isAssignedCoach

    var editing by remember { mutableStateOf(false) }
    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var dateOfBirth by remember(profile) { mutableStateOf(profile.dateOfBirth) }
    var primaryPosition by remember(profile) { mutableStateOf(profile.primaryPosition) }
    var secondaryPosition by remember(profile) { mutableStateOf(profile.secondaryPosition) }
    var dominantFoot by remember(profile) { mutableStateOf(profile.dominantFoot) }
    var heightText by remember(profile) { mutableStateOf(profile.heightMeters.toString()) }
    var weightText by remember(profile) { mutableStateOf(profile.weightKilograms.toString()) }
    var photoUri by remember(profile) { mutableStateOf(profile.profilePhotoUri) }
    var contactPhone by remember(profile) { mutableStateOf(profile.contactPhone) }
    var emergencyName by remember(profile) { mutableStateOf(profile.emergencyContactName) }
    var emergencyRelationship by remember(profile) { mutableStateOf(profile.emergencyRelationship) }
    var emergencyPhone by remember(profile) { mutableStateOf(profile.emergencyPhone) }
    var formError by remember { mutableStateOf<String?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }

    val galleryPicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
            uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    photoError =
                        "No se pudo conservar el acceso a la foto. Selecciónala de nuevo antes de guardar."
                }
                if (photoError == null) {
                    photoUri = uri.toString()
                    photoError = null
                }
            }
        }
    val cameraPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->
            if (bitmap != null) {
                try {
                    val photoFile = File(context.filesDir, "player_profile_photo.jpg")
                    photoFile.outputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                    }
                    photoUri = Uri.fromFile(photoFile).toString()
                    photoError = null
                } catch (_: IOException) {
                    photoError = "No se pudo guardar la foto tomada. Inténtalo nuevamente."
                }
            }
        }

    fun cancelEditing() {
        fullName = profile.fullName
        dateOfBirth = profile.dateOfBirth
        primaryPosition = profile.primaryPosition
        secondaryPosition = profile.secondaryPosition
        dominantFoot = profile.dominantFoot
        heightText = profile.heightMeters.toString()
        weightText = profile.weightKilograms.toString()
        photoUri = profile.profilePhotoUri
        contactPhone = profile.contactPhone
        emergencyName = profile.emergencyContactName
        emergencyRelationship = profile.emergencyRelationship
        emergencyPhone = profile.emergencyPhone
        formError = null
        photoError = null
        editing = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = if (editing) ::cancelEditing else onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite,
                    )
                }
                Text(
                    text = if (editing) "Editar ficha" else "Ficha del Jugador",
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    color = TextWhite,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (!editing && canManageProfile) {
                    TextButton(onClick = { editing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = NeonGreen)
                        Text(" Editar", color = NeonGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Column(
                modifier =
                    Modifier.weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PlayerIdentityCard(
                    name = if (editing) fullName else profile.fullName,
                    category = profile.category,
                    primaryPosition = if (editing) primaryPosition else profile.primaryPosition,
                    photoUri = if (editing) photoUri else profile.profilePhotoUri,
                )

                if (editing) {
                    ProfileEditor(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        dateOfBirth = dateOfBirth,
                        onDateOfBirthChange = { dateOfBirth = it },
                        primaryPosition = primaryPosition,
                        onPrimaryPositionChange = { primaryPosition = it },
                        secondaryPosition = secondaryPosition,
                        onSecondaryPositionChange = { secondaryPosition = it },
                        dominantFoot = dominantFoot,
                        onDominantFootChange = { dominantFoot = it },
                        heightText = heightText,
                        onHeightChange = { heightText = it },
                        weightText = weightText,
                        onWeightChange = { weightText = it },
                        contactPhone = contactPhone,
                        onContactPhoneChange = { contactPhone = it },
                        emergencyName = emergencyName,
                        onEmergencyNameChange = { emergencyName = it },
                        emergencyRelationship = emergencyRelationship,
                        onEmergencyRelationshipChange = { emergencyRelationship = it },
                        emergencyPhone = emergencyPhone,
                        onEmergencyPhoneChange = { emergencyPhone = it },
                        onChooseGallery = { galleryPicker.launch(arrayOf("image/*")) },
                        onTakePhoto = { cameraPicker.launch(null) },
                        photoError = photoError,
                    )
                    formError?.let { Text(it, color = ErrorRed, fontSize = 13.sp) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = ::cancelEditing,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 14.dp),
                        ) {
                            Text("Cancelar", color = TextMuted)
                        }
                        Button(
                            onClick = {
                                val height = heightText.toDoubleOrNull()
                                val weight = weightText.toDoubleOrNull()
                                if (
                                    fullName.isBlank() ||
                                        dateOfBirth.isBlank() ||
                                        primaryPosition.isBlank() ||
                                        secondaryPosition.isBlank() ||
                                        dominantFoot.isBlank() ||
                                        height == null ||
                                        height <= 0.0 ||
                                        weight == null ||
                                        weight <= 0.0 ||
                                        photoUri.isBlank() ||
                                        contactPhone.isBlank() ||
                                        emergencyName.isBlank() ||
                                        emergencyRelationship.isBlank() ||
                                        emergencyPhone.isBlank()
                                ) {
                                    formError =
                                        "Completa los campos, agrega una foto y revisa los datos físicos."
                                } else {
                                    repository.updateProfile(
                                        profile.copy(
                                            fullName = fullName.trim(),
                                            dateOfBirth = dateOfBirth,
                                            primaryPosition = primaryPosition,
                                            secondaryPosition = secondaryPosition,
                                            dominantFoot = dominantFoot,
                                            heightMeters = height,
                                            weightKilograms = weight,
                                            profilePhotoUri = photoUri,
                                            contactPhone = contactPhone.trim(),
                                            emergencyContactName = emergencyName.trim(),
                                            emergencyRelationship = emergencyRelationship,
                                            emergencyPhone = emergencyPhone.trim(),
                                        )
                                    )
                                    formError = null
                                    editing = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            contentPadding = PaddingValues(vertical = 14.dp),
                        ) {
                            Text(
                                "Guardar ficha",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    PhysicalSummary(profile)
                    InformationCard(title = "Datos Personales") {
                        InformationRow("Fecha de Nacimiento", formatBirthDate(profile.dateOfBirth))
                        InformationRow("Posición Secundaria", profile.secondaryPosition)
                        InformationRow("Pierna Hábil", profile.dominantFoot)
                    }
                    if (canViewPrivateData) {
                        InformationCard(title = "Contacto") {
                            InformationRow("Teléfono", profile.contactPhone)
                        }
                        InformationCard(title = "Apoderado / Emergencia") {
                            InformationRow("Nombre", profile.emergencyContactName)
                            InformationRow("Parentesco", profile.emergencyRelationship)
                            InformationRow("Teléfono Emergencia", profile.emergencyPhone)
                        }
                    } else {
                        PrivacyNotice()
                    }
                    PhysicalHistoryCard(profile)
                    AttendanceHistoryCard(playerAttendance)
                }
            }
        }
    }
}

@Composable
private fun PlayerIdentityCard(
    name: String,
    category: String,
    primaryPosition: String,
    photoUri: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(photoUri = photoUri, size = 76)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$primaryPosition  •  Categoría $category",
                    color = NeonGreen,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun PhysicalSummary(profile: PlayerTechnicalProfile) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Estatura", color = TextMuted, fontSize = 13.sp)
                Text(
                    "%.2f m".format(Locale.US, profile.heightMeters),
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    "Historial: ${profile.physicalHistory.size} registro(s)",
                    color = NeonGreen,
                    fontSize = 11.sp,
                )
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Peso", color = TextMuted, fontSize = 13.sp)
                Text(
                    "%.1f kg".format(Locale.US, profile.weightKilograms),
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    "Act: ${formatDate(profile.physicalHistory.lastOrNull()?.recordedAt)}",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun InformationCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun InformationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = TextWhite,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PrivacyNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonGreen)
            Text(
                "Por privacidad, los datos de contacto y emergencia solo son visibles para el DT de la categoría y el Administrador.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun PhysicalHistoryCard(profile: PlayerTechnicalProfile) {
    InformationCard(title = "Historial físico") {
        profile.physicalHistory
            .sortedByDescending { it.recordedAt }
            .forEach { measurement ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(19.dp),
                    )
                    Text(
                        "${formatDate(measurement.recordedAt)}  •  %.2f m  •  %.1f kg"
                            .format(
                                Locale.US,
                                measurement.heightMeters,
                                measurement.weightKilograms,
                            ),
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
    }
}

@Composable
private fun AttendanceHistoryCard(records: List<PlayerAttendanceRecord>) {
    InformationCard(title = "Historial de asistencia") {
        if (records.isEmpty()) {
            Text("Aún no hay asistencias registradas.", color = TextMuted, fontSize = 13.sp)
        } else {
            records.forEach { record ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "${formatSessionDate(record.sessionDate)}  •  ${record.status.label}",
                        color = NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (record.observation.isNotBlank()) {
                        Text(record.observation, color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditor(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    dateOfBirth: String,
    onDateOfBirthChange: (String) -> Unit,
    primaryPosition: String,
    onPrimaryPositionChange: (String) -> Unit,
    secondaryPosition: String,
    onSecondaryPositionChange: (String) -> Unit,
    dominantFoot: String,
    onDominantFootChange: (String) -> Unit,
    heightText: String,
    onHeightChange: (String) -> Unit,
    weightText: String,
    onWeightChange: (String) -> Unit,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit,
    emergencyName: String,
    onEmergencyNameChange: (String) -> Unit,
    emergencyRelationship: String,
    onEmergencyRelationshipChange: (String) -> Unit,
    emergencyPhone: String,
    onEmergencyPhoneChange: (String) -> Unit,
    onChooseGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    photoError: String?,
) {
    val context = LocalContext.current
    InformationCard(title = "Datos del jugador") {
        ProfileInput(fullName, onFullNameChange, "Nombre completo")
        Button(
            onClick = {
                val calendar = parseBirthDate(dateOfBirth)
                DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            onDateOfBirthChange(
                                "%04d-%02d-%02d".format(Locale.US, year, month + 1, day)
                            )
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                    )
                    .show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NeonGreen)
            Text("  Fecha de nacimiento: ${formatBirthDate(dateOfBirth)}", color = TextWhite)
        }
        SelectInput("Posición principal", primaryPosition, positions, onPrimaryPositionChange)
        SelectInput("Posición secundaria", secondaryPosition, positions, onSecondaryPositionChange)
        SelectInput("Pierna hábil", dominantFoot, dominantFeet, onDominantFootChange)
    }

    InformationCard(title = "Datos físicos") {
        ProfileInput(
            heightText,
            onHeightChange,
            "Estatura (m)",
            keyboardType = KeyboardType.Decimal,
        )
        ProfileInput(weightText, onWeightChange, "Peso (kg)", keyboardType = KeyboardType.Decimal)
    }

    InformationCard(title = "Foto de perfil") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                text = "Cámara",
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.weight(1f),
                onClick = onTakePhoto,
            )
            ActionButton(
                text = "Galería",
                icon = Icons.Default.AddAPhoto,
                modifier = Modifier.weight(1f),
                onClick = onChooseGallery,
            )
        }
        photoError?.let { Text(it, color = ErrorRed, fontSize = 12.sp) }
    }

    InformationCard(title = "Contacto") {
        ProfileInput(
            contactPhone,
            onContactPhoneChange,
            "Teléfono de contacto",
            keyboardType = KeyboardType.Phone,
        )
    }

    InformationCard(title = "Apoderado / Emergencia") {
        ProfileInput(emergencyName, onEmergencyNameChange, "Nombre del apoderado")
        SelectInput(
            "Parentesco",
            emergencyRelationship,
            emergencyRelationships,
            onEmergencyRelationshipChange,
        )
        ProfileInput(
            emergencyPhone,
            onEmergencyPhoneChange,
            "Teléfono de emergencia *",
            keyboardType = KeyboardType.Phone,
        )
    }
}

@Composable
private fun ProfileInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = profileFieldColors(),
    )
}

@Composable
private fun SelectInput(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            label = { Text(label) },
            readOnly = true,
            singleLine = true,
            colors = profileFieldColors(),
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
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

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
    ) {
        Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
        Text(" $text", color = TextWhite, maxLines = 1)
    }
}

@Composable
private fun ProfileAvatar(photoUri: String, size: Int) {
    val bitmap = loadProfileBitmap(photoUri)
    Box(
        modifier =
            Modifier.size(size.dp)
                .background(DarkBackground, CircleShape)
                .border(1.5.dp, NeonGreen, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto de perfil del jugador",
                modifier = Modifier.size(size.dp).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = "Foto de perfil no seleccionada",
                tint = NeonGreen,
                modifier = Modifier.size((size / 2).dp),
            )
        }
    }
}

@Composable
private fun loadProfileBitmap(photoUri: String): Bitmap? {
    val context = LocalContext.current
    return remember(photoUri) {
        if (photoUri.isBlank()) {
            null
        } else {
            val uri = Uri.parse(photoUri)
            try {
                when (uri.scheme) {
                    "file" -> BitmapFactory.decodeFile(uri.path)
                    "content" ->
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    else -> null
                }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }
    }
}

@Composable
private fun profileFieldColors() =
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

private fun parseBirthDate(date: String): Calendar {
    val calendar = Calendar.getInstance()
    val parsed =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(date)
        } catch (_: ParseException) {
            null
        }
    if (parsed != null) calendar.time = parsed
    return calendar
}

private fun formatBirthDate(date: String): String {
    val parsed =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(date)
        } catch (_: ParseException) {
            null
        } ?: return date
    return SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "PE")).format(parsed)
}

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return "Sin registros"
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(Date(timestamp))
}

private fun formatSessionDate(value: String): String {
    val parsed =
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)
        } catch (_: ParseException) {
            null
        } ?: return value
    return SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).format(parsed)
}
