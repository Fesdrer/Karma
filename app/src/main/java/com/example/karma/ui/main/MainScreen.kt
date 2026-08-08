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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

    // 触摸暂停标志：按下（touching=true）暂停倒计时，放手继续
    var touching by remember { mutableStateOf(false) }

    // 仅在首次进入应用时显示经文启动画面
    // 倒计时可被触摸暂停：以 50ms 为刻度累计实际流逝时间，按下期间不计时
    LaunchedEffect(state != null) {
        if (state != null && !splashShown) {
            val totalMs = (settings.splashDurationSec * 1000L).coerceAtLeast(1000L)
            var elapsedMs = 0L
            var lastTick = SystemClock.elapsedRealtime()
            while (elapsedMs < totalMs) {
                val now = SystemClock.elapsedRealtime()
                if (!touching) elapsedMs += now - lastTick
                lastTick = now
                delay(50L)
            }
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
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // 有任何手指按下 → 暂停倒计时；全部松开 → 继续
                            val event = awaitPointerEvent()
                            touching = event.changes.any { it.pressed }
                        }
                    }
                },
        )
    }
}

/**
 * 竖排经文画面：金色衬线体，从右到左排列，先填满一列再换下一列。
 * 上下预留空间避开系统栏，列间有金色分割线，四周有金色边框（仿古书样式）。
 */
@Composable
private fun ScriptureOverlay(text: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    // 上下预留 = 真实系统栏 inset + 16dp 呼吸边距（不再硬编码 56dp，适应任意机型栏高/刘海）
    val topPadPx = with(density) { WindowInsets.statusBars.getTop(density).toFloat() + 16.dp.toPx() }
    val botPadPx = with(density) { WindowInsets.navigationBars.getBottom(density).toFloat() + 16.dp.toPx() }
    val maxFontSizePx = with(density) { 42.dp.toPx() }  // 最大字号
    val borderPadPx = with(density) { 12.dp.toPx() }    // 边框与文字间距
    val lineStroke = with(density) { 1.dp.toPx() }      // 线宽

    Canvas(modifier = modifier.background(Color(0xFF0a0a0f))) {
        val w = size.width
        val h = size.height
        if (text.isEmpty()) return@Canvas

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 255, 215, 0)   // 金色
            isAntiAlias = true
            typeface = android.graphics.Typeface.SERIF              // 衬线体
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(200, 255, 215, 0)  // 半透明金
            isAntiAlias = true
            strokeWidth = lineStroke
            style = android.graphics.Paint.Style.STROKE
        }

        val lineHeightFactor = 1.4f
        val columnSpacingFactor = 0.5f

        val availH = h - topPadPx - botPadPx   // 扣除上下预留后的可用高度

        /** 按 \n 分段，每段内按 maxCharsPerCol 自动换列，返回各列字符串 */
        fun buildColumns(maxPerCol: Int): List<String> {
            val result = mutableListOf<String>()
            val sb = StringBuilder()
            for (ch in text) {
                if (ch == '\n') {
                    result.add(sb.toString())
                    sb.clear()
                } else {
                    if (sb.length >= maxPerCol) {
                        result.add(sb.toString())
                        sb.clear()
                    }
                    sb.append(ch)
                }
            }
            if (sb.isNotEmpty()) result.add(sb.toString())
            return result
        }

        // 迭代计算自适应字号：不超过最大字号，且文字块不超出屏幕宽度
        var fontSize = minOf(w / 3f, availH / (3f * lineHeightFactor), maxFontSizePx)
        var maxCharsPerCol = 1
        var columnTexts: List<String> = buildColumns(maxCharsPerCol)
        for (iter in 0 until 30) {
            textPaint.textSize = fontSize
            maxCharsPerCol = maxOf(1, (availH / (fontSize * lineHeightFactor)).toInt())
            columnTexts = buildColumns(maxCharsPerCol)
            val columns = columnTexts.size
            val totalWidth = columns * fontSize * (1f + columnSpacingFactor) - fontSize * columnSpacingFactor
            if (totalWidth + borderPadPx * 2 + lineStroke * 2 <= w) break
            fontSize *= 0.95f
        }

        val columns = columnTexts.size
        if (columns == 0) return@Canvas

        val charHeight = fontSize * lineHeightFactor
        val colWidth = fontSize * (1f + columnSpacingFactor)

        // 文字块尺寸
        val textBlockW = columns * colWidth - fontSize * columnSpacingFactor
        val textBlockH = maxCharsPerCol * charHeight

        // 文字块左右居中、上下置顶
        val blockLeft = (w - textBlockW) / 2f
        val blockTop = topPadPx

        // 边框矩形（文字块 + 内边距）
        val frameLeft = blockLeft - borderPadPx
        val frameTop = blockTop - borderPadPx
        val frameRight = blockLeft + textBlockW + borderPadPx
        val frameBottom = blockTop + textBlockH + borderPadPx

        // === 绘制金色边框 ===
        // 上下边框（比左右稍粗，仿古籍线装）
        borderPaint.strokeWidth = lineStroke * 1.5f
        drawContext.canvas.nativeCanvas.drawLine(frameLeft, frameTop, frameRight, frameTop, borderPaint)
        drawContext.canvas.nativeCanvas.drawLine(frameLeft, frameBottom, frameRight, frameBottom, borderPaint)

        // 左右边框
        borderPaint.strokeWidth = lineStroke
        drawContext.canvas.nativeCanvas.drawLine(frameLeft, frameTop, frameLeft, frameBottom, borderPaint)
        drawContext.canvas.nativeCanvas.drawLine(frameRight, frameTop, frameRight, frameBottom, borderPaint)

        // === 列间分割竖线 ===
        // 最右列中心 X（col=0 在最右，从右到左排列）
        val rightColX = blockLeft + textBlockW - fontSize / 2f
        borderPaint.strokeWidth = lineStroke
        for (col in 1 until columns) {
            val dividerX = rightColX - col * colWidth + colWidth / 2f
            drawContext.canvas.nativeCanvas.drawLine(dividerX, frameTop, dividerX, frameBottom, borderPaint)
        }

        // === 绘制竖排文字（从右到左），每列顶部对齐 ===
        for (col in 0 until columns) {
            val colText = columnTexts[col]
            val charsInThisCol = colText.length
            if (charsInThisCol == 0) continue  // 空列（段落间距）

            // 列中心 X：col=0 在最右
            val colX = rightColX - col * colWidth

            // 此列文字从顶部开始
            val startY = blockTop + fontSize

            for (row in 0 until charsInThisCol) {
                val ch = colText[row].toString()
                val charY = startY + row * charHeight
                drawContext.canvas.nativeCanvas.drawText(ch, colX, charY, textPaint)
            }
        }
    }
}
