package com.example.karma

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.karma.ui.navigation.KarmaNavGraph
import com.example.karma.ui.navigation.Screen
import com.example.karma.ui.theme.KarmaTheme
import com.example.karma.ui.widget.DailyMustDoWidgetProvider

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

        // 桌面组件点击进入（冷启动路径）：先登记请求，再建 UI
        handleWidgetDeedIntent(intent)

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

                        // 桌面组件点击时把界面拉回首页：
                        // 用户若停在设置/誓约等子页，MainScreen 未被组合，请求不会被消费，
                        // 故收到请求先导航回 Main（已在 Main 时 launchSingleTop 使其成为空操作）。
                        val widgetRequest by appContainer.widgetDeedRequest.collectAsState()
                        LaunchedEffect(widgetRequest) {
                            if (widgetRequest != null) {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }

                        KarmaNavGraph(
                            navController = navController,
                            appContainer = appContainer,
                        )
                    }
                }
            }
        }
    }

    /**
     * App 已开着时点桌面组件走这里（singleTask 不重建 Activity，只回调 onNewIntent）。
     * **必须与 onCreate 走同一处理函数**，否则「App 开着时点组件不加分」。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetDeedIntent(intent)
    }

    /**
     * 统一处理桌面组件点击：取出 deed 名称登记到容器，并立刻清空 extra，
     * 防止 Activity 重建（旋屏、被系统回收后恢复）时重复消费同一次点击。
     */
    private fun handleWidgetDeedIntent(intent: Intent?) {
        val deedName = intent?.getStringExtra(DailyMustDoWidgetProvider.EXTRA_DEED_NAME) ?: return
        intent.removeExtra(DailyMustDoWidgetProvider.EXTRA_DEED_NAME)
        setIntent(Intent())
        (application as KarmaApplication).container.requestWidgetDeedCompletion(deedName)
    }
}
