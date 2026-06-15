package com.example.karma.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.karma.di.AppContainer
import com.example.karma.ui.history.HistoryScreen
import com.example.karma.ui.main.MainScreen
import com.example.karma.ui.prayer.PrayerScreen
import com.example.karma.ui.settings.SettingsScreen

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
    }
}
