package com.dpa.sportpro.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.data.model.PlayerTournamentStatistics
import com.dpa.sportpro.data.model.TeamStatistics
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite

@Composable
fun MatchStatisticsScreen(
    onBackClick: () -> Unit,
    viewModel: MatchStatisticsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val selectedTeam = state.teams.firstOrNull { it.teamName == state.selectedTeamName }
    val selectedPlayers = state.players.filter { it.teamName == state.selectedTeamName }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextWhite,
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = "Estadísticas",
                        color = TextWhite,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(text = "Rendimiento del Plantel", color = TextMuted, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBackground)
                        .padding(4.dp)
            ) {
                StatisticsTabButton(
                    label = "EQUIPO",
                    selected = state.selectedTab == StatisticsTab.TEAM,
                    onClick = { viewModel.selectTab(StatisticsTab.TEAM) },
                    modifier = Modifier.weight(1f),
                )
                StatisticsTabButton(
                    label = "JUGADORES",
                    selected = state.selectedTab == StatisticsTab.PLAYERS,
                    onClick = { viewModel.selectTab(StatisticsTab.PLAYERS) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (state.finishedMatchesCount == 0) {
                EmptyStatistics()
            } else if (state.selectedTab == StatisticsTab.TEAM) {
                Text(
                    text = "RESUMEN DEL TORNEO",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.teams.forEach { team ->
                        TeamSelector(
                            team = team.teamName,
                            selected = team.teamName == state.selectedTeamName,
                            onClick = { viewModel.selectTeam(team.teamName) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                selectedTeam?.let { TeamSummary(team = it) }
                if (selectedPlayers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "LÍDERES DE RENDIMIENTO",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    selectedPlayers
                        .maxByOrNull { it.goals }
                        ?.let { PerformanceLeader(player = it, metric = "${it.goals} Goles") }
                    selectedPlayers
                        .maxByOrNull { it.assists }
                        ?.let { PerformanceLeader(player = it, metric = "${it.assists} Asist.") }
                    selectedPlayers
                        .maxByOrNull { it.minutesPlayed }
                        ?.let { PerformanceLeader(player = it, metric = "${it.minutesPlayed} Min") }
                }
            } else {
                Text(
                    text = "RENDIMIENTO INDIVIDUAL",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (selectedPlayers.isEmpty()) {
                    Text(
                        text = "No hay participaciones registradas para este equipo.",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                } else {
                    selectedPlayers.forEach { player ->
                        PlayerStatisticsCard(player = player)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                Text(
                    text =
                        "Los minutos consideran a los titulares registrados y los cambios " +
                            "anotados en la bitácora.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StatisticsTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = if (selected) NeonGreen else TextMuted,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) InputBorder else CardBackground)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun TeamSelector(team: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = team,
        color = if (selected) DarkBackground else TextWhite,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier.clip(RoundedCornerShape(18.dp))
                .background(if (selected) NeonGreen else CardBackground)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun TeamSummary(team: TeamStatistics) {
    Column {
        val metrics =
            listOf(
                "Jugados" to team.matchesPlayed,
                "Ganados" to team.matchesWon,
                "Empatados" to team.matchesDrawn,
                "Perdidos" to team.matchesLost,
                "Goles a favor" to team.goalsFor,
                "Goles en contra" to team.goalsAgainst,
                "Diferencia de gol" to team.goalDifference,
            )
        metrics.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { (label, value) ->
                    MetricCard(
                        label = label,
                        value =
                            if (label == "Diferencia de gol" && value > 0) "+$value" else "$value",
                        modifier = Modifier.weight(1f),
                        highlighted = label == "Diferencia de gol",
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .height(92.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(1.dp, InputBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = if (highlighted) NeonGreen else TextWhite,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun PlayerStatisticsCard(player: PlayerTournamentStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(38.dp), shape = CircleShape, color = InputBorder) {
                    BoxedPlayerIcon()
                }
                Text(
                    text = player.playerName,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerValue("PJ", player.matchesPlayed, Modifier.weight(1f))
                PlayerValue("Min", player.minutesPlayed, Modifier.weight(1f))
                PlayerValue("G", player.goals, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerValue("Asist.", player.assists, Modifier.weight(1f))
                PlayerValue("TA", player.yellowCards, Modifier.weight(1f))
                PlayerValue("TR", player.redCards, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PerformanceLeader(player: PlayerTournamentStatistics, metric: String) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardBackground)
                .border(1.dp, InputBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.SportsSoccer,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                player.playerName,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Jugador del plantel", color = TextMuted, fontSize = 12.sp)
        }
        Text(
            metric,
            color = NeonGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(NeonGreen.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun PlayerValue(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(10.dp))
                .background(DarkBackground)
                .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun BoxedPlayerIcon() {
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Icon(
            imageVector = Icons.Default.SportsSoccer,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun EmptyStatistics() {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.SportsSoccer,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(34.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Aún no hay partidos finalizados",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Las métricas se actualizarán al finalizar un partido.",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}
