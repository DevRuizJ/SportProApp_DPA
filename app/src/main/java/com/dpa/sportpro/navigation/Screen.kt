package com.dpa.sportpro.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Academies : Screen("academies")
    data object PlayerProfile : Screen("player-profile/{viewerRole}/{viewerName}") {
        fun createRoute(viewerRole: String, viewerName: String): String =
            "player-profile/$viewerRole/${android.net.Uri.encode(viewerName)}"
    }
    data object Home : Screen("home/{roleCode}/{userName}") {
        fun createRoute(roleCode: String, userName: String): String {
            return "home/$roleCode/$userName"
        }
    }
}
