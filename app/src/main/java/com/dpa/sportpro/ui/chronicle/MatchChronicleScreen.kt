package com.dpa.sportpro.ui.chronicle

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.dpa.sportpro.data.model.ChronicleStatus
import com.dpa.sportpro.data.model.MatchChronicle
import com.dpa.sportpro.data.model.MatchRecord
import com.dpa.sportpro.data.model.MatchStatus
import com.dpa.sportpro.data.repository.MatchChronicleEligibility
import com.dpa.sportpro.data.repository.MatchChronicleRepository
import com.dpa.sportpro.data.repository.MatchEventsRepositoryImpl
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
fun MatchChronicleScreen(viewerRole: UserRole, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val matchRepository = remember { MatchEventsRepositoryImpl.getInstance() }
    val chronicleRepository = remember(context) { MatchChronicleRepository.getInstance(context) }
    val matches by matchRepository.matches.collectAsState()
    val chronicles by chronicleRepository.chronicles.collectAsState()
    val latestFinished = matches.lastOrNull { it.status == MatchStatus.FINISHED }
    val latestChronicle =
        latestFinished?.let { match -> chronicles.firstOrNull { it.matchId == match.id } }
    val isEligible = MatchChronicleEligibility.canGenerate(latestFinished)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        "Resumen IA",
                        color = TextWhite,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Narrativa automatizada de partidos", color = TextMuted, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (viewerRole != UserRole.COACH) {
                ChronicleMessage("Esta función está disponible para el Director Técnico.")
            } else if (latestFinished == null) {
                ChronicleMessage("Aún no hay partidos finalizados para resumir.")
            } else {
                MatchSummaryCard(latestFinished)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        try {
                            chronicleRepository.generatePending(latestFinished)
                            errorMessage = null
                            infoMessage = "Crónica guardada como pendiente de aprobación."
                        } catch (error: IllegalArgumentException) {
                            errorMessage = error.message ?: "No se pudo generar la crónica."
                            infoMessage = null
                        }
                    },
                    enabled = isEligible && latestChronicle == null,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            disabledContainerColor = InputBorder,
                        ),
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isEligible) DarkBackground else TextMuted,
                    )
                    Text(
                        "Generar resumen del partido",
                        color = if (isEligible) DarkBackground else TextMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (!isEligible) {
                    Text(
                        "Para generar el resumen, el partido debe estar finalizado y registrar inicio y fin.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 9.dp),
                    )
                }
                errorMessage?.let {
                    Text(
                        it,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                infoMessage?.let {
                    Text(
                        it,
                        color = NeonGreen,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                latestChronicle?.let { chronicle ->
                    Spacer(modifier = Modifier.height(18.dp))
                    ChronicleReviewCard(
                        chronicle = chronicle,
                        canReview = viewerRole == UserRole.COACH &&
                            chronicle.status == ChronicleStatus.PENDING_APPROVAL,
                        onRejectAndRegenerate = {
                            try {
                                chronicleRepository.rejectAndRegenerate(latestFinished, viewerRole)
                                errorMessage = null
                                infoMessage = "El borrador se regeneró y sigue pendiente de aprobación."
                            } catch (error: IllegalArgumentException) {
                                errorMessage = error.message ?: "No se pudo regenerar la crónica."
                            }
                        },
                        onApprove = { narrative, technicalComment ->
                            try {
                                chronicleRepository.approveAndPublish(
                                    matchId = chronicle.matchId,
                                    narrative = narrative,
                                    technicalComment = technicalComment,
                                    reviewerRole = viewerRole,
                                )
                                errorMessage = null
                                infoMessage = "Crónica aprobada y publicada."
                            } catch (error: IllegalArgumentException) {
                                errorMessage = error.message ?: "No se pudo aprobar la crónica."
                            }
                        },
                    )
                }
                Text(
                    "La narrativa se genera localmente usando únicamente los eventos registrados; " +
                        "no se envían datos a un servicio externo.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun MatchSummaryCard(match: MatchRecord) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .border(1.dp, InputBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
    ) {
        Text(
            "ÚLTIMO PARTIDO REGISTRADO",
            color = NeonGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                match.homeTeamName,
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${matchScore(match, match.homeTeamName)}",
                color = NeonGreen,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                match.awayTeamName,
                color = TextMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${matchScore(match, match.awayTeamName)}",
                color = TextMuted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "Fecha: ${SimpleDateFormat("dd 'de' MMMM, yyyy", Locale.forLanguageTag("es-PE")).format(Date(match.finishedAtMillis ?: match.startedAtMillis))}",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (match.status != MatchStatus.FINISHED) {
            Text("Partido en curso", color = ErrorRed, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChronicleReviewCard(
    chronicle: MatchChronicle,
    canReview: Boolean,
    onRejectAndRegenerate: () -> Unit,
    onApprove: (String, String) -> Unit,
) {
    var narrative by remember(chronicle.id, chronicle.createdAtMillis) {
        mutableStateOf(chronicle.narrative)
    }
    var technicalComment by remember(chronicle.id, chronicle.createdAtMillis) {
        mutableStateOf(chronicle.technicalComment)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .border(1.dp, InputBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonGreen)
                Text(
                    if (canReview) "Revisar resumen" else "Resumen del partido",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(
                    if (chronicle.status == ChronicleStatus.PENDING_APPROVAL) "Borrador DT"
                    else "Aprobado",
                    color =
                        if (chronicle.status == ChronicleStatus.PENDING_APPROVAL) NeonGreen
                        else DarkBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier.clip(RoundedCornerShape(7.dp))
                            .background(
                                if (chronicle.status == ChronicleStatus.PENDING_APPROVAL) {
                                    NeonGreen.copy(alpha = 0.12f)
                                } else {
                                    NeonGreen
                                }
                            )
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                )
            }
            Text(
                if (canReview) "Narrativa generada (editable)" else "Narrativa publicada",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
            )
            OutlinedTextField(
                value = narrative,
                onValueChange = { narrative = it },
                enabled = canReview,
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                shape = RoundedCornerShape(13.dp),
                colors = chronicleFieldColors(),
            )
            if (canReview) {
                Text(
                    "Añadir comentario técnico",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                OutlinedTextField(
                    value = technicalComment,
                    onValueChange = { technicalComment = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Agrega observaciones tácticas para acompañar la crónica.", color = TextMuted)
                    },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = chronicleFieldColors(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onRejectAndRegenerate,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkBackground,
                            contentColor = ErrorRed,
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text("Rechazar / Regenerar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onApprove(narrative, technicalComment) },
                        enabled = narrative.isNotBlank(),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            disabledContainerColor = InputBorder,
                        ),
                    ) {
                        Text(
                            "Aprobar y Publicar",
                            color = if (narrative.isNotBlank()) DarkBackground else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                if (chronicle.technicalComment.isNotBlank()) {
                    Text(
                        "Análisis del Director Técnico",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        chronicle.technicalComment,
                        color = TextWhite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                Spacer(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = 14.dp)
                            .height(1.dp)
                            .background(InputBorder)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                Text(
                    "Publicado para la comunidad",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
                }
            }
        }
    }
}

@Composable
private fun chronicleFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        disabledTextColor = TextWhite,
        focusedBorderColor = NeonGreen,
        unfocusedBorderColor = InputBorder,
        disabledBorderColor = InputBorder,
        cursorColor = NeonGreen,
        focusedContainerColor = CardBackground,
        unfocusedContainerColor = CardBackground,
        disabledContainerColor = CardBackground,
    )

@Composable
private fun ChronicleMessage(message: String) {
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

private fun matchScore(match: MatchRecord, teamName: String): Int =
    match.events.count {
        it.eventType == com.dpa.sportpro.data.model.EventType.GOAL &&
            it.teamName == teamName &&
            it.status != com.dpa.sportpro.data.model.MatchEventStatus.ANNULLED
    }
