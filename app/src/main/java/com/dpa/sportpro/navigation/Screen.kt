package com.dpa.sportpro.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home/{roleCode}/{userName}") {
        fun createRoute(roleCode: String, userName: String): String {
            return "home/$roleCode/$userName"
        }
    }
}
