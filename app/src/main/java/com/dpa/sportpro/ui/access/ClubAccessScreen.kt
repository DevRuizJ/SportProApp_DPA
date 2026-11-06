package com.dpa.sportpro.ui.access

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.data.repository.*
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.theme.*
import java.text.DateFormat

@Composable
fun ClubAccessScreen(onBack: () -> Unit, vm: ClubAccessViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(if(vm.user.role == UserRole.PARENT) "children" else "clubs") }
    var showTeams by rememberSaveable { mutableStateOf(false) }
    var form by rememberSaveable { mutableStateOf<String?>(null) }
    var decision by remember { mutableStateOf<Pair<Any, String>?>(null) }
    val admin = vm.isAdmin(s)
    val maySeeTeams = admin || s.own.any { it.clubId == s.selected?.id && it.status == "APPROVED" && it.role == "DT" }
    if(showTeams && s.selected != null && maySeeTeams) {
        TeamsScreen(checkNotNull(s.selected), admin, onBack = { showTeams = false })
        return
    }
    BackHandler(s.selected != null) { vm.back() }
    Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(onClick = { if(s.selected != null) vm.back() else onBack() }) { Text("Volver", color = NeonGreen) }
                Text(s.selected?.name ?: "Clubes y vinculaciones", color = TextWhite, fontSize = 24.sp)
                if (s.platform) Text("Administración de plataforma", color = NeonGreen)
                s.error?.let { Text(it, color = ErrorRed) }
                s.notice?.let { Text(it, color = NeonGreen) }
                if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = NeonGreen)
                AccessButton("Actualizar", !s.busy, vm::refresh)
            }
            if (s.selected == null) {
                item {
                    Row {
                        TextButton(onClick = { tab = "clubs" }) { Text("Clubes", color = NeonGreen) }
                        TextButton(onClick = { tab = "requests" }) { Text("Mis solicitudes", color = NeonGreen) }
                        if(vm.user.role == UserRole.PARENT) TextButton(onClick = { tab = "children" }) { Text("Mis hijos", color = NeonGreen) }
                    }
                }
                when(tab) {
                    "requests" -> {
                        if(s.own.isEmpty()) item { Text("Sin solicitudes. Selecciona un club para solicitar tu rol.", color = TextMuted) }
                        items(s.own, key = { it.id }) { r ->
                            AccessCard {
                                Text("${r.clubName} · ${roleLabel(r.role)}", color = TextWhite)
                                Text(statusLabel(r.status), color = NeonGreen)
                                if(r.reason.isNotBlank()) Text(r.reason, color = TextMuted)
                                Text(r.createdAt?.let { DateFormat.getDateTimeInstance().format(it) } ?: "Confirmando fecha", color = TextMuted)
                            }
                        }
                    }
                    "children" -> {
                        item { AccessButton("Vincular hijo", !s.busy) { form = "link" } }
                        if(s.children.isEmpty()) item { Text("No tienes vínculos. Solicita una invitación al administrador del club.", color = TextMuted) }
                        items(s.children, key = { it.id }) { child ->
                            AccessCard {
                                Text(child.playerName, color = TextWhite)
                                Text("${child.category} · ${child.clubName}", color = TextMuted)
                                Text(statusLabel(child.status), color = NeonGreen)
                                if(child.reason.isNotBlank()) Text(child.reason, color = TextMuted)
                                if(child.status == "APPROVED") AccessButton("Ver perfil deportivo", !s.busy) { vm.child(child) }
                            }
                        }
                    }
                    else -> {
                        if(s.platform) item { AccessButton("Crear club", !s.busy) { form = "club" } }
                        if(s.clubs.isEmpty() && !s.busy) item { Text("No hay clubes disponibles. La autoridad de plataforma debe crear el primer club.", color = TextMuted) }
                        items(s.clubs, key = { it.id }) { club ->
                            AccessCard {
                                Text(club.name, color = TextWhite)
                                Text(club.description, color = TextMuted)
                                val membership = s.own.firstOrNull { it.clubId == club.id }
                                membership?.let { Text("${roleLabel(it.role)} · ${statusLabel(it.status)}", color = NeonGreen) }
                                AccessButton(if(s.platform || membership?.let { it.status == "APPROVED" && it.role == "ADM" } == true) "Administrar club" else "Ver club", !s.busy) { vm.select(club) }
                            }
                        }
                        if(s.moreClubs) item { AccessButton("Cargar más clubes", !s.busy, vm::moreClubs) }
                    }
                }
            } else {
                val own = s.own.firstOrNull { it.clubId == s.selected?.id }
                item {
                    if(own == null) AccessButton("Solicitar rol en este club", !s.busy) { form = "role" }
                    else Text("${roleLabel(own.role)} · ${statusLabel(own.status)}", color = NeonGreen)
                    if(admin || own?.let { it.status == "APPROVED" && it.role == "DT" } == true) {
                        AccessButton(if(admin) "Categorías, equipos y planteles" else "Mis equipos", !s.busy) { showTeams = true }
                    }
                }
                if(admin) {
                    item { Text("Administración · Solicitudes", color = TextWhite, fontSize = 20.sp) }
                    if(s.requests.isEmpty()) item { Text("Sin solicitudes en este club.", color = TextMuted) }
                    items(s.requests, key = { "request_" + it.id }) { r ->
                        AccessCard {
                            Text("${r.name} · ${roleLabel(r.role)}", color = TextWhite)
                            Text(r.email, color = TextMuted)
                            Text("${r.clubName} · ${statusLabel(r.status)}", color = NeonGreen)
                            Text(r.createdAt?.let { DateFormat.getDateTimeInstance().format(it) } ?: "", color = TextMuted)
                            if(r.reason.isNotBlank()) Text(r.reason, color = TextMuted)
                            if(vm.mayDecide(r)) DecisionButtons(r.status, !s.busy) { decision = r to it }
                        }
                    }
                    if(s.moreRequests) item { AccessButton("Más solicitudes", !s.busy, vm::moreRequests) }
                    item {
                        Text("Fichas deportivas y vínculos familiares", color = TextWhite, fontSize = 20.sp)
                        AccessButton("Crear ficha de jugador sin cuenta", !s.busy) { form = "player" }
                    }
                    items(s.players, key = { "player_" + it.id }) { player ->
                        AccessCard {
                            Text("${player.name} · ${player.category}", color = TextWhite)
                            AccessButton("Invitar apoderado", !s.busy) { vm.invite(player) }
                        }
                    }
                    if(s.morePlayers) item { AccessButton("Más jugadores", !s.busy, vm::morePlayers) }
                    s.generatedCode?.let { code -> item {
                        AccessCard {
                            Text("Invitación familiar: válida 24 horas y para un solo uso. Compártela de forma privada con el apoderado verificado.", color = TextWhite)
                            SelectionContainer { Text(code, color = NeonGreen) }
                        }
                    } }
                    item { Text("Solicitudes familiares", color = TextWhite, fontSize = 20.sp) }
                    items(s.families, key = { "family_" + it.id }) { link ->
                        AccessCard {
                            Text("${link.parentName} → ${link.playerName}", color = TextWhite)
                            Text("${link.category} · ${statusLabel(link.status)}", color = TextMuted)
                            if(link.reason.isNotBlank()) Text(link.reason, color = TextMuted)
                            if(link.parentUid != vm.user.uid) DecisionButtons(link.status, !s.busy) { decision = link to it }
                        }
                    }
                    if(s.moreFamilies) item { AccessButton("Más vínculos", !s.busy, vm::moreFamilies) }
                }
            }
        }
    }
    form?.let { kind ->
        var first by rememberSaveable(kind) { mutableStateOf("") }
        var second by rememberSaveable(kind) { mutableStateOf("") }
        var role by rememberSaveable(kind) { mutableStateOf(vm.user.role.code) }
        val title = when(kind) {
            "club" -> "Crear club"; "player" -> "Ficha deportiva"; "link" -> "Vincular hijo"; else -> "Solicitar rol"
        }
        AlertDialog(onDismissRequest = { form = null }, containerColor = CardBackground,
            title = { Text(title, color = TextWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if(kind == "role") {
                        listOf("ADM", "DT", "JUG", "PAD").forEach { value ->
                            Row { RadioButton(selected = role == value, onClick = { role = value }); Text(roleLabel(value), color = TextWhite, modifier = Modifier.padding(top = 12.dp)) }
                        }
                        Text("ADM requiere aprobación de la autoridad de plataforma.", color = TextMuted)
                    } else {
                        OutlinedTextField(value = first, onValueChange = { if(it.length <= 100) first = it },
                            label = { Text(if(kind == "link") "Código de invitación" else "Nombre") }, singleLine = true)
                        if(kind != "link") OutlinedTextField(value = second, onValueChange = { if(it.length <= 500) second = it },
                            label = { Text(if(kind == "club") "Descripción" else "Categoría") })
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !s.busy && (kind == "role" || first.isNotBlank()), onClick = {
                    when(kind) {
                        "club" -> vm.createClub(first, second)
                        "player" -> vm.addPlayer(first, second)
                        "link" -> vm.inspect(first)
                        else -> vm.requestRole(role)
                    }; form = null
                }) { Text(if(kind == "link") "Buscar invitación" else "Enviar") }
            },
            dismissButton = { TextButton(onClick = { form = null }) { Text("Cancelar") } })
    }
    decision?.let { (item, status) ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { decision = null }, containerColor = CardBackground,
            title = { Text("Confirmar: ${statusLabel(status)}", color = TextWhite) },
            text = { Column {
                Text("Verifica la identidad y la relación con el club antes de aprobar. La decisión quedará registrada.", color = TextMuted)
                OutlinedTextField(value = reason, onValueChange = { if(it.length <= 500) reason = it },
                    label = { Text("Motivo obligatorio") })
            } },
            confirmButton = { TextButton(enabled = reason.isNotBlank() && !s.busy, onClick = {
                when(item) {
                    is AccessRequest -> vm.decide(item, status, reason)
                    is FamilyLink -> vm.decideFamily(item, status, reason)
                }; decision = null
            }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { decision = null }) { Text("Cancelar") } })
    }
    s.invitation?.let { invitation ->
        AlertDialog(onDismissRequest = vm::dismissInvitation, containerColor = CardBackground,
            title = { Text("Confirmar solicitud de vínculo", color = TextWhite) },
            text = { Text("${invitation.playerName}\n${invitation.category} · ${invitation.clubName}\nEl club debe verificar y aprobar tu relación familiar.", color = TextWhite) },
            confirmButton = { TextButton(enabled = !s.busy, onClick = vm::confirmInvitation) { Text("Solicitar vinculación") } },
            dismissButton = { TextButton(onClick = vm::dismissInvitation) { Text("Cancelar") } })
    }
    s.child?.let { child ->
        AlertDialog(onDismissRequest = vm::dismissChild, containerColor = CardBackground,
            title = { Text(child.name, color = TextWhite) },
            text = { Text("Categoría: ${child.category}", color = TextWhite) },
            confirmButton = { TextButton(onClick = vm::dismissChild) { Text("Cerrar") } })
    }
}

@Composable private fun AccessCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBackground)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}
@Composable private fun AccessButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled) { Text(text, color = NeonGreen) }
}
@Composable private fun DecisionButtons(status: String, enabled: Boolean, onDecision: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if(status == "PENDING") {
            AccessButton("Aprobar", enabled) { onDecision("APPROVED") }
            AccessButton("Rechazar", enabled) { onDecision("REJECTED") }
        } else if(status == "APPROVED") AccessButton("Revocar", enabled) { onDecision("REVOKED") }
    }
}
private fun roleLabel(role: String) = when(role) {
    "ADM" -> "Administrador de club"; "DT" -> "Entrenador"; "PAD" -> "Padre / apoderado"; else -> "Jugador"
}
private fun statusLabel(status: String) = when(status) {
    "PENDING" -> "Pendiente"; "APPROVED" -> "Aprobado"; "REJECTED" -> "Rechazado"; "REVOKED" -> "Revocado"; else -> status
}
