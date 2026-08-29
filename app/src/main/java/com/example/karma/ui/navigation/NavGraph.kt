package com.example.karma.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.karma.di.AppContainer
import com.example.karma.ui.alarm.AlarmEditScreen
import com.example.karma.ui.alarm.AlarmListScreen
import com.example.karma.ui.bet.BetScreen
import com.example.karma.ui.divination.DivinationScreen
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
        composable(
            route = Screen.Main.route,
            enterTransition = { fadeIn(tween(0)) },
            exitTransition = { fadeOut(tween(0)) },
            popEnterTransition = { fadeIn(tween(0)) },
            popExitTransition = { fadeOut(tween(0)) },
        ) {
            MainScreen(
                appContainer = appContainer,
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToPrayer = {
                    navController.navigate(Screen.Prayer.route)
                },
                onNavigateToBet = {
                    navController.navigate(Screen.Bet.route)
                },
                onNavigateToDivination = {
                    navController.navigate(Screen.Divination.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAlarm = {
                    navController.navigate(Screen.Alarm.route)
                },
            )
        }
        composable(
            route = Screen.History.route,
            enterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
        ) {
            HistoryScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.Prayer.route,
            enterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
        ) {
            PrayerScreen(
                appContainer = appContainer,
                onComplete = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.Settings.route,
            // 设置页已拆分为轻量的分类入口页（根页只有 6 张卡片），
            // 进入/退出淡入淡出与其他页面一致。
            enterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
        ) {
            SettingsScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.Bet.route,
            enterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
        ) {
            BetScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.Divination.route,
            enterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
        ) {
            DivinationScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.Alarm.route,
            enterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
        ) {
            AlarmListScreen(
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
                onNewAlarm = { navController.navigate(Screen.AlarmEdit.route(0L)) },
                onEditAlarm = { id -> navController.navigate(Screen.AlarmEdit.route(id)) },
            )
        }
        composable(
            route = Screen.AlarmEdit.route,
            enterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            exitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
            popEnterTransition = { fadeIn(tween(250, easing = FastOutSlowInEasing)) },
            popExitTransition = { fadeOut(tween(250, easing = FastOutSlowInEasing)) },
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getString("alarmId")?.toLongOrNull() ?: 0L
            AlarmEditScreen(
                alarmId = alarmId,
                appContainer = appContainer,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
