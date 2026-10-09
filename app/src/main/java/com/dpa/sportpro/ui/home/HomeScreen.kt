package com.dpa.sportpro.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun HomeScreen(
    userProfile: UserProfile,
    onAcademiesClick: () -> Unit = {},
    onPlayerProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(CardBackground, CircleShape)
                            .border(1.5.dp, NeonGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Hola, ${userProfile.names}",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Rol: ${userProfile.role.displayName} (${userProfile.role.code})",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(onClick = onLogoutClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        tint = ErrorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Panel de ${userProfile.role.displayName}",
                        color = NeonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = getRoleWelcomeMessage(userProfile.role),
                        color = TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Funciones Principales",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (userProfile.role) {
                UserRole.COACH -> {
                    FeatureCard(
                        "Gestión de Plantilla",
                        "Control de asistencias y ficha técnica",
                        Icons.Default.Group,
                        onClick = onPlayerProfileClick
                    )
                    FeatureCard("Planes de Entrenamiento", "Asignación de ejercicios y convocatorias", Icons.Default.SportsSoccer)
                    FeatureCard("Calendario de Partidos", "Programación de fechas y rivales", Icons.Default.CalendarMonth)
                }
                UserRole.PLAYER -> {
                    FeatureCard("Mis Estadísticas", "Rendimiento, minutos y goles de la temporada", Icons.Default.SportsSoccer)
                    FeatureCard("Próximos Entrenamientos", "Horarios y convocatorias asignadas", Icons.Default.CalendarMonth)
                    FeatureCard("Ficha Médica y Deportiva", "Registro de salud y evaluaciones", Icons.Default.Badge)
                }
                UserRole.PARENT -> {
                    FeatureCard(
                        "Mis Hijos Vinculados",
                        "Ficha deportiva de Mateo Silva Rossi",
                        Icons.Default.Group,
                        onClick = onPlayerProfileClick
                    )
                    FeatureCard("Autorizaciones y Cuotas", "Aprobaciones de viajes y estado de pagos", Icons.Default.Badge)
                    FeatureCard("Calendario Familiar", "Horarios de partidos y eventos", Icons.Default.CalendarMonth)
                }
                UserRole.ADMIN -> {
                    FeatureCard("Gestión Global de Usuarios", "Aprobación de cuentas y asignación de roles", Icons.Default.Group)
                    FeatureCard(
                        "Configuración de Equipos",
                        "Registro de academias, categorías y directores técnicos",
                        Icons.Default.Settings,
                        onClick = onAcademiesClick
                    )
                    FeatureCard(
                        "Ficha Técnica de Jugadores",
                        "Datos deportivos y evolución física",
                        Icons.Default.Person,
                        onClick = onPlayerProfileClick
                    )
                    FeatureCard("Reportes del Club", "Métricas financieras y deportivas", Icons.Default.Badge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardBackground,
                    contentColor = ErrorRed
                )
            ) {
                Text(
                    text = "Cerrar Sesión",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(DarkBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, InputBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

private fun getRoleWelcomeMessage(role: UserRole): String {
    return when (role) {
        UserRole.COACH -> "Bienvenido Director Técnico. Desde aquí puedes planificar entrenamientos, gestionar convocatorias y analizar el rendimiento de tus jugadores."
        UserRole.PLAYER -> "Bienvenido Jugador. Revisa tu calendario de entrenamientos, convocatorias para el fin de semana y tu progreso personal."
        UserRole.PARENT -> "Bienvenido. Aquí puedes hacer seguimiento del rendimiento de tu hijo, autorizar eventos y estar al tanto del calendario del equipo."
        UserRole.ADMIN -> "Bienvenido Administrador. Tienes acceso completo a la gestión de usuarios, roles, finanzas y configuración del club."
    }
}
