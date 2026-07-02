package com.example.karma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.karma.ui.navigation.KarmaNavGraph
import com.example.karma.ui.theme.KarmaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 沉浸式状态栏/导航栏：深色背景 + 浅色图标
        window.statusBarColor = android.graphics.Color.rgb(10, 10, 10)
        window.navigationBarColor = android.graphics.Color.rgb(10, 10, 10)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val appContainer = (application as KarmaApplication).container

        setContent {
            KarmaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                ) {
                    val navController = rememberNavController()
                    KarmaNavGraph(
                        navController = navController,
                        appContainer = appContainer,
                    )
                }
            }
        }
    }
}
