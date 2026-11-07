package com.dpa.sportpro.ui.moderation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dpa.sportpro.data.model.ReportReason
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextWhite

@Composable
fun ReportActionMenu(onSubmitReport: (ReportReason) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var dialogVisible by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf(ReportReason.ABUSIVE_CONDUCT) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    IconButton(onClick = { menuExpanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Opciones de contenido", tint = TextWhite)
    }
    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
        DropdownMenuItem(
            text = { Text("Reportar contenido") },
            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = ErrorRed) },
            onClick = {
                menuExpanded = false
                errorMessage = null
                dialogVisible = true
            },
        )
    }

    if (dialogVisible) {
        AlertDialog(
            onDismissRequest = { dialogVisible = false },
            title = { Text("Reportar contenido") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text("Selecciona el motivo del reporte.")
                    ReportReason.entries.forEach { reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                            )
                            Text(reason.title)
                        }
                    }
                    errorMessage?.let { Text(it, color = ErrorRed) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            onSubmitReport(selectedReason)
                            dialogVisible = false
                        } catch (error: IllegalArgumentException) {
                            errorMessage = error.message ?: "No se pudo enviar el reporte."
                        }
                    }
                ) {
                    Text("Enviar reporte", color = NeonGreen)
                }
            },
            dismissButton = { TextButton(onClick = { dialogVisible = false }) { Text("Cancelar") } },
        )
    }
}
