package com.dpa.sportpro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.model.UserRole
import com.dpa.sportpro.ui.academy.AcademiesScreen
import com.dpa.sportpro.ui.announcements.AnnouncementsScreen
import com.dpa.sportpro.ui.attendance.AttendanceScreen
import com.dpa.sportpro.ui.chronicle.MatchChronicleScreen
import com.dpa.sportpro.ui.convocation.ConvocationScreen
import com.dpa.sportpro.ui.events.EventCatalogScreen
import com.dpa.sportpro.ui.fees.MonthlyFeesScreen
import com.dpa.sportpro.ui.home.HomeScreen
import com.dpa.sportpro.ui.home.GlobalNavigationDrawer
import com.dpa.sportpro.ui.lineup.TacticalLineupScreen
import com.dpa.sportpro.ui.login.LoginScreen
import com.dpa.sportpro.ui.moderation.ModerationScreen
import com.dpa.sportpro.ui.player.PlayerTechnicalProfileScreen
import com.dpa.sportpro.ui.register.RegisterScreen
import com.dpa.sportpro.ui.statistics.MatchStatisticsScreen
import com.dpa.sportpro.ui.training.TrainingPlannerScreen
import com.dpa.sportpro.ui.tryouts.TryoutsScreen
import com.dpa.sportpro.ui.theme.DarkBackground
import com.dpa.sportpro.ui.theme.NeonGreen
import com.dpa.sportpro.ui.theme.TextMuted
import com.dpa.sportpro.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun SportProNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
) {
    var activeUser by remember { mutableStateOf<UserProfile?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val selectedMenuKey =
        when {
            currentRoute == Screen.MatchEvents.route -> "match-events"
            currentRoute == Screen.MatchLiveFeed.route -> "live-feed"
            currentRoute == Screen.MatchStatistics.route -> "statistics"
            currentRoute == Screen.MatchChronicle.route -> "chronicle"
            currentRoute == Screen.Announcements.route -> "announcements"
            currentRoute == Screen.Community.route || currentRoute == Screen.Home.route -> "community"
            currentRoute == Screen.Tryouts.route -> "tryouts"
            currentRoute == Screen.Moderation.route -> "moderation"
            currentRoute == Screen.Academies.route -> "academies"
            currentRoute == Screen.PlayerProfile.route -> "player-profile"
            currentRoute == Screen.TrainingPlanner.route -> "training"
            currentRoute == Screen.MonthlyFees.route -> "monthly-fees"
            currentRoute == Screen.Attendance.route -> "attendance"
            currentRoute == Screen.Convocations.route -> "convocations"
            currentRoute == Screen.TacticalLineup.route -> "lineup"
            currentRoute == Screen.EventCatalog.route -> "event-catalog"
            else -> "community"
        }

    fun navigateToMenuOption(key: String, user: UserProfile) {
        val role = user.role
        when (key) {
            "community" -> {
                val homeRoute =
                    Screen.Home.createRoute(
                        role.code,
                        user.uid,
                        listOf(user.names, user.lastNames)
                            .filter { it.isNotBlank() }
                            .joinToString(" "),
                        user.isVerified,
                        user.email,
                    )
                if (!navController.popBackStack(homeRoute, inclusive = false)) {
                    navController.navigate(homeRoute) { launchSingleTop = true }
                }
            }
            "academies" -> navController.navigate(Screen.Academies.route)
            "player-profile" ->
                navController.navigate(Screen.PlayerProfile.createRoute(role.code, user.names))
            "training" ->
                navController.navigate(Screen.TrainingPlanner.createRoute(role.code, user.names))
            "monthly-fees" ->
                navController.navigate(Screen.MonthlyFees.createRoute(role.code, user.names))
            "attendance" ->
                navController.navigate(Screen.Attendance.createRoute(role.code, user.names))
            "convocations" ->
                navController.navigate(Screen.Convocations.createRoute(role.code, user.names))
            "lineup" ->
                navController.navigate(Screen.TacticalLineup.createRoute(role.code, "none"))
            "event-catalog" -> navController.navigate(Screen.EventCatalog.createRoute(role.code))
            "match-events" -> navController.navigate(Screen.MatchEvents.route)
            "live-feed" -> navController.navigate(Screen.MatchLiveFeed.route)
            "statistics" -> navController.navigate(Screen.MatchStatistics.route)
            "chronicle" -> navController.navigate(Screen.MatchChronicle.createRoute(role.code))
            "announcements" ->
                navController.navigate(
                    Screen.Announcements.createRoute(role.code, user.uid, user.names)
                )
            "tryouts" ->
                navController.navigate(Screen.Tryouts.createRoute(role.code, user.uid, user.names))
            "moderation" -> navController.navigate(Screen.Moderation.createRoute(role.code))
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            activeUser?.let { user ->
                GlobalNavigationDrawer(
                    userProfile = user,
                    selectedKey = selectedMenuKey,
                    onOptionSelected = { key ->
                        navigateToMenuOption(key, user)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onLogoutClick = {
                        activeUser = null
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        coroutineScope.launch { drawerState.close() }
                    },
                )
            } ?: ModalDrawerSheet {}
        },
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .pointerInput(activeUser, drawerState) {
                        var dragDistance = 0f
                        var startedAtDrawerEdge = false
                        var opened = false
                        detectHorizontalDragGestures(
                            onDragStart = { start ->
                                dragDistance = 0f
                                startedAtDrawerEdge = start.x <= 96.dp.toPx()
                                opened = false
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (startedAtDrawerEdge) {
                                    dragDistance += dragAmount
                                    if (
                                        !opened &&
                                            dragDistance >= 72.dp.toPx() &&
                                            activeUser != null &&
                                            !drawerState.isOpen
                                    ) {
                                        opened = true
                                        change.consume()
                                        coroutineScope.launch { drawerState.open() }
                                    }
                                }
                            },
                            onDragEnd = { dragDistance = 0f },
                            onDragCancel = { dragDistance = 0f },
                        )
                    }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (activeUser != null) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .background(DarkBackground)
                                .padding(start = 12.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir menú de navegación",
                                tint = NeonGreen,
                            )
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "SportPro",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = activeUser?.role?.displayName.orEmpty(),
                                color = TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.weight(1f),
                ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = { userProfile ->
                    activeUser = userProfile
                    val fullName =
                        listOf(userProfile.names, userProfile.lastNames)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    val route =
                        Screen.Home.createRoute(
                            userProfile.role.code,
                            userProfile.uid,
                            fullName,
                            userProfile.isVerified,
                            userProfile.email,
                        )
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

        composable(route = Screen.MatchEvents.route) {
            com.dpa.sportpro.ui.matchevents.MatchEventsScreen()
        }

        composable(route = Screen.MatchLiveFeed.route) {
            com.dpa.sportpro.ui.livefeed.MatchLiveFeedScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.MatchStatistics.route) {
            MatchStatisticsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = Screen.MatchChronicle.route,
            arguments = listOf(navArgument("viewerRole") { type = NavType.StringType }),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole").orEmpty())
                    ?: UserRole.PLAYER
            MatchChronicleScreen(
                viewerRole = viewerRole,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Announcements.route,
            arguments =
                listOf(
                    navArgument("viewerRole") { type = NavType.StringType },
                    navArgument("viewerId") { type = NavType.StringType },
                    navArgument("viewerName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole") ?: "")
                    ?: UserRole.PLAYER
            AnnouncementsScreen(
                viewerRole = viewerRole,
                viewerId = backStackEntry.arguments?.getString("viewerId").orEmpty(),
                viewerName = backStackEntry.arguments?.getString("viewerName").orEmpty(),
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Community.route,
            arguments =
                listOf(
                    navArgument("roleCode") { type = NavType.StringType },
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType },
                    navArgument("verified") { type = NavType.BoolType },
                ),
        ) { backStackEntry ->
            val role =
                UserRole.fromCode(backStackEntry.arguments?.getString("roleCode").orEmpty())
                    ?: UserRole.PLAYER
            com.dpa.sportpro.ui.community.CommunityScreen(
                userProfile =
                    UserProfile(
                        uid = backStackEntry.arguments?.getString("userId").orEmpty(),
                        email = "",
                        names = backStackEntry.arguments?.getString("userName").orEmpty(),
                        lastNames = "",
                        role = role,
                        isVerified = backStackEntry.arguments?.getBoolean("verified") == true,
                    ),
            )
        }

        composable(
            route = Screen.Tryouts.route,
            arguments =
                listOf(
                    navArgument("viewerRole") { type = NavType.StringType },
                    navArgument("viewerId") { type = NavType.StringType },
                    navArgument("viewerName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole").orEmpty())
                    ?: UserRole.PLAYER
            TryoutsScreen(
                viewerRole = viewerRole,
                viewerId = backStackEntry.arguments?.getString("viewerId").orEmpty(),
                viewerName = backStackEntry.arguments?.getString("viewerName").orEmpty(),
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Moderation.route,
            arguments = listOf(navArgument("viewerRole") { type = NavType.StringType }),
        ) { backStackEntry ->
            val viewerRole =
                UserRole.fromCode(backStackEntry.arguments?.getString("viewerRole").orEmpty())
                    ?: UserRole.PLAYER
            ModerationScreen(
                viewerRole = viewerRole,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Home.route,
            arguments =
                listOf(
                    navArgument("roleCode") { type = NavType.StringType },
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType },
                    navArgument("verified") { type = NavType.BoolType },
                    navArgument("email") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val roleCode = backStackEntry.arguments?.getString("roleCode") ?: "JUG"
            val userId = backStackEntry.arguments?.getString("userId").orEmpty()
            val userName = backStackEntry.arguments?.getString("userName") ?: "Usuario"
            val isVerified = backStackEntry.arguments?.getBoolean("verified") == true
            val userRole = UserRole.fromCode(roleCode) ?: UserRole.PLAYER

            val userProfile =
                UserProfile(
                    uid = userId,
                    email = backStackEntry.arguments?.getString("email").orEmpty(),
                    names = userName,
                    lastNames = "",
                    role = userRole,
                    isVerified = isVerified,
                )

            HomeScreen(userProfile = userProfile)
        }
                }
            }
            if (activeUser != null) {
                Box(
                    modifier =
                        Modifier.align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(96.dp)
                            .systemGestureExclusion()
                )
            }
        }
    }
}
