package com.dpa.sportpro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.academy.AcademiesScreen
import com.dpa.sportpro.ui.attendance.AttendanceScreen
import com.dpa.sportpro.ui.convocation.ConvocationScreen
import com.dpa.sportpro.ui.events.EventCatalogScreen
import com.dpa.sportpro.ui.fees.MonthlyFeesScreen
import com.dpa.sportpro.ui.home.HomeScreen
import com.dpa.sportpro.ui.lineup.TacticalLineupScreen
import com.dpa.sportpro.ui.login.LoginScreen
import com.dpa.sportpro.ui.player.PlayerTechnicalProfileScreen
import com.dpa.sportpro.ui.register.RegisterScreen
import com.dpa.sportpro.ui.training.TrainingPlannerScreen

@Composable
fun SportProNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = { userProfile ->
                    val fullName =
                        listOf(userProfile.names, userProfile.lastNames)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    val route = Screen.Home.createRoute(userProfile.role.code, fullName)
                    navController.navigate(route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(onNavigateToLogin = { navController.popBackStack() })
        }

        composable(route = Screen.Academies.route) {
            AcademiesScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = Screen.PlayerProfile.route,
            arguments =
                listOf(
                    navArgument("viewerRole") { type = NavType.StringType },
                    navArgument("viewerName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole") ?: "")
                    ?: UserRole.PLAYER
            PlayerTechnicalProfileScreen(
                viewerRole = viewerRole,
                viewerName = backStackEntry.arguments?.getString("viewerName").orEmpty(),
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.TrainingPlanner.route,
            arguments =
                listOf(
                    navArgument("viewerRole") { type = NavType.StringType },
                    navArgument("viewerName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole") ?: "")
                    ?: UserRole.PLAYER
            TrainingPlannerScreen(
                viewerRole = viewerRole,
                viewerName = backStackEntry.arguments?.getString("viewerName").orEmpty(),
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.MonthlyFees.route,
            arguments =
                listOf(
                    navArgument("viewerRole") { type = NavType.StringType },
                    navArgument("viewerName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole") ?: "")
                    ?: UserRole.PLAYER
            MonthlyFeesScreen(
                viewerRole = viewerRole,
                viewerName = backStackEntry.arguments?.getString("viewerName").orEmpty(),
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Attendance.route,
            arguments =
                listOf(
                    navArgument("viewerRole") { type = NavType.StringType },
                    navArgument("viewerName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole") ?: "")
                    ?: UserRole.PLAYER
            AttendanceScreen(
                viewerRole = viewerRole,
                viewerName = backStackEntry.arguments?.getString("viewerName").orEmpty(),
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Convocations.route,
            arguments =
                listOf(
                    navArgument("viewerRole") { type = NavType.StringType },
                    navArgument("viewerName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole") ?: "")
                    ?: UserRole.PLAYER
            ConvocationScreen(
                viewerRole = viewerRole,
                viewerName = backStackEntry.arguments?.getString("viewerName").orEmpty(),
                onBackClick = { navController.popBackStack() },
                onLineupClick = { convocationId ->
                    navController.navigate(
                        Screen.TacticalLineup.createRoute(viewerRole.code, convocationId)
                    )
                },
            )
        }

        composable(
            route = Screen.TacticalLineup.route,
            arguments =
                listOf(
                    navArgument("viewerRole") { type = NavType.StringType },
                    navArgument("convocationId") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole") ?: "")
                    ?: UserRole.PLAYER
            TacticalLineupScreen(
                viewerRole = viewerRole,
                convocationId = backStackEntry.arguments?.getString("convocationId"),
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.EventCatalog.route,
            arguments = listOf(navArgument("viewerRole") { type = NavType.StringType }),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole") ?: "")
                    ?: UserRole.PLAYER
            EventCatalogScreen(
                viewerRole = viewerRole,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Home.route,
            arguments =
                listOf(
                    navArgument("roleCode") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val roleCode = backStackEntry.arguments?.getString("roleCode") ?: "JUG"
            val userName = backStackEntry.arguments?.getString("userName") ?: "Usuario"
            val userRole = UserRole.fromCode(roleCode) ?: UserRole.PLAYER

            val userProfile =
                UserProfile(
                    uid = "active_user",
                    email = "$userName@sportpro.com",
                    names = userName,
                    lastNames = "",
                    role = userRole,
                )

            HomeScreen(
                userProfile = userProfile,
                onAcademiesClick = { navController.navigate(Screen.Academies.route) },
                onPlayerProfileClick = {
                    navController.navigate(
                        Screen.PlayerProfile.createRoute(userRole.code, userProfile.names)
                    )
                },
                onTrainingPlannerClick = {
                    navController.navigate(
                        Screen.TrainingPlanner.createRoute(userRole.code, userProfile.names)
                    )
                },
                onMonthlyFeesClick = {
                    navController.navigate(
                        Screen.MonthlyFees.createRoute(userRole.code, userProfile.names)
                    )
                },
                onAttendanceClick = {
                    navController.navigate(
                        Screen.Attendance.createRoute(userRole.code, userProfile.names)
                    )
                },
                onConvocationsClick = {
                    navController.navigate(
                        Screen.Convocations.createRoute(userRole.code, userProfile.names)
                    )
                },
                onTacticalLineupClick = {
                    navController.navigate(Screen.TacticalLineup.createRoute(userRole.code, "none"))
                },
                onEventCatalogClick = {
                    navController.navigate(Screen.EventCatalog.createRoute(userRole.code))
                },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
            )
        }
    }
}
