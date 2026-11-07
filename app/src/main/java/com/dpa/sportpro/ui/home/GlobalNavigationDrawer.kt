package com.dpa.sportpro.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite

data class DrawerMenuOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@Composable
fun GlobalNavigationDrawer(
    userProfile: UserProfile,
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
    onLogoutClick: () -> Unit,
) {
    val options = drawerOptions(userProfile.role)
    ModalDrawerSheet(
        modifier = Modifier.fillMaxSize(),
        drawerContainerColor = DarkBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier.size(48.dp)
                                .background(CardBackground, CircleShape)
                                .border(1.5.dp, NeonGreen, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Datos del usuario", tint = NeonGreen)
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            listOf(userProfile.names, userProfile.lastNames)
                                .filter(String::isNotBlank)
                                .joinToString(" "),
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${userProfile.role.displayName} (${userProfile.role.code})",
                            color = NeonGreen,
                            fontSize = 12.sp,
                        )
                    }
                }
                Text(userProfile.email, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                Text(
                    if (userProfile.isVerified) "Perfil verificado" else "Perfil sin verificar",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = InputBorder,
            )
            Text(
                "MENÚ PRINCIPAL",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            )
            Column(
                modifier =
                    Modifier.weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
            ) {
                options.forEach { option ->
                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(option.title, maxLines = 2)
                                Text(
                                    option.subtitle,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    maxLines = 2,
                                )
                            }
                        },
                        icon = { Icon(option.icon, contentDescription = null) },
                        selected = selectedKey == option.key,
                        onClick = { onOptionSelected(option.key) },
                        colors =
                            NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = NeonGreen.copy(alpha = 0.16f),
                                selectedTextColor = NeonGreen,
                                selectedIconColor = NeonGreen,
                                unselectedTextColor = TextWhite,
                                unselectedIconColor = TextMuted,
                            ),
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = InputBorder,
            )
            NavigationDrawerItem(
                label = { Text("Cerrar sesión") },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                selected = false,
                onClick = onLogoutClick,
                modifier = Modifier.padding(horizontal = 12.dp),
                colors =
                    NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = ErrorRed,
                        unselectedIconColor = ErrorRed,
                    ),
            )
        }
    }
}

fun drawerOptions(role: UserRole): List<DrawerMenuOption> {
    fun option(key: String, title: String, subtitle: String, icon: ImageVector) =
        DrawerMenuOption(key, title, subtitle, icon)
    val community =
        option(
            "community",
            "Comunidad",
            "Publicaciones, comentarios y reacciones de la comunidad.",
            Icons.Default.Group,
        )
    val roleOptions =
        when (role) {
            UserRole.COACH ->
                listOf(
                    option("match-events", "Bitácora de Partido en Vivo", "Registra goles, tarjetas y eventos minuto a minuto.", Icons.Default.SportsSoccer),
                    option("player-profile", "Gestión de Plantilla", "Controla asistencias y consulta fichas técnicas.", Icons.Default.Group),
                    option("training", "Planificar Sesión", "Organiza objetivos, ejercicios y preparación táctica.", Icons.Default.SportsSoccer),
                    option("attendance", "Control de Asistencia", "Registra participación y puntualidad del equipo.", Icons.Default.Badge),
                    option("convocations", "Convocatorias", "Publica partidos y revisa disponibilidad del equipo.", Icons.Default.CalendarMonth),
                    option("lineup", "Alineación Táctica", "Arma el once titular y los suplentes.", Icons.Default.SportsSoccer),
                    option("event-catalog", "Catálogo de Eventos", "Configura acciones del partido por categoría.", Icons.Default.Badge),
                    option("statistics", "Estadísticas del equipo", "Resultados y rendimiento acumulado del torneo.", Icons.Default.SportsSoccer),
                    option("chronicle", "Resumen IA del partido", "Genera y revisa crónicas de partidos finalizados.", Icons.Default.AutoAwesome),
                    option("announcements", "Anuncios oficiales", "Publica cambios y avisos para tus categorías.", Icons.Default.Badge),
                    option("tryouts", "Tryouts abiertos", "Publica pruebas deportivas para nuevos talentos.", Icons.Default.SportsSoccer),
                )
            UserRole.PLAYER ->
                listOf(
                    option("live-feed", "Marcador y Eventos en Vivo", "Sigue el partido y sus eventos en tiempo real.", Icons.Default.SportsSoccer),
                    option("statistics", "Mis Estadísticas", "Consulta rendimiento, minutos y goles de temporada.", Icons.Default.SportsSoccer),
                    option("announcements", "Anuncios oficiales", "Consulta avisos de tus categorías suscritas.", Icons.Default.Badge),
                    option("convocations", "Convocatorias", "Confirma tu disponibilidad para los partidos.", Icons.Default.CalendarMonth),
                    option("monthly-fees", "Mis Mensualidades", "Consulta el estado de tus cuotas.", Icons.Default.CalendarMonth),
                    option("tryouts", "Tryouts abiertos", "Encuentra pruebas deportivas y postula a academias.", Icons.Default.SportsSoccer),
                )
            UserRole.PARENT ->
                listOf(
                    option("live-feed", "Marcador y Eventos en Vivo", "Sigue el partido de tu hijo en tiempo real.", Icons.Default.SportsSoccer),
                    option("statistics", "Estadísticas del jugador", "Consulta resultados y rendimiento acumulado.", Icons.Default.SportsSoccer),
                    option("announcements", "Anuncios oficiales", "Mantente al día con comunicados del club.", Icons.Default.Badge),
                    option("player-profile", "Mis Hijos Vinculados", "Consulta fichas deportivas de tus hijos vinculados.", Icons.Default.Group),
                    option("monthly-fees", "Autorizaciones y Cuotas", "Consulta mensualidades y autorizaciones vinculadas.", Icons.Default.Badge),
                    option("convocations", "Convocatorias", "Responde por los jugadores vinculados.", Icons.Default.CalendarMonth),
                )
            UserRole.ADMIN ->
                listOf(
                    option("moderation", "Moderación de contenido", "Revisa reportes y protege la comunidad.", Icons.Default.Security),
                    option("academies", "Configuración de Equipos", "Gestiona academias, categorías y directores técnicos.", Icons.Default.Settings),
                    option("player-profile", "Ficha Técnica de Jugadores", "Consulta datos deportivos y evolución física.", Icons.Default.Person),
                    option("monthly-fees", "Mensualidades", "Controla cuotas por jugador y periodo.", Icons.Default.CalendarMonth),
                    option("event-catalog", "Catálogo de Eventos", "Configura acciones del partido por categoría.", Icons.Default.Badge),
                    option("announcements", "Anuncios oficiales", "Publica comunicados para padres y jugadores.", Icons.Default.Badge),
                    option("tryouts", "Tryouts abiertos", "Publica pruebas deportivas para nuevos talentos.", Icons.Default.SportsSoccer),
                )
        }
    return listOf(community) + roleOptions
}
