package com.dpa.sportpro.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Academies : Screen("academies")
    data object PlayerProfile : Screen("player-profile/{viewerRole}/{viewerName}") {
        fun createRoute(viewerRole: String, viewerName: String): String =
            "player-profile/$viewerRole/${android.net.Uri.encode(viewerName)}"
    }
    data object TrainingPlanner : Screen("training-planner/{viewerRole}/{viewerName}") {
        fun createRoute(viewerRole: String, viewerName: String): String =
            "training-planner/$viewerRole/${android.net.Uri.encode(viewerName)}"
    }
    data object MonthlyFees : Screen("monthly-fees/{viewerRole}/{viewerName}") {
        fun createRoute(viewerRole: String, viewerName: String): String =
            "monthly-fees/$viewerRole/${android.net.Uri.encode(viewerName)}"
    }
    data object Attendance : Screen("attendance/{viewerRole}/{viewerName}") {
        fun createRoute(viewerRole: String, viewerName: String): String =
            "attendance/$viewerRole/${android.net.Uri.encode(viewerName)}"
    }
    data object Convocations : Screen("convocations/{viewerRole}/{viewerName}") {
        fun createRoute(viewerRole: String, viewerName: String): String =
            "convocations/$viewerRole/${android.net.Uri.encode(viewerName)}"
    }
    data object TacticalLineup : Screen("tactical-lineup/{viewerRole}/{convocationId}") {
        fun createRoute(viewerRole: String, convocationId: String): String =
            "tactical-lineup/$viewerRole/${android.net.Uri.encode(convocationId)}"
    }
    data object EventCatalog : Screen("event-catalog/{viewerRole}") {
        fun createRoute(viewerRole: String): String = "event-catalog/$viewerRole"
    }
    data object Home : Screen("home/{roleCode}/{userName}") {
        fun createRoute(roleCode: String, userName: String): String {
            return "home/$roleCode/$userName"
        }
    }
}
