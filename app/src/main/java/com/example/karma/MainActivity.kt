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

        // 桌面组件跨阶打开 App（冷启动路径）：先登记补弹自证请求，再建 UI
        handleWidgetProofIntent(intent)

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

                        // 每日必做 vis / 条目变化后刷新桌面组件（组件未添加时 refreshAll 内部直接返回）。
                        // 放在 Activity 层而非 MainScreen：用户停在设置等子页时 MainScreen 未组合，
                        // 此时衰减跨天重置 vis、或设置页改配置，也需要刷新组件。
                        val settings by appContainer.repository.settings.collectAsState(initial = null)
                        LaunchedEffect(settings?.dailyMustDoDeeds) {
                            DailyMustDoWidgetProvider.refreshAll(this@MainActivity)
                        }

                        // 桌面组件跨阶时把界面拉回首页：
                        // 用户若停在设置/誓约等子页，MainScreen 未被组合，补弹请求不会被消费，
                        // 故收到请求先导航回 Main（已在 Main 时直接返回，冷启动也无需导航）。
                        val widgetRequest by appContainer.widgetProofRequest.collectAsState()
                        LaunchedEffect(widgetRequest) {
                            if (widgetRequest == null) return@LaunchedEffect
                            // currentDestination 为 null 表示 NavHost 尚未就绪（首帧）：此时起始页就是 Main，
                            // 请求会由 MainScreen 自行消费，不必也不应访问 graph（否则可能抛 setGraph 未调用）。
                            val current = navController.currentDestination?.route ?: return@LaunchedEffect
                            if (current == Screen.Main.route) return@LaunchedEffect
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Main.route) { inclusive = false }
                                launchSingleTop = true
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
     * App 已开着时被组件跨阶唤起走这里（singleTask 不重建 Activity，只回调 onNewIntent）。
     * 必须与 onCreate 走同一处理函数，否则「App 开着时跨阶不弹自证」。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetProofIntent(intent)
    }

    /**
     * 统一处理组件跨阶带来的 Intent：取出加分前/后阶位登记补弹请求，并立刻清空 extra，
     * 防止 Activity 重建（旋屏、被系统回收后恢复）时重复消费。
     * 组件点击的写库（vis + 加分 + 历史）已由 DailyMustDoWidgetProvider 完成，这里只负责弹自证窗。
     */
    private fun handleWidgetProofIntent(intent: Intent?) {
        if (intent == null) return
        val oldLevel = intent.getIntExtra(DailyMustDoWidgetProvider.EXTRA_PROOF_FROM, INVALID_LEVEL)
        val newLevel = intent.getIntExtra(DailyMustDoWidgetProvider.EXTRA_PROOF_TO, INVALID_LEVEL)
        if (oldLevel == INVALID_LEVEL || newLevel == INVALID_LEVEL) return
        intent.removeExtra(DailyMustDoWidgetProvider.EXTRA_PROOF_FROM)
        intent.removeExtra(DailyMustDoWidgetProvider.EXTRA_PROOF_TO)
        setIntent(Intent())
        (application as KarmaApplication).container.requestWidgetProof(oldLevel, newLevel)
    }

    private companion object {
        /** Intent 中缺少阶位 extra 时的哨兵值。 */
        const val INVALID_LEVEL = Int.MIN_VALUE
    }
}
