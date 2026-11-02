package com.dpa.sportpro.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.ui.components.ErrorBanner
import com.dpa.sportpro.ui.components.SportProInputField
import com.dpa.sportpro.ui.theme.*

@Composable
fun AccountSecurityScreen(
    recovery: Boolean,
    onBack: () -> Unit,
    onSignInAgain: () -> Unit,
    currentEmail: String = "",
    viewModel: AccountSecurityViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(recovery) {
        if (recovery && state.action != AccountAction.RESET_PASSWORD) {
            viewModel.select(AccountAction.RESET_PASSWORD)
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier = Modifier.fillMaxSize().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = onBack, enabled = !state.submitting) {
                Text("Volver", color = NeonGreen)
            }
            Text(
                if (recovery) "Recuperar contraseña" else "Mi cuenta",
                color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            )
            if (!recovery) {
                Text(currentEmail, color = TextMuted)
                if (state.message == null) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { viewModel.select(AccountAction.CHANGE_PASSWORD) },
                            enabled = !state.submitting,
                        ) { Text("Contraseña", color = if (state.action == AccountAction.CHANGE_PASSWORD) NeonGreen else TextMuted) }
                        TextButton(
                            onClick = { viewModel.select(AccountAction.CHANGE_EMAIL) },
                            enabled = !state.submitting,
                        ) { Text("Correo", color = if (state.action == AccountAction.CHANGE_EMAIL) NeonGreen else TextMuted) }
                    }
                }
            }

            state.message?.let { message ->
                Text(message, color = TextWhite)
                Button(
                    onClick = if (recovery) onBack else onSignInAgain,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkBackground),
                ) { Text("Volver a iniciar sesión") }
            } ?: run {
                Text(
                    when (state.action) {
                        AccountAction.RESET_PASSWORD -> "Ingresa el correo de tu cuenta. Recibirás un enlace para elegir una nueva contraseña."
                        AccountAction.CHANGE_PASSWORD -> "Confirma tu contraseña actual y elige una nueva de al menos 8 caracteres, con una mayúscula y un número."
                        AccountAction.CHANGE_EMAIL -> "Verificaremos el nuevo correo antes de cambiarlo. Tu cuenta y tus datos se conservarán."
                    },
                    color = TextMuted,
                )
                state.error?.let { ErrorBanner(errorMessage = it) }
                if (state.action != AccountAction.RESET_PASSWORD) {
                    SportProInputField(
                        label = "Contraseña actual", value = state.currentPassword,
                        onValueChange = viewModel::currentPassword, placeholder = "Ingresa tu contraseña actual",
                        isPassword = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                }
                if (state.action == AccountAction.CHANGE_PASSWORD) {
                    SportProInputField(
                        label = "Nueva contraseña", value = state.newPassword,
                        onValueChange = viewModel::newPassword, placeholder = "Ingresa la nueva contraseña",
                        isPassword = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                    SportProInputField(
                        label = "Confirmar nueva contraseña", value = state.confirmation,
                        onValueChange = viewModel::confirmation, placeholder = "Repite la nueva contraseña",
                        isPassword = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )
                } else {
                    SportProInputField(
                        label = if (recovery) "Correo electrónico" else "Nuevo correo electrónico",
                        value = state.email, onValueChange = viewModel::email, placeholder = "ejemplo@correo.com",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                }
                Button(
                    onClick = viewModel::submit, enabled = !state.submitting,
                    modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkBackground),
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = DarkBackground)
                    } else Text(when (state.action) {
                        AccountAction.RESET_PASSWORD -> "Enviar enlace"
                        AccountAction.CHANGE_PASSWORD -> "Cambiar contraseña"
                        AccountAction.CHANGE_EMAIL -> "Verificar nuevo correo"
                    })
                }
            }
        }
    }
}
