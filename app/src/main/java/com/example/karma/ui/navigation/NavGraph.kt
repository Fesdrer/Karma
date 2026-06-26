package com.example.karma.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.karma.di.AppContainer
import com.example.karma.ui.divination.DivinationScreen
import com.example.karma.ui.history.HistoryScreen
import com.example.karma.ui.main.MainScreen
import com.example.karma.ui.prayer.PrayerScreen
import com.example.karma.ui.settings.SettingsScreen
import com.example.karma.ui.timer.TimerScreen

@Composable
fun KarmaNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                appContainer = appContainer,
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToPrayer = {
                    navController.navigate(Screen.Prayer.route)
                },
                onNavigateToDivination = {
                    navController.navigate(Screen.Divination.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToTimer = { score, event ->
                    navController.navigate(Screen.Timer.createRoute(score, event))
                },
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Prayer.route) {
            PrayerScreen(
                appContainer = appContainer,
                onComplete = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Divination.route) {
            DivinationScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Timer.route) { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toFloatOrNull() ?: 0f
            val event = backStackEntry.arguments?.getString("event")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            TimerScreen(
                score = score,
                event = event,
                appContainer = appContainer,
                onComplete = { navController.popBackStack() },
            )
        }
    }
}
