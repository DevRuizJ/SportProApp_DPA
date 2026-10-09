package com.dpa.sportpro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.home.HomeScreen
import com.dpa.sportpro.ui.login.LoginScreen
import com.dpa.sportpro.ui.register.RegisterScreen

@Composable
fun SportProNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = { userProfile ->
                    val route = Screen.Home.createRoute(userProfile.role.code, userProfile.names)
                    navController.navigate(route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Home.route,
            arguments = listOf(
                navArgument("roleCode") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roleCode = backStackEntry.arguments?.getString("roleCode") ?: "JUG"
            val userName = backStackEntry.arguments?.getString("userName") ?: "Usuario"
            val userRole = UserRole.fromCode(roleCode) ?: UserRole.PLAYER

            val userProfile = UserProfile(
                uid = "active_user",
                email = "$userName@sportpro.com",
                names = userName,
                lastNames = "",
                role = userRole
            )

            HomeScreen(
                userProfile = userProfile,
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
