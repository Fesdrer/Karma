package com.example.karma.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object History : Screen("history")
    data object Prayer : Screen("prayer")
    data object Bet : Screen("bet")
    data object Divination : Screen("divination")
    data object Settings : Screen("settings")
    data object Alarm : Screen("alarm")
    data object AlarmEdit : Screen("alarm_edit/{alarmId}") {
        fun route(alarmId: Long) = "alarm_edit/$alarmId"
    }
}
