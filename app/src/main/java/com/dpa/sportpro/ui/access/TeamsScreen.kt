package com.dpa.sportpro.ui.access

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.data.repository.*
import com.dpa.sportpro.ui.theme.*

@Composable
fun TeamsScreen(club: ClubRecord, admin: Boolean, onBack: () -> Unit,
    vm: TeamsViewModel = viewModel(key = "teams-${club.id}")) {
    val s by vm.state.collectAsStateWithLifecycle()
    var create by rememberSaveable { mutableStateOf(false) }
    var coachPicker by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<RosterPlayer?>(null) }
    LaunchedEffect(club.id, admin) { vm.load(club.id, admin) }
    BackHandler { if(s.selected != null) vm.back() else onBack() }
    Surface(color = DarkBackground, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                TextButton(onClick = { if(s.selected != null) vm.back() else onBack() }) { Text("Volver", color = NeonGreen) }
                Text(s.selected?.name ?: if(admin) "Equipos de ${club.name}" else "Mis equipos", color = TextWhite, fontSize = 24.sp)
                if(s.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                s.error?.let { Text(it, color = ErrorRed) }
            }
            val team = s.selected
            if(team == null) {
                if(admin) item { OutlinedButton(enabled = !s.busy, onClick = { create = true }) { Text("Crear equipo y categoría", color = NeonGreen) } }
                if(s.teams.isEmpty() && !s.busy) item { Text("No hay equipos asignados.", color = TextMuted) }
                items(s.teams, key = { it.id }) { entry ->
                    Card(onClick = { vm.select(entry) }, colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(entry.name, color = TextWhite)
                            Text("${entry.category} · DT: ${entry.coachName}", color = TextMuted)
                        }
                    }
                }
                if(s.teams.size == 100) item { Text("Mostrando los primeros 100 equipos del club.", color = TextMuted) }
            } else {
                item {
                    Text("${team.category} · DT: ${team.coachName}", color = TextMuted)
                    if(admin) OutlinedButton(enabled = !s.busy, onClick = { coachPicker = true }) { Text("Asignar entrenador", color = NeonGreen) }
                    Text("Plantel", color = TextWhite, fontSize = 20.sp)
                }
                if(s.roster.none { it.active }) item { Text("No hay jugadores activos en este equipo.", color = TextMuted) }
                items(s.roster, key = { it.id }) { player ->
                    Column {
                        Text(player.name + if(player.active) "" else " · Retirado", color = TextWhite)
                        if(admin) TextButton(enabled = !s.busy, onClick = {
                            if(player.active) removing = player else vm.player(player.id, true)
                        }) { Text(if(player.active) "Retirar del plantel" else "Reincorporar", color = NeonGreen) }
                    }
                }
                if(admin) {
                    item { Text("Agregar ficha existente del club", color = TextWhite) }
                    items(s.players.filter { p -> s.roster.none { it.id == p.id } }, key = { "add-" + it.id }) { player ->
                        TextButton(enabled = !s.busy, onClick = { vm.player(player.id, true) }) {
                            Text("${player.name} · ${player.category}", color = NeonGreen)
                        }
                    }
                    item { Text("Las fichas se crean en la administración del club. Incorporar una ficha no crea ni duplica cuentas.", color = TextMuted) }
                }
            }
        }
    }
    if(create || coachPicker) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var chosen by remember { mutableStateOf<TeamCoach?>(null) }
        AlertDialog(onDismissRequest = { create = false; coachPicker = false }, containerColor = CardBackground,
            title = { Text(if(create) "Nuevo equipo" else "Asignar entrenador", color = TextWhite) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if(create) item {
                        OutlinedTextField(name, { if(it.length <= 80) name = it }, label = { Text("Nombre del equipo") })
                        OutlinedTextField(category, { if(it.length <= 80) category = it }, label = { Text("Categoría (Sub-12, Sub-15…)") })
                    }
                    item { Text("Entrenadores aprobados de este club", color = TextWhite) }
                    if(s.coaches.isEmpty()) item { Text("Aprueba primero una solicitud DT en este club.", color = TextMuted) }
                    items(s.coaches, key = { it.uid }) { coach ->
                        Row {
                            RadioButton(chosen?.uid == coach.uid, onClick = { chosen = coach })
                            Text(coach.name, color = TextWhite, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(enabled = !s.busy && chosen != null && (!create || name.isNotBlank() && category.isNotBlank()), onClick = {
                chosen?.let { if(create) vm.create(name, category, it) else vm.assign(it) }
                create = false; coachPicker = false
            }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { create = false; coachPicker = false }) { Text("Cancelar") } })
    }
    removing?.let { player ->
        AlertDialog(onDismissRequest = { removing = null }, containerColor = CardBackground,
            title = { Text("Retirar del plantel", color = TextWhite) },
            text = { Text("Se retirará a ${player.name} de este equipo. Su ficha e historial se conservarán.", color = TextWhite) },
            confirmButton = { TextButton(enabled = !s.busy, onClick = { vm.player(player.id, false); removing = null }) { Text("Retirar") } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancelar") } })
    }
}
