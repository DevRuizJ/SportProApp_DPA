package com.dpa.sportpro.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.ContentReport
import com.dpa.sportpro.data.model.ReportStatus
import com.dpa.sportpro.data.model.ReportTargetType
import com.dpa.sportpro.data.repository.AnnouncementRepository
import com.dpa.sportpro.data.repository.CommunityRepository
import com.dpa.sportpro.data.repository.ModerationRepository
import com.dpa.sportpro.data.repository.TryoutRepository
import com.dpa.sportpro.model.UserRole
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

@Composable
fun ModerationScreen(viewerRole: UserRole, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val moderation = remember(context) { ModerationRepository.getInstance(context) }
    val community = remember(context) { CommunityRepository.getInstance(context) }
    val announcements = remember(context) { AnnouncementRepository.getInstance(context) }
    val tryouts = remember(context) { TryoutRepository.getInstance(context) }
    val reports by moderation.reports.collectAsState()
    val suspendedUsers by moderation.suspendedUserIds.collectAsState()
    val pendingReports =
        reports
            .filter { it.status == ReportStatus.PENDING }
            .sortedByDescending { it.createdAtMillis }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite,
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        "Moderación",
                        color = TextWhite,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Revisa los reportes de la comunidad", color = TextMuted, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (viewerRole != UserRole.ADMIN) {
                ModerationMessage("Solo Administradores pueden acceder a este panel.")
            } else {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NeonGreen.copy(alpha = 0.1f))
                            .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = NeonGreen,
                    )
                    Text(
                        "${pendingReports.size} reportes pendientes",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 9.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (pendingReports.isEmpty()) {
                    ModerationMessage("No hay reportes pendientes de revisión.")
                } else {
                    pendingReports.forEach { report ->
                        val targetReportCount =
                            pendingReports.count {
                                it.targetType == report.targetType && it.targetId == report.targetId
                            }
                        ReportReviewCard(
                            report = report,
                            targetReportCount = targetReportCount,
                            isSuspended = report.targetOwnerId in suspendedUsers,
                            onDismiss = {
                                moderationAction {
                                    moderation.dismissReport(report.id, viewerRole.code)
                                }
                            },
                            onDelete = {
                                moderationAction {
                                    when (report.targetType) {
                                        ReportTargetType.POST ->
                                            community.deletePost(report.targetId, viewerRole.code)
                                        ReportTargetType.COMMENT ->
                                            community.deleteComment(
                                                report.targetParentId,
                                                report.targetId,
                                                viewerRole.code,
                                            )
                                        ReportTargetType.TRYOUT ->
                                            tryouts.deleteAnnouncement(
                                                report.targetId,
                                                viewerRole.code,
                                            )
                                        ReportTargetType.OFFICIAL_ANNOUNCEMENT ->
                                            announcements.deleteAnnouncement(
                                                report.targetId,
                                                viewerRole.code,
                                            )
                                    }
                                }
                            },
                            onSuspend = {
                                moderationAction {
                                    moderation.suspendUser(report.targetOwnerId, viewerRole.code)
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Cuentas suspendidas en este dispositivo: ${suspendedUsers.size}",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ReportReviewCard(
    report: ContentReport,
    targetReportCount: Int,
    isSuspended: Boolean,
    onDismiss: () -> String?,
    onDelete: () -> String?,
    onSuspend: () -> String?,
) {
    var actionError by
        androidx.compose.runtime.remember(report.id) {
            androidx.compose.runtime.mutableStateOf<String?>(null)
        }
    var confirmSuspend by
        androidx.compose.runtime.remember(report.id) {
            androidx.compose.runtime.mutableStateOf(false)
        }
    var confirmDelete by
        androidx.compose.runtime.remember(report.id) {
            androidx.compose.runtime.mutableStateOf(false)
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .border(1.dp, InputBorder, RoundedCornerShape(16.dp))
                    .padding(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = ErrorRed)
                Text(
                    "${report.targetType.title} reportada",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(
                    SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
                        .format(Date(report.createdAtMillis)),
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
            Text(
                report.targetTitle,
                color = TextWhite,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            ReviewDetail("Motivo", report.reason.title)
            ReviewDetail("Reportado por", report.reporterName)
            ReviewDetail("ID de la cuenta", report.targetOwnerId)
            Text(
                if (targetReportCount >= ModerationRepository.HIDE_REPORT_THRESHOLD) {
                    "Oculto preventivamente · $targetReportCount reportes"
                } else {
                    "$targetReportCount reportes activos de este contenido"
                },
                color =
                    if (targetReportCount >= ModerationRepository.HIDE_REPORT_THRESHOLD) {
                        ErrorRed
                    } else {
                        TextMuted
                    },
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            actionError?.let {
                Text(
                    it,
                    color = ErrorRed,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { actionError = onDismiss() },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                    Text("Desestimar", color = NeonGreen, fontSize = 12.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TextWhite)
                    Text("Eliminar", color = TextWhite, fontSize = 12.sp)
                }
                Button(
                    onClick = { confirmSuspend = true },
                    enabled = !isSuspended,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = InputBorder,
                            disabledContainerColor = InputBorder,
                        ),
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = if (isSuspended) TextMuted else ErrorRed,
                    )
                    Text(
                        if (isSuspended) "Suspendida" else "Suspender",
                        color = if (isSuspended) TextMuted else TextWhite,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        if (confirmSuspend) {
            AlertDialog(
                onDismissRequest = { confirmSuspend = false },
                title = { Text("Suspender cuenta") },
                text = {
                    Text(
                        "La cuenta infractora no podrá publicar ni interactuar en este dispositivo."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            actionError = onSuspend()
                            confirmSuspend = false
                        }
                    ) {
                        Text("Suspender", color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmSuspend = false }) { Text("Cancelar") }
                },
            )
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Eliminar contenido") },
                text = { Text("Esta acción eliminará definitivamente el contenido reportado.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            actionError = onDelete()
                            confirmDelete = false
                        }
                    ) {
                        Text("Eliminar definitivamente", color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
                },
            )
        }
    }
}

private fun moderationAction(action: () -> Unit): String? =
    try {
        action()
        null
    } catch (error: IllegalArgumentException) {
        error.message ?: "No se pudo completar la acción."
    }

@Composable
private fun ReviewDetail(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
        Text("$label: ", color = TextMuted, fontSize = 12.sp)
        Text(value, color = TextWhite, fontSize = 12.sp)
    }
}

@Composable
private fun ModerationMessage(message: String) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, color = TextMuted, fontSize = 13.sp)
    }
}
