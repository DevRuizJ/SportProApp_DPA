package com.dpa.sportpro.ui.academy

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.Academy
import com.dpa.sportpro.data.model.AcademyCategory
import com.dpa.sportpro.data.repository.AcademyRepository
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite

private data class CategoryDraft(val name: String = "", val coachName: String = "")

private val availableCoaches = listOf("Carlos Gómez")

@Composable
fun AcademiesScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { AcademyRepository(context.applicationContext) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextWhite,
                        )
                    }
                    Text(
                        text = "Academias",
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        color = TextWhite,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = {},
                        modifier =
                            Modifier.background(CardBackground, RoundedCornerShape(14.dp))
                                .size(48.dp),
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = TextWhite,
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Buscar academia o equipo...", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = academyTextFieldColors(),
                )

                Spacer(modifier = Modifier.height(22.dp))

                val filteredAcademies =
                    repository.academies.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.categories.any { category ->
                                category.name.contains(searchQuery, ignoreCase = true)
                            }
                    }
                if (filteredAcademies.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            if (searchQuery.isBlank()) "Aún no hay academias registradas"
                            else "No se encontraron academias",
                            color = TextWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (searchQuery.isBlank()) "Usa + para registrar la primera."
                            else "Prueba con otro nombre o categoría.",
                            color = TextMuted,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                } else {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        filteredAcademies.forEach { academy -> AcademyCard(academy) }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp),
                containerColor = NeonGreen,
                contentColor = DarkBackground,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar academia")
            }
        }
    }

    if (showCreateDialog) {
        CreateAcademyDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, crestUri, venue, description, categories ->
                repository.addAcademy(name, crestUri, venue, description, categories)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun AcademyCard(academy: Academy) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(62.dp)
                            .background(DarkBackground, RoundedCornerShape(16.dp))
                            .border(1.dp, InputBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription =
                            if (academy.crestUri.isNotBlank()) "Escudo seleccionado" else "Escudo",
                        tint = NeonGreen,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
                    Text(
                        academy.name,
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Sede Principal: ${academy.mainVenue}",
                        color = TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier =
                        Modifier.background(NeonGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${academy.categories.size} Categorías",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (academy.description.isNotBlank()) {
                Text(
                    academy.description,
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(InputBorder))
            Text(
                "CATEGORÍAS INSCRITAS",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            academy.categories.forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        category.name,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Director técnico",
                            tint = TextMuted,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            " DT: ${category.coachName}",
                            color = TextMuted,
                            modifier = Modifier.padding(start = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAcademyDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, List<AcademyCategory>) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var crestUri by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    val categories = remember { mutableStateListOf(CategoryDraft()) }
    val imagePicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) {
            uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    // The selected image remains available for the current session.
                }
                crestUri = uri.toString()
                formError = null
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = { Text("Registrar academia", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AcademyInput(name, { name = it }, "Nombre de la academia/club")
                AcademyInput(venue, { venue = it }, "Sede principal")
                AcademyInput(description, { description = it }, "Descripción", singleLine = false)

                OutlinedButtonLike(
                    text = if (crestUri.isBlank()) "Seleccionar escudo" else "Escudo seleccionado",
                    icon = Icons.Default.Image,
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                )
                Text(
                    "Modo de prueba: la imagen queda vinculada localmente. La carga a Cloud Storage requiere configurar Firebase.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Categorías y directores técnicos",
                        modifier = Modifier.weight(1f),
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(
                        onClick = { categories.add(CategoryDraft()) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonGreen)
                        Text("Agregar", color = NeonGreen)
                    }
                }

                categories.forEachIndexed { index, category ->
                    CategoryInput(
                        category = category,
                        canRemove = categories.size > 1,
                        onCategoryChange = { categories[index] = it },
                        onRemove = { categories.removeAt(index) },
                    )
                }

                formError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val completeCategories =
                        categories.map { AcademyCategory(it.name.trim(), it.coachName) }
                    val hasValidInput =
                        name.isNotBlank() &&
                            venue.isNotBlank() &&
                            description.isNotBlank() &&
                            crestUri.isNotBlank() &&
                            completeCategories.isNotEmpty() &&
                            completeCategories.all {
                                it.name.isNotBlank() && it.coachName.isNotBlank()
                            }
                    if (hasValidInput) {
                        onSave(name, crestUri, venue, description, completeCategories)
                    } else {
                        formError =
                            "Completa los datos, selecciona el escudo y asigna un DT a cada categoría."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            ) {
                Text("Guardar academia", color = DarkBackground)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) } },
    )
}

@Composable
private fun CategoryInput(
    category: CategoryDraft,
    canRemove: Boolean,
    onCategoryChange: (CategoryDraft) -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(DarkBackground, RoundedCornerShape(12.dp))
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = category.name,
                onValueChange = { onCategoryChange(category.copy(name = it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Categoría") },
                colors = academyTextFieldColors(),
            )
            if (canRemove) {
                TextButton(onClick = onRemove) { Text("Quitar", color = TextMuted) }
            }
        }
        Box {
            OutlinedButtonLike(
                text = category.coachName.ifBlank { "Seleccionar director técnico" },
                icon = Icons.Default.PersonAdd,
                onClick = { expanded = true },
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                availableCoaches.forEach { coach ->
                    DropdownMenuItem(
                        text = { Text(coach) },
                        onClick = {
                            onCategoryChange(category.copy(coachName = coach))
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AcademyInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        colors = academyTextFieldColors(),
    )
}

@Composable
private fun OutlinedButtonLike(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
        Text(
            text,
            color = TextWhite,
            modifier = Modifier.padding(start = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun academyTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        focusedBorderColor = NeonGreen,
        unfocusedBorderColor = InputBorder,
        focusedLabelColor = NeonGreen,
        unfocusedLabelColor = TextMuted,
        cursorColor = NeonGreen,
        focusedContainerColor = CardBackground,
        unfocusedContainerColor = CardBackground,
    )
