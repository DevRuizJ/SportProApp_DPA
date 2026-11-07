package com.dpa.sportpro.ui.livefeed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.data.model.EventType
import com.dpa.sportpro.data.model.MatchEvent
import com.dpa.sportpro.data.model.MatchEventStatus
import com.dpa.sportpro.data.model.ChronicleStatus
import com.dpa.sportpro.data.repository.MatchChronicleRepository
import com.dpa.sportpro.data.repository.MatchEventsRepositoryImpl
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite

@Composable
fun MatchLiveFeedScreen(
    viewModel: MatchLiveFeedViewModel = viewModel(),
    onBackClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val matchRepository = remember { MatchEventsRepositoryImpl.getInstance() }
    val currentMatch by matchRepository.currentMatch.collectAsState()
    val chronicleRepository = remember(context) { MatchChronicleRepository.getInstance(context) }
    val chronicles by chronicleRepository.chronicles.collectAsState()
    val publishedChronicle =
        currentMatch?.let { match ->
            chronicles.firstOrNull {
                it.matchId == match.id && it.status == ChronicleStatus.APPROVED
            }
        }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
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
                Text(
                    text = "Marcador en Vivo",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier.background(
                                        ErrorRed.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .border(
                                        1.dp,
                                        ErrorRed.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(8.dp).background(ErrorRed, CircleShape)
                                )
                                Text(
                                    text =
                                        if (uiState.matchStatus == com.dpa.sportpro.data.model.MatchStatus.FINISHED) {
                                            " FINALIZADO"
                                        } else {
                                            " EN VIVO"
                                        },
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }

                        Text(
                            text = uiState.matchDateText,
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = uiState.homeTeamName,
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        Text(
                            text = "${uiState.homeScore}  -  ${uiState.awayScore}",
                            color = TextWhite,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.awayTeamName,
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val minutes = uiState.timerSeconds / 60
                    val seconds = uiState.timerSeconds % 60
                    val timerFormatted =
                        java.lang.String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

                    Text(
                        text = "${uiState.matchPeriodText} • $timerFormatted",
                        color = NeonGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "SUCESOS RECIENTES",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(14.dp))

            uiState.events.forEach { event -> SpectatorEventCard(event = event) }

            publishedChronicle?.let { chronicle ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                ) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .border(1.dp, NeonGreen.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                    ) {
                        Text(
                            "CRÓNICA APROBADA POR EL DIRECTOR TÉCNICO",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            chronicle.narrative,
                            color = TextWhite,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        if (chronicle.technicalComment.isNotBlank()) {
                            Text(
                                "Análisis del Director Técnico",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            Text(
                                chronicle.technicalComment,
                                color = TextMuted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpectatorEventCard(event: MatchEvent) {
    val isAnnulled = event.status == MatchEventStatus.ANNULLED

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isAnnulled) CardBackground.copy(alpha = 0.4f) else CardBackground
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.background(DarkBackground, RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            if (isAnnulled) ErrorRed else InputBorder,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${event.minute}'",
                    color = if (isAnnulled) ErrorRed else NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val actionIcon: ImageVector =
                        when (event.eventType) {
                            EventType.GOAL -> Icons.Default.Cancel
                            EventType.YELLOW_CARD -> Icons.Default.Warning
                            EventType.RED_CARD -> Icons.Default.Warning
                            EventType.FOUL -> Icons.Default.Cancel
                            EventType.CORNER -> Icons.Default.Flag
                            EventType.SUBSTITUTION -> Icons.Default.SwapHoriz
                            else -> Icons.Default.Flag
                        }

                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = if (isAnnulled) ErrorRed else NeonGreen,
                        modifier = Modifier.size(18.dp),
                    )

                    val actionTitle =
                        if (isAnnulled) "[ANULADO] ${event.eventType.displayName}"
                        else event.eventType.displayName

                    Text(
                        text = actionTitle,
                        color = if (isAnnulled) ErrorRed else TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration =
                            if (isAnnulled) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val descriptionText = buildString {
                    when (event.eventType) {
                        EventType.GOAL -> append("Gol de ${event.playerName} (${event.teamName}).")
                        EventType.YELLOW_CARD ->
                            append("Tarjeta Amarilla a ${event.playerName} (${event.teamName}).")
                        EventType.RED_CARD ->
                            append("Tarjeta Roja a ${event.playerName} (${event.teamName}).")
                        EventType.FOUL ->
                            append("Falta de ${event.playerName} (${event.teamName}).")
                        EventType.CORNER ->
                            append("Tiro de esquina ejecutado por ${event.teamName}.")
                        EventType.SUBSTITUTION ->
                            append("Cambio en ${event.teamName}: ${event.playerName}.")
                        else ->
                            append(
                                "${event.eventType.displayName} para ${event.teamName} (${event.playerName})."
                            )
                    }
                    if (event.notes.isNotBlank()) {
                        append(" ${event.notes}")
                    }
                }

                Text(
                    text = descriptionText,
                    color = TextMuted,
                    fontSize = 13.sp,
                    textDecoration =
                        if (isAnnulled) TextDecoration.LineThrough else TextDecoration.None,
                )
            }
        }
    }
}
