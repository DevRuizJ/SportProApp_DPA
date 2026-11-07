package com.dpa.sportpro.ui.announcements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.dpa.sportpro.data.model.AcademyCategory
import com.dpa.sportpro.data.model.AnnouncementPriority
import com.dpa.sportpro.data.model.OfficialAnnouncement
import com.dpa.sportpro.data.model.ReportReason
import com.dpa.sportpro.data.model.ReportTargetType
import com.dpa.sportpro.data.repository.AcademyRepository
import com.dpa.sportpro.data.repository.AnnouncementRepository
import com.dpa.sportpro.data.repository.ModerationRepository
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
import java.util.Date
import java.util.Locale

private const val GENERAL_CATEGORY = "General"

@Composable
fun AnnouncementsScreen(
    viewerRole: UserRole,
    viewerId: String,
    viewerName: String,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val announcementRepository =
        remember(context) { AnnouncementRepository.getInstance(context.applicationContext) }
    val academyRepository = remember(context) { AcademyRepository.getInstance(context) }
    val moderation = remember(context) { ModerationRepository.getInstance(context) }
    val academies by academyRepository.academiesState.collectAsState()
    val announcements by announcementRepository.announcements.collectAsState()
    val reports by moderation.reports.collectAsState()
    val suspendedUsers by moderation.suspendedUserIds.collectAsState()
    val categories =
        remember(academies) {
            listOf(GENERAL_CATEGORY) +
                academies
                    .flatMap { academy -> academy.categories.map(AcademyCategory::name) }
                    .distinct()
        }
    val userKey = "${viewerRole.code}_${viewerName.trim().lowercase(Locale.ROOT)}"
    val isSuspended = viewerId in suspendedUsers
    val canPublish = (viewerRole == UserRole.COACH || viewerRole == UserRole.ADMIN) && !isSuspended
    var subscribedCategories by
        remember(userKey, categories) {
            mutableStateOf(announcementRepository.subscriptionsFor(userKey, categories))
        }
    var selectedCategory by remember(categories) { mutableStateOf(categories.first()) }
    var selectedPriority by remember { mutableStateOf(AnnouncementPriority.INFORMATIONAL) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var reportFeedback by remember { mutableStateOf<String?>(null) }
    var readVersion by remember { mutableIntStateOf(0) }
    val unreadIds =
        remember(announcements, readVersion, userKey, subscribedCategories) {
            announcementRepository.unreadIdsFor(userKey, subscribedCategories)
        }
    val visibleAnnouncements =
        if (canPublish) announcements
        else announcementRepository.announcementsFor(subscribedCategories)
    val announcementsForFeed =
        remember(visibleAnnouncements, reports, viewerRole) {
            if (viewerRole == UserRole.ADMIN) {
                visibleAnnouncements
            } else {
                visibleAnnouncements.filterNot {
                    moderation.isPreventivelyHidden(ReportTargetType.OFFICIAL_ANNOUNCEMENT, it.id)
                }
            }
        }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite,
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(
                        text = "Anuncios",
                        color = TextWhite,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Comunicados oficiales y avisos urgentes",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    text = "Rol: ${viewerRole.code}",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier.clip(RoundedCornerShape(18.dp))
                            .background(NeonGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isSuspended) {
                Text(
                    "Tu cuenta está suspendida y no puede publicar ni reportar contenido.",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            reportFeedback?.let {
                Text(
                    it,
                    color = NeonGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            if (canPublish) {
                PublishAnnouncementCard(
                    title = title,
                    onTitleChange = {
                        title = it
                        errorMessage = null
                    },
                    body = body,
                    onBodyChange = {
                        body = it
                        errorMessage = null
                    },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = {
                        selectedCategory = it
                        errorMessage = null
                    },
                    selectedPriority = selectedPriority,
                    onPrioritySelected = { selectedPriority = it },
                    errorMessage = errorMessage,
                    successMessage = successMessage,
                    onPublish = {
                        try {
                            announcementRepository.publish(
                                title = title,
                                body = body,
                                priority = selectedPriority,
                                category = selectedCategory,
                                senderId = viewerId,
                                senderName = viewerName,
                                senderRoleCode = viewerRole.code,
                            )
                            title = ""
                            body = ""
                            errorMessage = null
                            successMessage = "Anuncio publicado para $selectedCategory."
                        } catch (error: IllegalArgumentException) {
                            errorMessage = error.message ?: "No se pudo publicar el anuncio."
                            successMessage = null
                        }
                    },
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                SubscriptionCard(
                    categories = categories,
                    selectedCategories = subscribedCategories,
                    onCategoryToggle = { category ->
                        val updated =
                            if (category in subscribedCategories) {
                                subscribedCategories - category
                            } else {
                                subscribedCategories + category
                            }
                        subscribedCategories = updated
                        announcementRepository.saveSubscriptions(userKey, updated)
                    },
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text =
                        if (unreadIds.isEmpty()) "COMUNICADOS RECIBIDOS"
                        else "COMUNICADOS RECIBIDOS • ${unreadIds.size} SIN LEER",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (announcementsForFeed.isEmpty()) {
                EmptyAnnouncements(canPublish)
            } else {
                announcementsForFeed.forEach { announcement ->
                    AnnouncementCard(
                        announcement = announcement,
                        unread = announcement.id in unreadIds,
                        onClick = {
                            if (announcement.id in unreadIds) {
                                announcementRepository.markAsRead(userKey, announcement.id)
                                readVersion++
                            }
                        },
                        onReport = { reason ->
                            moderation.report(
                                targetType = ReportTargetType.OFFICIAL_ANNOUNCEMENT,
                                targetId = announcement.id,
                                targetOwnerId = announcement.senderId,
                                targetTitle = "${announcement.title}\n${announcement.body}",
                                reporterId = viewerId,
                                reporterName = viewerName,
                                reason = reason,
                            )
                            reportFeedback = "Anuncio reportado para revisión."
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PublishAnnouncementCard(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedPriority: AnnouncementPriority,
    onPrioritySelected: (AnnouncementPriority) -> Unit,
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
                .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = null, tint = NeonGreen)
            Text(
                text = "Nuevo Anuncio Oficial",
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        FieldLabel("Título del Anuncio")
        AnnouncementTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = "Ej. Cambio de horario de entrenamiento",
        )

        Spacer(modifier = Modifier.height(14.dp))
        FieldLabel("Cuerpo del Mensaje")
        AnnouncementTextField(
            value = body,
            onValueChange = onBodyChange,
            placeholder = "Escribe los detalles importantes aquí...",
            minLines = 3,
        )

        Spacer(modifier = Modifier.height(14.dp))
        FieldLabel("Prioridad")
        SelectionField(
            value = selectedPriority.label,
            options = AnnouncementPriority.entries.map { it.label },
            onSelect = { label ->
                AnnouncementPriority.entries
                    .firstOrNull { it.label == label }
                    ?.let(onPrioritySelected)
            },
        )

        Spacer(modifier = Modifier.height(14.dp))
        FieldLabel("Categoría Destino")
        SelectionField(
            value = selectedCategory,
            options = categories,
            onSelect = onCategorySelected,
        )

        errorMessage?.let {
            Text(
                text = it,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        successMessage?.let {
            Text(
                text = it,
                color = NeonGreen,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Button(
            onClick = onPublish,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = DarkBackground)
            Text(
                text = "Publicar Anuncio",
                color = DarkBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun SubscriptionCard(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, InputBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
    ) {
        Text("CANALES SUSCRITOS", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Selecciona las categorías de las que deseas recibir comunicados.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
        categories.forEach { category ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onCategoryToggle(category) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = category in selectedCategories,
                    onCheckedChange = { onCategoryToggle(category) },
                    colors = CheckboxDefaults.colors(checkedColor = NeonGreen),
                )
                Text(category, color = TextWhite, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: OfficialAnnouncement,
    unread: Boolean,
    onClick: () -> Unit,
    onReport: (ReportReason) -> Unit,
) {
    val urgent = announcement.priority == AnnouncementPriority.URGENT
    val accent = if (urgent) ErrorRed else NeonGreen
    Card(
        modifier =
            Modifier.fillMaxWidth()
                .then(if (unread) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .border(
                        1.dp,
                        if (unread) accent.copy(alpha = 0.55f) else InputBorder,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (unread) {
                    Box(modifier = Modifier.size(8.dp).background(accent, CircleShape))
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = announcement.priority.label.uppercase(Locale.ROOT),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier.clip(RoundedCornerShape(7.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                )
                Text(
                    text = announcement.category,
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 9.dp).weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    relativeTime(announcement.createdAtMillis),
                    color = TextMuted,
                    fontSize = 11.sp,
                )
                ReportActionMenu(onSubmitReport = onReport)
            }

            Text(
                text = announcement.title,
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 13.dp),
            )
            Text(
                text = announcement.body,
                color = TextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (unread) {
                Text(
                    text = "NO LEÍDO • Toca para marcar como leído",
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SelectionField(value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = value,
            color = TextWhite,
            fontSize = 14.sp,
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(DarkBackground)
                    .border(1.dp, InputBorder, RoundedCornerShape(11.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardBackground),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextWhite) },
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
private fun AnnouncementTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextMuted, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = InputBorder,
                cursorColor = NeonGreen,
                focusedContainerColor = DarkBackground,
                unfocusedContainerColor = DarkBackground,
            ),
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun EmptyAnnouncements(canPublish: Boolean) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(30.dp),
        )
        Text(
            text = if (canPublish) "Aún no hay anuncios" else "No hay anuncios en tus canales",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 9.dp),
        )
        Text(
            text =
                if (canPublish) "Los comunicados publicados aparecerán aquí."
                else "Los comunicados de tus categorías aparecerán aquí.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun relativeTime(timestamp: Long): String {
    val elapsed = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val minutes = elapsed / 60_000
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "Hace ${minutes} min"
        minutes < 1_440 -> "Hace ${minutes / 60} h"
        minutes < 2_880 -> "Ayer"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
