package com.dpa.sportpro.ui.login

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.ui.components.BrandHeader
import com.dpa.sportpro.ui.components.ErrorBanner
import com.dpa.sportpro.ui.components.SportProInputField
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateToRegister: () -> Unit = {},
    onNavigateToHome: (UserProfile) -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.authenticatedUser) {
        uiState.authenticatedUser?.let { user ->
            onNavigateToHome(user)
            viewModel.clearAuthenticationState()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            BrandHeader()

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "¡Bienvenido de vuelta!",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ingresa tus credenciales para acceder",
                color = TextMuted,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            uiState.globalError?.let { error ->
                ErrorBanner(errorMessage = error)
                Spacer(modifier = Modifier.height(20.dp))
            }

            SportProInputField(
                label = "Correo electrónico",
                value = uiState.email,
                onValueChange = viewModel::onEmailChanged,
                placeholder = "ejemplo@sportpro.com",
                errorMessage = uiState.emailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            SportProInputField(
                label = "Contraseña",
                value = uiState.password,
                onValueChange = viewModel::onPasswordChanged,
                placeholder = "Ingresa tu contraseña",
                errorMessage = uiState.passwordError,
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onForgotPasswordClick() }
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = { viewModel.login() },
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = DarkBackground,
                    disabledContainerColor = NeonGreen.copy(alpha = 0.5f)
                )
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = DarkBackground,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿No tienes cuenta? ",
                    color = TextMuted,
                    fontSize = 14.sp
                )
                Text(
                    text = "Regístrate",
                    color = NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}
