package com.dpa.sportpro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dpa.sportpro.ui.academy.AcademiesScreen
import com.dpa.sportpro.ui.account.AccountSecurityScreen
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
    val sessionViewModel: SessionViewModel = viewModel()
    val session by sessionViewModel.profile.collectAsStateWithLifecycle()
    val restoring by sessionViewModel.restoring.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    LaunchedEffect(restoring, session, route) {
        if (!restoring && route != null) {
            if (session == null && route !in setOf(Screen.Login.route, Screen.Register.route, Screen.PasswordRecovery.route)) {
                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            } else if (session != null && route == Screen.Login.route) {
                val user = requireNotNull(session)
                navController.navigate(Screen.Home.createRoute(user.role.code, listOf(user.names, user.lastNames).filter { it.isNotBlank() }.joinToString(" "))) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onForgotPasswordClick = { navController.navigate(Screen.PasswordRecovery.route) },
                onNavigateToHome = { userProfile ->
                    val fullName =
                        listOf(userProfile.names, userProfile.lastNames)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    val route = Screen.Home.createRoute(userProfile.role.code, fullName)
                    navController.navigate(route) {
                        launchSingleTop = true
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(route = Screen.PasswordRecovery.route) {
            AccountSecurityScreen(
                recovery = true,
                onBack = { navController.popBackStack() },
                onSignInAgain = { navController.popBackStack() },
            )
        }

        composable(route = Screen.AccountSecurity.route) {
            val user = session ?: return@composable
            AccountSecurityScreen(
                recovery = false,
                currentEmail = user.email,
                onBack = { navController.popBackStack() },
                onSignInAgain = {
                    sessionViewModel.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable(route = Screen.Register.route) {
            RegisterScreen(onNavigateToLogin = { navController.popBackStack() })
        }

        composable(route = Screen.Academies.route) {
            if (session == null) return@composable
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
            val viewerRole = session?.role ?: return@composable
            PlayerTechnicalProfileScreen(
                viewerRole = viewerRole,
                viewerName = listOfNotNull(session?.names, session?.lastNames).filter { it.isNotBlank() }.joinToString(" "),
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
            val viewerRole = session?.role ?: return@composable
            TrainingPlannerScreen(
                viewerRole = viewerRole,
                viewerName = listOfNotNull(session?.names, session?.lastNames).filter { it.isNotBlank() }.joinToString(" "),
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
            val viewerRole = session?.role ?: return@composable
            MonthlyFeesScreen(
                viewerRole = viewerRole,
                viewerName = listOfNotNull(session?.names, session?.lastNames).filter { it.isNotBlank() }.joinToString(" "),
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
            val viewerRole = session?.role ?: return@composable
            AttendanceScreen(
                viewerRole = viewerRole,
                viewerName = listOfNotNull(session?.names, session?.lastNames).filter { it.isNotBlank() }.joinToString(" "),
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
            val viewerRole = session?.role ?: return@composable
            ConvocationScreen(
                viewerRole = viewerRole,
                viewerName = listOfNotNull(session?.names, session?.lastNames).filter { it.isNotBlank() }.joinToString(" "),
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
            val viewerRole = session?.role ?: return@composable
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
            val viewerRole = session?.role ?: return@composable
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
            val userProfile = session ?: return@composable
            val userRole = userProfile.role

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
                onAccountClick = { navController.navigate(Screen.AccountSecurity.route) },
                onLogoutClick = {
                    sessionViewModel.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
            )
        }
    }
}
