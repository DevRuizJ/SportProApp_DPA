package com.dpa.sportpro.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.components.RoleChip
import com.dpa.sportpro.ui.components.SportProInputField
import com.dpa.sportpro.ui.theme.CardBackground
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.ErrorRed
import com.dpa.sportpro.ui.theme.InputBorder
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextPlaceholder
import com.dpa.sportpro.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(viewModel: RegisterViewModel = viewModel(), onNavigateToLogin: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            // Header
            Text(
                text = "Crea tu Cuenta",
                color = TextWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Únete a la plataforma líder de gestión de fútbol base.",
                color = TextMuted,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Role selection
            Text(
                text = "Selecciona tu Rol",
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UserRole.entries.forEach { role ->
                    val isSelected = uiState.selectedRole == role
                    RoleChip(
                        role = role,
                        isSelected = isSelected,
                        onClick = { viewModel.onRoleSelected(role) },
                    )
                }
            }

            uiState.roleError?.let { error ->
                Text(
                    text = error,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            SportProInputField(
                label = "Nombres",
                value = uiState.names,
                onValueChange = viewModel::onNamesChanged,
                placeholder = "Ej. Mateo Silva",
                errorMessage = uiState.namesError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SportProInputField(
                label = "Apellidos",
                value = uiState.lastNames,
                onValueChange = viewModel::onLastNamesChanged,
                placeholder = "Ej. Rossi Castro",
                errorMessage = uiState.lastNamesError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SportProInputField(
                label = "Correo electrónico",
                value = uiState.email,
                onValueChange = viewModel::onEmailChanged,
                placeholder = "correo@ejemplo.com",
                errorMessage = uiState.emailError,
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )

            // Conditional Fecha de Nacimiento field for JUG & PAD
            if (uiState.selectedRole?.requiresBirthDate == true) {
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(
                        text = "Fecha de Nacimiento",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )

                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(56.dp)
                                .background(CardBackground, RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color =
                                        if (uiState.birthDateError != null) ErrorRed
                                        else InputBorder,
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = uiState.birthDate.ifEmpty { "12 de Abril, 2010" },
                                color =
                                    if (uiState.birthDate.isEmpty()) TextPlaceholder else TextWhite,
                                fontSize = 15.sp,
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Seleccionar fecha",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    if (uiState.birthDate.isNotEmpty() && uiState.isMinor) {
                        Text(
                            text =
                                "ℹ️ Registrado como menor de edad. Se habilitará vinculación con apoderado.",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        )
                    }

                    uiState.birthDateError?.let { error ->
                        Text(
                            text = error,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SportProInputField(
                label = "Contraseña",
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                placeholder = "Mínimo 8 caracteres",
                errorMessage = uiState.passwordError,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SportProInputField(
                label = "Confirmar contraseña",
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChanged,
                placeholder = "Repite tu contraseña",
                errorMessage = uiState.confirmPasswordError,
                isPassword = true,
                isPasswordVisible = isConfirmPasswordVisible,
                onTogglePasswordVisibility = {
                    isConfirmPasswordVisible = !isConfirmPasswordVisible
                },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Action Button: "Crear cuenta"
            Button(
                onClick = { viewModel.register() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = DarkBackground,
                    ),
            ) {
                Text(text = "Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "¿Ya tienes cuenta? ", color = TextMuted, fontSize = 14.sp)
                Text(
                    text = "Inicia sesión",
                    color = NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToLogin() },
                )
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.onBirthDateSelected(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar", color = NeonGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Registration Success Dialog
    if (uiState.registrationSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            containerColor = CardBackground,
            title = {
                Text(
                    text = "¡Cuenta Creada!",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            },
            text = {
                Text(
                    text = uiState.successMessage ?: "Tu cuenta ha sido registrada correctamente.",
                    color = TextWhite,
                    fontSize = 14.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissSuccessDialog()
                        onNavigateToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                ) {
                    Text("Continuar", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}
