package com.example.karma.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object History : Screen("history")
    data object Prayer : Screen("prayer")
    data object Divination : Screen("divination")
    data object Settings : Screen("settings")
    data object Timer : Screen("timer/{score}/{event}") {
        fun createRoute(score: Float, event: String): String =
            "timer/$score/${java.net.URLEncoder.encode(event, "UTF-8")}"
    }
}
