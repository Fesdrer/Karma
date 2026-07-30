package com.example.karma.ui.main

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.karma.data.local.entity.KarmaSettingsEntity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.data.model.TimerStatus
import com.example.karma.ui.main.components.AxisCanvas
import com.example.karma.ui.main.components.EventPanel
import com.example.karma.ui.main.components.Footer
import com.example.karma.ui.main.components.Header
import com.example.karma.ui.main.components.ScorePanel
import com.example.karma.ui.timer.TimerService
import kotlin.math.round
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 进程级标志：经文启动画面只在每次进入应用时显示一次，导航到子页面再返回不重复显示 */
private var splashShown = false

@Composable
fun MainScreen(
    appContainer: AppContainer,
    onNavigateToHistory: () -> Unit,
    onNavigateToPrayer: () -> Unit,
    onNavigateToDivination: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(appContainer.repository)
    )
    val state by viewModel.uiState.collectAsState()
    val effectiveScore by viewModel.effectiveScoreState.collectAsState()
    val context = LocalContext.current

    // 经文启动画面阶段（进程级标志，导航回来不重复显示）
    var splashDone by remember { mutableStateOf(splashShown) }
    val settings by appContainer.repository.settings.collectAsState(initial = KarmaSettingsEntity())

    // 仅在首次进入应用时显示经文启动画面
    LaunchedEffect(state != null) {
        if (state != null && !splashShown) {
            delay((settings.splashDurationSec * 1000L).coerceAtLeast(1000L))
            splashDone = true
            splashShown = true
        }
    }

    // 数据就绪时从纯黑渐变到主页面
    Crossfade(targetState = state != null, animationSpec = tween(300)) { ready ->
        if (!ready) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            return@Crossfade
        }
        val s = state!!

    // 启动时恢复计时状态 + 持续观察状态变化并持久化
    // 不依赖 TimerService 的生命周期，所有数据库读写通过 LaunchedEffect 的协程作用域执行
    LaunchedEffect(Unit) {
        // step 1：恢复上次未结束的计时
        val saved = appContainer.repository.loadTimerState()
        if (saved != null && (saved.status == TimerStatus.RUNNING || saved.status == TimerStatus.PAUSED)) {
            TimerService.restoreTimerState(saved, SystemClock.elapsedRealtime())
            if (saved.status == TimerStatus.RUNNING) {
                val intent = android.content.Intent(context, com.example.karma.ui.timer.TimerService::class.java).apply { action = "RESTORE" }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        // step 2：持续观察计时状态，任何变化都写 Room
        // 用 NonCancellable 防止 LaunchedEffect 被取消（例如用户离开主屏）时
        // Room 写入被回滚，留下「已停止但 DB 仍标记为 RUNNING」的残留状态。
        TimerService.timerState.collect { state ->
            withContext(NonCancellable) {
                when (state.status) {
                    TimerStatus.RUNNING, TimerStatus.PAUSED -> {
                        appContainer.repository.saveTimerState(
                            status = state.status.name,
                            startElapsed = state.startElapsed,
                            resumeElapsed = state.resumeElapsed,
                            accumulatedMs = state.accumulatedMs,
                            selectedScore = state.selectedScore,
                            selectedEvent = state.selectedEvent,
                        )
                    }
                    TimerStatus.STOPPED -> {
                        appContainer.repository.clearTimerState()
                    }
                    TimerStatus.IDLE -> { /* 首次打开没有计时，不操作 */ }
                }
            }
        }
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(s.message) {
        s.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
        ) {
            // Header
            Header(
                totalScore = s.totalScore,
                rank = s.rank,
                ranks = s.ranks,
                luckValue = s.luckValue,
            )

            Spacer(Modifier.height(12.dp))

            // Main content: three columns
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                // Left panel: Scores
                ScorePanel(
                    scoreFlow = viewModel.effectiveScoreState,
                    onScoreSelected = { viewModel.selectScore(it) },
                    onCustomScoreChanged = { viewModel.onCustomScoreChanged(it) },
                    axisFontSize = s.scoreAxisFontSize,
                    axisRangeMin = s.scoreAxisRangeMin,
                    axisRangeMax = s.scoreAxisRangeMax,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                Spacer(Modifier.width(6.dp))

                // Center panel: Axis
                AxisCanvas(
                    totalScore = s.totalScore,
                    labelColor = s.axisLabelColor,
                    tickThickness = s.axisTickThickness,
                    labelFontSize = s.axisLabelFontSize,
                    displayRange = s.axisDisplayRange,
                    showNearby = s.showNearbyTicks,
                    nearbyRange = s.nearbyTickRange,
                    quarterValue = s.axisQuarterValue,
                    ranks = s.ranks,
                    dotColor = s.dotColor,
                    modifier = Modifier
                        .width(75.dp)
                        .fillMaxHeight(),
                )

                Spacer(Modifier.width(6.dp))

                // Right panel: Events
                EventPanel(
                    goodDeedPresets = s.goodDeedPresets,
                    badDeedPresets = s.badDeedPresets,
                    goodResultPresets = s.goodResultPresets,
                    eventFlow = viewModel.effectiveEventState,
                    onEventSelected = { viewModel.selectEvent(it) },
                    onCustomGoodDeedChanged = { viewModel.onCustomGoodDeedEventChanged(it) },
                    onCustomBadDeedChanged = { viewModel.onCustomBadDeedEventChanged(it) },
                    onCustomGoodResultChanged = { viewModel.onCustomGoodResultEventChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    timerEnabled = s.hasScoreAndEvent,
                    selectedScore = effectiveScore,
                    dailyMustDoDeeds = s.dailyMustDoDeeds,
                    onStopTimer = { elapsedMs ->
                        val ts = TimerService.timerState.value
                        val totMin = elapsedMs / 60000.0
                        val capturedEvent = ts.selectedEvent
                        val capturedScore = ts.selectedScore
                        val delta = round(totMin / 60.0 * capturedScore * 2.0) / 2.0
                        viewModel.viewModelScope.launch {
                            try {
                                // ★ 先写 DB（标记每日必做完成 + 添加历史记录）
                                val deed = s.dailyMustDoDeeds.find { it.name == capturedEvent }
                                if (deed != null && deed.vis == 0) {
                                    appContainer.repository.markDeedDone(capturedEvent)
                                }
                                appContainer.repository.addHistoryEntry(delta.toFloat(), capturedEvent, "record")
                            } finally {
                                // ★ DB 写入完成后才停服务，保证 LaunchedEffect 的 collect
                                //    读到 STOPPED 时 DB 数据已经是最新的，不会并发覆盖。
                                TimerService.stop(context)
                            }
                        }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Footer
            Footer(
                confirmEnabled = s.hasScoreAndEvent,
                prayerEnabled = s.totalScore >= 30f,
                onConfirm = { viewModel.onConfirm() },
                onPrayer = onNavigateToPrayer,
                onDivination = onNavigateToDivination,
                onHistory = onNavigateToHistory,
                onSettings = onNavigateToSettings,
            )
        }
    }
    }

    // ---- 经文启动画面覆盖层 ----
    if (!splashDone && state != null) {
        ScriptureOverlay(
            text = settings.splashScripture.ifEmpty { "凡所有相，皆是虚妄。若见诸相非相，即见如来。" },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 竖排经文画面：金色衬线体，从右到左排列，先填满一列再换下一列，左右居中、上下居中
 */
@Composable
private fun ScriptureOverlay(text: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(Color(0xFF0a0a0f))) {
        val w = size.width
        val h = size.height
        val charCount = text.length
        if (charCount == 0) return@Canvas

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 255, 215, 0)   // 金色
            isAntiAlias = true
            typeface = android.graphics.Typeface.SERIF              // 衬线体
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val lineHeightFactor = 1.4f      // 行高倍率
        val columnSpacingFactor = 0.5f   // 列间距倍率（相对于字号）

        // 迭代计算自适应字号：确保文字块不超出屏幕宽度
        var fontSize = minOf(w / 3f, h / (3f * lineHeightFactor))
        var maxCharsPerCol = 1
        var columns = charCount
        for (iter in 0 until 30) {
            paint.textSize = fontSize
            maxCharsPerCol = maxOf(1, (h / (fontSize * lineHeightFactor)).toInt())
            columns = (charCount + maxCharsPerCol - 1) / maxCharsPerCol
            val totalWidth = columns * fontSize * (1f + columnSpacingFactor) - fontSize * columnSpacingFactor
            if (totalWidth <= w) break
            fontSize *= 0.95f
        }

        val charHeight = fontSize * lineHeightFactor   // 每字占用高度
        val colWidth = fontSize * (1f + columnSpacingFactor)  // 列宽（字宽+列间距）

        // 文字块总宽度（所有列）
        val totalWidth = columns * colWidth - fontSize * columnSpacingFactor

        // 最右列中心 X（左右居中）
        val rightEdgeX = (w + totalWidth) / 2f

        // 竖排：从右到左逐列绘制，每列从上到下填满字符
        for (col in 0 until columns) {
            val charStart = col * maxCharsPerCol
            val charEnd = minOf(charStart + maxCharsPerCol, charCount)
            val charsInThisCol = charEnd - charStart

            // 此列中心 X（从右到左递减）
            val colX = rightEdgeX - col * colWidth

            // 此列文字垂直居中
            val colHeight = charsInThisCol * charHeight
            val startY = (h - colHeight) / 2f + fontSize  // baseline 偏移

            for (row in 0 until charsInThisCol) {
                val charIndex = charStart + row
                val ch = text[charIndex].toString()
                val charY = startY + row * charHeight
                drawContext.canvas.nativeCanvas.drawText(ch, colX, charY, paint)
            }
        }
    }
}
