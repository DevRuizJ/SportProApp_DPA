package com.dpa.sportpro.ui.academy

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.ui.components.SportProInputField
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextPlaceholder
import com.dpa.sportpro.ui.theme.TextWhite

@Composable
fun AcademyScreen(
    viewModel: AcademyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isCoachDropdownExpanded by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Gestión de Academias",
                color = TextWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Registra clubes, define categorías y asigna Directores Técnicos.",
                color = TextMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Registrar Nueva Academia",
                        color = NeonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SportProInputField(
                        label = "Nombre de la Academia / Club",
                        value = uiState.academyName,
                        onValueChange = viewModel::onAcademyNameChanged,
                        placeholder = "Ej. SportPro FC",
                        errorMessage = uiState.nameError
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SportProInputField(
                        label = "URL Escudo (Cloud Storage)",
                        value = uiState.shieldUrl,
                        onValueChange = viewModel::onShieldUrlChanged,
                        placeholder = "https://cloud-storage.com/escudo.png"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SportProInputField(
                        label = "Sede Principal",
                        value = uiState.mainVenue,
                        onValueChange = viewModel::onMainVenueChanged,
                        placeholder = "Ej. Sede Central Lima Norte",
                        errorMessage = uiState.venueError
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SportProInputField(
                        label = "Descripción",
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChanged,
                        placeholder = "Descripción del club o academia..."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.registerAcademy() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text(
                            text = "Guardar Academia",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Academias y Categorías Registradas",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            uiState.academies.forEach { academy ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(DarkBackground, CircleShape)
                                        .border(1.dp, NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Domain,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = academy.name,
                                        color = TextWhite,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = academy.mainVenue,
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(start = 2.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.openAddCategoryDialog(academy) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar Categoría",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = " Categoría",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (academy.categories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Categorías y Directores Técnicos:",
                                color = TextMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            academy.categories.forEach { category ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .background(DarkBackground, RoundedCornerShape(10.dp))
                                        .border(1.dp, InputBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Group,
                                                contentDescription = null,
                                                tint = NeonGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = category.name,
                                                color = TextWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = category.coachName ?: "Sin DT Asignado",
                                                color = if (category.coachName != null) NeonGreen else TextMuted,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(start = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeAddCategoryDialog() },
            containerColor = CardBackground,
            title = {
                Text(
                    text = "Nueva Categoría (${uiState.selectedAcademyForCategory?.name})",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    SportProInputField(
                        label = "Nombre de Categoría",
                        value = uiState.newCategoryName,
                        onValueChange = viewModel::onNewCategoryNameChanged,
                        placeholder = "Ej. Sub-12, Sub-15, Primera",
                        errorMessage = uiState.categoryError
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Asignar Director Técnico (DT)",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(DarkBackground, RoundedCornerShape(12.dp))
                                .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                                .clickable { isCoachDropdownExpanded = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = uiState.selectedCoach?.let { "${it.names} ${it.lastNames}" } ?: "Seleccionar DT...",
                                color = if (uiState.selectedCoach != null) TextWhite else TextPlaceholder,
                                fontSize = 14.sp
                            )
                        }

                        DropdownMenu(
                            expanded = isCoachDropdownExpanded,
                            onDismissRequest = { isCoachDropdownExpanded = false },
                            modifier = Modifier.background(CardBackground)
                        ) {
                            uiState.availableCoaches.forEach { coach ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${coach.names} ${coach.lastNames} (${coach.email})",
                                            color = TextWhite
                                        )
                                    },
                                    onClick = {
                                        viewModel.onCoachSelected(coach)
                                        isCoachDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addCategoryToAcademy() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Asignar Categoría", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddCategoryDialog() }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    uiState.successMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessMessage() },
            containerColor = CardBackground,
            title = {
                Text("Operación Exitosa", color = NeonGreen, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(message, color = TextWhite)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSuccessMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("Aceptar", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
