package com.dpa.sportpro.ui.tryouts

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.ReportReason
import com.dpa.sportpro.data.model.ReportTargetType
import com.dpa.sportpro.data.model.TryoutAnnouncement
import com.dpa.sportpro.data.repository.ModerationRepository
import com.dpa.sportpro.data.repository.TryoutRepository
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.moderation.ReportActionMenu
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TryoutsScreen(
    viewerRole: UserRole,
    viewerId: String,
    viewerName: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { TryoutRepository.getInstance(context) }
    val moderation = remember(context) { ModerationRepository.getInstance(context) }
    val announcements by repository.announcements.collectAsState()
    val reports by moderation.reports.collectAsState()
    val suspendedUsers by moderation.suspendedUserIds.collectAsState()
    val isSuspended = viewerId in suspendedUsers
    val canPublish = (viewerRole == UserRole.ADMIN || viewerRole == UserRole.COACH) && !isSuspended

    var showForm by remember { mutableStateOf(false) }
    var clubName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var positions by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var formSuccess by remember { mutableStateOf<String?>(null) }

    var filterCategory by remember { mutableStateOf("") }
    var filterPosition by remember { mutableStateOf("") }
    var filterDate by remember { mutableStateOf("") }
    var filterCity by remember { mutableStateOf("") }
    var contactError by remember { mutableStateOf<String?>(null) }
    var reportFeedback by remember { mutableStateOf<String?>(null) }
    val filtered =
        remember(announcements, reports, filterCategory, filterPosition, filterDate, filterCity) {
            repository.search(filterCategory, filterPosition, filterDate, filterCity).filterNot {
                moderation.isPreventivelyHidden(ReportTargetType.TRYOUT, it.id)
            }
        }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite,
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "Tryouts Abiertos",
                        color = TextWhite,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Oportunidades de captación de talento",
                        color = TextMuted,
                        fontSize = 14.sp,
                    )
                }
            }

            if (isSuspended) {
                Text(
                    "Tu cuenta está suspendida y no puede publicar contenido.",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (canPublish) {
                Spacer(modifier = Modifier.height(20.dp))
                PublisherBanner(
                    expanded = showForm,
                    onClick = {
                        showForm = !showForm
                        formError = null
                        formSuccess = null
                    },
                )
                if (showForm) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TryoutForm(
                        clubName = clubName,
                        onClubNameChange = { clubName = it },
                        category = category,
                        onCategoryChange = { category = it },
                        positions = positions,
                        onPositionsChange = { positions = it },
                        date = date,
                        onDateChange = { date = it },
                        time = time,
                        onTimeChange = { time = it },
                        address = address,
                        onAddressChange = { address = it },
                        city = city,
                        onCityChange = { city = it },
                        requirements = requirements,
                        onRequirementsChange = { requirements = it },
                        contactEmail = contactEmail,
                        onContactEmailChange = { contactEmail = it },
                        contactPhone = contactPhone,
                        onContactPhoneChange = { contactPhone = it },
                        errorMessage = formError,
                        successMessage = formSuccess,
                        onPublish = {
                            try {
                                repository.publish(
                                    clubName = clubName,
                                    category = category,
                                    positions = positions.split(",", "\n"),
                                    date = date,
                                    time = time,
                                    address = address,
                                    city = city,
                                    requirements = requirements,
                                    contactEmail = contactEmail,
                                    contactPhone = contactPhone,
                                    publisherId = viewerId,
                                    publisherName = viewerName,
                                    publisherRole = viewerRole.code,
                                )
                                clubName = ""
                                category = ""
                                positions = ""
                                date = ""
                                time = ""
                                address = ""
                                city = ""
                                requirements = ""
                                contactEmail = ""
                                contactPhone = ""
                                formError = null
                                formSuccess = "Convocatoria publicada."
                            } catch (error: IllegalArgumentException) {
                                formError = error.message ?: "Revisa los datos de la convocatoria."
                                formSuccess = null
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Filtros de búsqueda",
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            SearchField(
                value = filterCategory,
                onValueChange = { filterCategory = it },
                placeholder = "Categoría / edad (ej. Sub-14)",
                modifier = Modifier.padding(top = 9.dp),
            )
            SearchField(
                value = filterPosition,
                onValueChange = { filterPosition = it },
                placeholder = "Posición de juego (ej. Delantero)",
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchDateField(
                    value = filterDate,
                    onValueChange = { filterDate = it },
                    context = context,
                    modifier = Modifier.weight(1f),
                )
                SearchField(
                    value = filterCity,
                    onValueChange = { filterCity = it },
                    placeholder = "Ciudad/distrito",
                    modifier = Modifier.weight(1f),
                )
            }
            if (
                filterCategory.isNotBlank() ||
                    filterPosition.isNotBlank() ||
                    filterDate.isNotBlank() ||
                    filterCity.isNotBlank()
            ) {
                TextButton(
                    onClick = {
                        filterCategory = ""
                        filterPosition = ""
                        filterDate = ""
                        filterCity = ""
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Limpiar filtros", color = NeonGreen, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            reportFeedback?.let {
                Text(
                    it,
                    color = NeonGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            if (filtered.isEmpty()) {
                EmptyTryouts(
                    if (announcements.isEmpty()) {
                        "Aún no hay convocatorias abiertas."
                    } else if (
                        announcements.none {
                            !moderation.isPreventivelyHidden(ReportTargetType.TRYOUT, it.id)
                        }
                    ) {
                        "Las convocatorias reportadas están ocultas mientras se revisan."
                    } else {
                        "No hay pruebas que coincidan con esos filtros."
                    }
                )
            } else {
                filtered.forEach { announcement ->
                    TryoutCard(
                        announcement = announcement,
                        onContact = { channel ->
                            contactError = openContactChannel(context, announcement, channel)
                        },
                        onReport = { reason ->
                            moderation.report(
                                targetType = ReportTargetType.TRYOUT,
                                targetId = announcement.id,
                                targetOwnerId = announcement.publisherId,
                                targetTitle =
                                    "Club: ${announcement.clubName}\n" +
                                        "Categoría: ${announcement.category}\n" +
                                        "Posiciones: ${announcement.positions.joinToString()}\n" +
                                        "Fecha: ${announcement.date} ${announcement.time}\n" +
                                        "Lugar: ${announcement.address}, ${announcement.city}\n" +
                                        "Requisitos: ${announcement.requirements}",
                                reporterId = viewerId,
                                reporterName = viewerName,
                                reason = reason,
                            )
                            reportFeedback = "Convocatoria reportada para revisión."
                        },
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
            contactError?.let {
                Text(
                    it,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun PublisherBanner(expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .clickable(onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen)
        Text(
            text = "Vista de publicación: Convocatoria",
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        Text(
            text = if (expanded) "Cerrar" else "Crear",
            color = DarkBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.clip(RoundedCornerShape(18.dp))
                    .background(NeonGreen)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun TryoutForm(
    clubName: String,
    onClubNameChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    positions: String,
    onPositionsChange: (String) -> Unit,
    date: String,
    onDateChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    requirements: String,
    onRequirementsChange: (String) -> Unit,
    contactEmail: String,
    onContactEmailChange: (String) -> Unit,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit,
    errorMessage: String?,
    successMessage: String?,
    onPublish: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .border(1.dp, NeonGreen, RoundedCornerShape(18.dp))
                .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        FormField("Club organizador", clubName, onClubNameChange, "Nombre de academia o club")
        FormField("Categoría / edad requerida", category, onCategoryChange, "Ej. 2012 / Sub-14")
        FormField(
            "Posiciones buscadas",
            positions,
            onPositionsChange,
            "Separadas por coma. Ej. Delantero, Volante",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatePickerField(
                label = "Fecha de prueba",
                value = date,
                onValueChange = onDateChange,
                modifier = Modifier.weight(1f),
            )
            FormField("Hora", time, onTimeChange, "HH:mm", modifier = Modifier.weight(1f))
        }
        FormField("Dirección exacta", address, onAddressChange, "Calle, número, sede o referencia")
        FormField("Ciudad / distrito", city, onCityChange, "Ej. Chorrillos, Lima")
        FormField(
            "Requisitos",
            requirements,
            onRequirementsChange,
            "Documentos, indumentaria y condiciones",
            minLines = 2,
        )
        FormField(
            "Correo de contacto (opcional)",
            contactEmail,
            onContactEmailChange,
            "captacion@academia.pe",
        )
        FormField(
            "Teléfono / WhatsApp (opcional)",
            contactPhone,
            onContactPhoneChange,
            "+51 987 654 321",
        )
        Text(
            "Agrega al menos un canal de contacto para recibir postulaciones.",
            color = TextMuted,
            fontSize = 11.sp,
        )
        errorMessage?.let { Text(it, color = ErrorRed, fontSize = 12.sp) }
        successMessage?.let { Text(it, color = NeonGreen, fontSize = 12.sp) }
        Button(
            onClick = onPublish,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = DarkBackground)
            Text(
                "Publicar convocatoria",
                color = DarkBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun TryoutCard(
    announcement: TryoutAnnouncement,
    onContact: (ContactChannel) -> Unit,
    onReport: (ReportReason) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .border(1.dp, InputBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(InputBorder),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeonGreen)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        announcement.clubName,
                        color = TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "${announcement.city} • ${announcement.address}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 3.dp),
                        )
                    }
                }
                Text(
                    announcement.category,
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                            .background(NeonGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 9.dp, vertical = 7.dp),
                )
                ReportActionMenu(onSubmitReport = onReport)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            InfoRow("Posición:", announcement.positions.joinToString(", "))
            InfoRow("Fecha:", "${displayDate(announcement.date)} — ${announcement.time}")
            InfoRow("Dirección:", "${announcement.address}, ${announcement.city}")
            InfoRow("Requisitos:", announcement.requirements)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (announcement.contactPhone.isNotBlank()) {
                    ContactButton(
                        title = "WhatsApp",
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = DarkBackground,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { onContact(ContactChannel.WHATSAPP) },
                    )
                    ContactButton(
                        title = "Llamar",
                        icon = {
                            Icon(
                                Icons.Default.SportsSoccer,
                                contentDescription = null,
                                tint = DarkBackground,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { onContact(ContactChannel.PHONE) },
                    )
                }
                if (announcement.contactEmail.isNotBlank()) {
                    ContactButton(
                        title =
                            if (announcement.contactPhone.isBlank()) "Contactar academia"
                            else "Correo",
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = DarkBackground,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { onContact(ContactChannel.EMAIL) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactButton(
    title: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(13.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
    ) {
        icon()
        Text(
            title,
            color = DarkBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(
            value,
            color = TextWhite,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextMuted, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            shape = RoundedCornerShape(11.dp),
            colors = fieldColors(),
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 12.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
    )
}

@Composable
private fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Text(
            label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Seleccionar", color = TextMuted, fontSize = 13.sp) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker(context, value, onValueChange) }) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Elegir fecha",
                        tint = NeonGreen,
                    )
                }
            },
            modifier =
                Modifier.fillMaxWidth().clickable { showDatePicker(context, value, onValueChange) },
            singleLine = true,
            shape = RoundedCornerShape(11.dp),
            colors = fieldColors(),
        )
    }
}

@Composable
private fun SearchDateField(
    value: String,
    onValueChange: (String) -> Unit,
    context: Context,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        placeholder = { Text("Fecha", color = TextMuted, fontSize = 12.sp) },
        leadingIcon = {
            IconButton(
                onClick = { showDatePicker(context, value, onValueChange, allowPast = true) }
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Filtrar por fecha",
                    tint = TextMuted,
                )
            }
        },
        modifier =
            modifier.clickable { showDatePicker(context, value, onValueChange, allowPast = true) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
    )
}

@Composable
private fun HorizontalDivider() {
    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(InputBorder))
}

@Composable
private fun EmptyTryouts(message: String) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = NeonGreen)
        Text(message, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 9.dp))
    }
}

@Composable
private fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = NeonGreen,
        unfocusedBorderColor = InputBorder,
        cursorColor = NeonGreen,
        focusedContainerColor = DarkBackground,
        unfocusedContainerColor = DarkBackground,
    )

private enum class ContactChannel {
    WHATSAPP,
    PHONE,
    EMAIL,
}

private fun openContactChannel(
    context: Context,
    announcement: TryoutAnnouncement,
    channel: ContactChannel,
): String? {
    val intent =
        when (channel) {
            ContactChannel.WHATSAPP -> {
                val phone = announcement.contactPhone.filter(Char::isDigit)
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://wa.me/$phone?text=" +
                            Uri.encode(
                                "Hola, estoy interesado en la prueba de ${announcement.clubName}."
                            )
                    ),
                )
            }
            ContactChannel.PHONE ->
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:${Uri.encode(announcement.contactPhone)}"),
                )
            ContactChannel.EMAIL ->
                Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:${Uri.encode(announcement.contactEmail)}"),
                    )
                    .putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Postulación a prueba deportiva - ${announcement.clubName}",
                    )
        }
    return try {
        context.startActivity(intent)
        null
    } catch (_: ActivityNotFoundException) {
        "No hay una aplicación disponible para abrir ese canal de contacto."
    }
}

private fun showDatePicker(
    context: Context,
    currentValue: String,
    onDateSelected: (String) -> Unit,
    allowPast: Boolean = false,
) {
    val calendar = Calendar.getInstance().apply { parseIsoDate(currentValue)?.let { time = it } }
    DatePickerDialog(
            context,
            { _, year, month, day ->
                onDateSelected(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        )
        .apply {
            if (!allowPast) {
                datePicker.minDate =
                    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            }
        }
        .show()
}

private fun parseIsoDate(value: String): Date? {
    if (value.isBlank()) return null
    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
    return try {
        parser.parse(value)
    } catch (_: java.text.ParseException) {
        null
    }
}

private fun displayDate(value: String): String {
    val date = parseIsoDate(value) ?: return value
    return SimpleDateFormat("dd 'de' MMMM", Locale.forLanguageTag("es-PE")).format(date)
}
