package com.example.karma.ui.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object History : Screen("history")
    data object Prayer : Screen("prayer")
}
