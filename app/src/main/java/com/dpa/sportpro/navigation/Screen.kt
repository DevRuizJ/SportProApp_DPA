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

    data object MatchEvents : Screen("match-events")

    data object MatchLiveFeed : Screen("match-live-feed")

    data object MatchStatistics : Screen("match-statistics")

    data object MatchChronicle : Screen("match-chronicle/{viewerRole}") {
        fun createRoute(viewerRole: String): String = "match-chronicle/$viewerRole"
    }

    data object Announcements : Screen("announcements/{viewerRole}/{viewerId}/{viewerName}") {
        fun createRoute(viewerRole: String, viewerId: String, viewerName: String): String =
            "announcements/$viewerRole/${android.net.Uri.encode(viewerId)}/" +
                android.net.Uri.encode(viewerName)
    }

    data object Community : Screen("community/{roleCode}/{userId}/{userName}/{verified}") {
        fun createRoute(
            roleCode: String,
            userId: String,
            userName: String,
            verified: Boolean,
        ): String =
            "community/$roleCode/${android.net.Uri.encode(userId)}/" +
                "${android.net.Uri.encode(userName)}/$verified"
    }

    data object Tryouts : Screen("tryouts/{viewerRole}/{viewerId}/{viewerName}") {
        fun createRoute(viewerRole: String, viewerId: String, viewerName: String): String =
            "tryouts/$viewerRole/${android.net.Uri.encode(viewerId)}/" +
                android.net.Uri.encode(viewerName)
    }

    data object Moderation : Screen("moderation/{viewerRole}") {
        fun createRoute(viewerRole: String): String = "moderation/$viewerRole"
    }

    data object Home : Screen("home/{roleCode}/{userId}/{userName}/{verified}/{email}") {
        fun createRoute(
            roleCode: String,
            userId: String,
            userName: String,
            verified: Boolean,
            email: String,
        ): String =
            "home/$roleCode/${android.net.Uri.encode(userId)}/" +
                "${android.net.Uri.encode(userName)}/$verified/${android.net.Uri.encode(email)}"
    }
}
