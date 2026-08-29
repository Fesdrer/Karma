package com.example.karma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.karma.ui.navigation.KarmaNavGraph
import com.example.karma.ui.theme.KarmaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 沉浸式状态栏/导航栏：系统栏透明化由 enableEdgeToEdge 处理，
        // 栏后区域由 App 自身的渐变背景接管（消除系统绘制的"黑框"）。
        // 图标明暗自适应（随渐变顶色亮度）在 Theme.kt 中动态设置；
        // 此处先设浅色图标作为深色主题下的初始默认值。
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        val appContainer = (application as KarmaApplication).container

        setContent {
            KarmaTheme(repository = appContainer.repository) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                ) {
                    // 整体处理软键盘：键盘弹出时页面底部空出键盘高度、内容可滚动，
                    // 不依赖华为/旧鸿蒙失效的 adjustResize（所有页面统一生效）
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding(),
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
}
