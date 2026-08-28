package com.example.karma.ui.divination

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.ui.components.BackButton
import com.example.karma.ui.divination.components.PalacePosition
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.divination.components.YarrowCanvas
import com.example.karma.ui.divination.components.YarrowResultPanel
import com.example.karma.ui.divination.components.XiaoLiuRenInputPanel
import com.example.karma.ui.divination.components.XiaoLiuRenPillarCanvas
import com.example.karma.ui.divination.components.XiaoLiuRenResultPanel
import com.example.karma.ui.divination.components.XiaoLiuRenThreadCanvas
import com.example.karma.ui.divination.model.ShiChen
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DivinationScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 气运测试状态（tab 0）
    var luckResult by remember { mutableStateOf<Int?>(null) }
    var luckComputing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // 占卜输入面板状态（小六壬和大衍筮法各自独立）
    var divinationTopic by remember { mutableStateOf("") }
    var divinationCost by remember { mutableStateOf("") }
    var xlrReady by remember { mutableStateOf(false) }
    var yarrowReady by remember { mutableStateOf(false) }
    var xlrRecorded by remember { mutableStateOf(false) }
    var yarrowRecorded by remember { mutableStateOf(false) }

    // 小六壬 ViewModel（tab 2）
    val xlrViewModel: DivinationViewModel = viewModel(
        factory = DivinationViewModel.Factory()
    )
    val xlrState by xlrViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 大衍筮法 ViewModel（在 tab 1 内部懒加载，避免影响其他 tab）

    // 小六壬错误 Toast
    LaunchedEffect(xlrState.errorMessage) {
        xlrState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            xlrViewModel.clearError()
        }
    }

    // 占卜资格由每日次数限制控制（不再要求 3 阶解锁）
    val settings by appContainer.repository.settings.collectAsState(initial = null)
    val divinationRemaining = settings?.let { s ->
        appContainer.repository.getDivinationRemaining(s)
    } ?: 0
    // 当前阶位的占卜上限（上限为 0 表示该阶位无法占卜，与「次数用完」页面区分开）
    val divinationLimit = settings?.let { s ->
        appContainer.repository.getDivinationLimit(s)
    } ?: 0

    val tabs = listOf("气运测试", "大衍筮法", "小六壬")

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // Back button（大衍筮法 tab 不显示）
        if (selectedTab != 1) {
            BackButton(
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 32.dp),
                label = "← 返回",
                labelColor = Color(0xFFa0c4ff),
                bgColor = Color.Transparent,
            )
        }

        // Center content — varies by tab, with crossfade transition
        Crossfade(targetState = selectedTab, animationSpec = tween(300)) { tab ->
            when (tab) {
                0 -> {
                // 气运测试 — 保持原样
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 72.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = luckResult?.toString() ?: "?",
                        fontFamily = FontFamily.Serif,
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFffd700),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            luckComputing = true
                            scope.launch(Dispatchers.Default) {
                                var cnt = 0
                                for (i in 1..1000) {
                                    if (Random.nextInt(1, 1001) <= 490) {
                                        cnt++
                                    }
                                }
                                luckResult = cnt
                                luckComputing = false
                            }
                        },
                        enabled = !luckComputing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFb8860b),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = if (luckResult == null) "开始" else "再来一次",
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            1 -> {
                if (settings == null) {
                    // 数据未就绪：不渲染，避免剩余次数 0 导致锁定画面首帧闪现
                } else if (!yarrowReady && divinationRemaining <= 0) {
                    // 大衍筮法：今日次数用完（上限为 0 时显示「该阶位无法占卜」）
                    DivinationLockedOverlay(title = "大衍筮法", noQuota = divinationLimit <= 0)
                } else if (!yarrowReady) {
                    // 大衍筮法：先输入占卜事情和扣除分数
                    DivinationInputOverlay(
                        topic = divinationTopic,
                        cost = divinationCost,
                        remaining = divinationRemaining,
                        onTopicChange = { divinationTopic = it },
                        onCostChange = { divinationCost = it },
                        onDivination = {
                            scope.launch { appContainer.repository.recordDivination() }
                            yarrowReady = true
                        },
                    )
                } else {
                // 大衍筮法 — ViewModel 在此内部创建，切换 tab 后自动释放
                val yv: YarrowViewModel = viewModel(factory = YarrowViewModel.Factory())
                val ys by yv.uiState.collectAsState()

                // 占卜完成自动记录
                LaunchedEffect(ys.showResult) {
                    if (ys.showResult && !yarrowRecorded) {
                        val cost = divinationCost.toFloatOrNull() ?: return@LaunchedEffect
                        appContainer.repository.addHistoryEntry(-cost, "占卜：${divinationTopic}", "divination")
                        yarrowRecorded = true
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    YarrowCanvas(
                        state = ys,
                        onUserTap = { num ->
                            if (ys.phase == YarrowPhase.WAITING) yv.startDivination()
                            else yv.onSplitTap(num)
                        },
                        onPhaseComplete = yv::advancePhase,
                        modifier = Modifier.fillMaxSize().padding(bottom = 52.dp).navigationBarsPadding(),
                    )
                    // 十八变完成后，显示神圣"查看启示"按钮
                    if (ys.phase == YarrowPhase.REVELATION_READY && ys.result != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "✨ 大衍之数五十，其用四十有九 ✨",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { yv.showRevelation() },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFb8860b),
                                        contentColor = Color.White,
                                    ),
                                    modifier = Modifier.height(56.dp).padding(horizontal = 16.dp),
                                ) {
                                    Text(
                                        "🔮 查看启示",
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    if (ys.showResult && ys.result != null) {
                        YarrowResultPanel(
                            result = ys.result!!,
                            onRetry = {
                                yv.reset()
                                yarrowReady = false
                                yarrowRecorded = false
                            },
                        )
                    }
                }
                } // end else yarrowReady
            }
            2 -> {
                if (settings == null) {
                    // 数据未就绪：不渲染，避免剩余次数 0 导致锁定画面首帧闪现
                } else if (!xlrReady && divinationRemaining <= 0) {
                    // 小六壬：今日次数用完（上限为 0 时显示「该阶位无法占卜」）
                    DivinationLockedOverlay(title = "小六壬", noQuota = divinationLimit <= 0)
                } else if (!xlrReady) {
                    // 小六壬：先输入占卜事情和扣除分数
                    DivinationInputOverlay(
                        topic = divinationTopic,
                        cost = divinationCost,
                        remaining = divinationRemaining,
                        onTopicChange = { divinationTopic = it },
                        onCostChange = { divinationCost = it },
                        onDivination = {
                            scope.launch { appContainer.repository.recordDivination() }
                            xlrReady = true
                        },
                    )
                } else {
                // 小六壬 — 完整功能

                // 占卜完成自动记录
                LaunchedEffect(xlrState.animationPhase) {
                    if (xlrState.animationPhase == AnimationPhase.COMPLETE && !xlrRecorded) {
                        val cost = divinationCost.toFloatOrNull() ?: return@LaunchedEffect
                        appContainer.repository.addHistoryEntry(-cost, "占卜：${divinationTopic}", "divination")
                        xlrRecorded = true
                    }
                }

                XiaoLiuRenContent(
                    state = xlrState,
                    onInputModeChanged = xlrViewModel::setInputMode,
                    onMonthChanged = xlrViewModel::setMonth,
                    onDayChanged = xlrViewModel::setDay,
                    onShiChenChanged = xlrViewModel::setShiChen,
                    onNumber1Changed = xlrViewModel::setNumber1,
                    onNumber2Changed = xlrViewModel::setNumber2,
                    onNumber3Changed = xlrViewModel::setNumber3,
                    onStartClick = xlrViewModel::startDivination,
                    onPhaseComplete = xlrViewModel::onPhaseComplete,
                    onRetry = {
                        xlrViewModel.reset()
                        xlrReady = false
                        xlrRecorded = false
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 52.dp)
                        .navigationBarsPadding(),
                )
                } // end else xlrReady
            }
            } // end when
        } // end Crossfade

        // Bottom tab bar（底部导航栏存在时整体上移避开系统栏）
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Gold.copy(alpha = 0.2f)
                            else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (isSelected) Gold else Color(0xFF334444),
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Gold else Color(0xFFa0c4ff),
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                    )
                }
                if (index < tabs.size - 1) {
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}

/**
 * 占卜输入覆盖层：输入占卜事情 + 扣除分数，确认后进入占卜。
 * 每日次数限制：remaining <= 0 时拦截（气运测试 tab 不经过此覆盖层，天然不计入）。
 */
@Composable
private fun DivinationInputOverlay(
    topic: String,
    cost: String,
    remaining: Int,
    onTopicChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onDivination: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0a0a0f)),
        contentAlignment = Alignment.Center,
    ) {
        // 窗口高度不超过屏幕（Box 约束），内容超高时窗口内部滚动，确认按钮始终可达
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1a1a2e))
                .border(1.dp, Color(0xFFb8860b).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔮 占卜", fontSize = 24.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = Color(0xFFffd700))
            Spacer(Modifier.height(20.dp))
            Text("占卜的事情", fontSize = 13.sp, fontFamily = FontFamily.Serif, color = Color(0xFF888888), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = topic,
                onValueChange = onTopicChange,
                placeholder = { Text("输入占卜事情...", fontFamily = FontFamily.Serif, color = Color(0xFF666666)) },
                textStyle = TextStyle(color = Color(0xFFff0000), fontFamily = FontFamily.Serif, fontSize = 16.sp),
                singleLine = false, minLines = 2, maxLines = 4,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFffd700), unfocusedBorderColor = Color(0xFF334444),
                    cursorColor = Color(0xFFff0000), focusedContainerColor = Color(0xFF111122), unfocusedContainerColor = Color(0xFF111122),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text("本次扣除业力分数", fontSize = 13.sp, fontFamily = FontFamily.Serif, color = Color(0xFF888888), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = cost,
                onValueChange = onCostChange,
                placeholder = { Text("输入正数...", fontFamily = FontFamily.Serif, color = Color(0xFF666666)) },
                textStyle = TextStyle(color = Color(0xFFe0e0e0), fontFamily = FontFamily.Serif, fontSize = 16.sp),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFffd700), unfocusedBorderColor = Color(0xFF334444),
                    cursorColor = Color(0xFFe0e0e0), focusedContainerColor = Color(0xFF111122), unfocusedContainerColor = Color(0xFF111122),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "今日剩余占卜次数：$remaining",
                fontSize = 13.sp,
                color = if (remaining <= 0) Color(0xFFff5252) else Color(0xFF888888),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (remaining <= 0) {
                        Toast.makeText(context, "今日占卜次数已用完，请明天再来", Toast.LENGTH_SHORT).show()
                    } else if (topic.isBlank()) {
                        Toast.makeText(context, "请输入占卜的事情", Toast.LENGTH_SHORT).show()
                    } else if (cost.toFloatOrNull() == null || cost.toFloat() <= 0f) {
                        Toast.makeText(context, "请输入有效的业力分数（正数）", Toast.LENGTH_SHORT).show()
                    } else {
                        onDivination()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb8860b), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("开始占卜", fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 占卜锁定画面：
 * - noQuota=true（当前阶位占卜上限为 0）→ 显示「该阶位无法占卜」
 * - noQuota=false（上限 > 0 但今日次数用完）→ 显示「今日占卜次数已用完」
 */
@Composable
private fun DivinationLockedOverlay(title: String, noQuota: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0a0a0f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔒", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (noQuota) {
                    "「$title」该阶位无法占卜"
                } else {
                    "「$title」今日占卜次数已用完"
                },
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFffd700),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (noQuota) {
                    "当前阶位的占卜上限为 0"
                } else {
                    "请明天再来"
                },
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = Color(0xFF888888),
            )
        }
    }
}

/**
 * 小六壬内容区：四层叠加（柱子 → 金线 → 输入 → 结果）
 */
@Composable
private fun XiaoLiuRenContent(
    state: DivinationUiState,
    onInputModeChanged: (InputMode) -> Unit,
    onMonthChanged: (Int) -> Unit,
    onDayChanged: (Int) -> Unit,
    onShiChenChanged: (ShiChen) -> Unit,
    onNumber1Changed: (String) -> Unit,
    onNumber2Changed: (String) -> Unit,
    onNumber3Changed: (String) -> Unit,
    onStartClick: () -> Unit,
    onPhaseComplete: (AnimationPhase) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palacePositions = remember { mutableStateListOf<PalacePosition>() }

    Box(modifier = Modifier.fillMaxSize()) {  // 全屏，不受底 padding 约束

        // 层 1：三柱六宫 Canvas — 全屏，图片贴到屏幕最底部
        XiaoLiuRenPillarCanvas(
            onPalacePositionsReady = { positions ->
                palacePositions.clear()
                palacePositions.addAll(positions)
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 层 2-4：受底部 tab bar padding 约束
        Box(modifier = modifier) {
            // 层 2：金线动画 Canvas（动画阶段显示）
            if (state.isAnimating && state.fullPath.isNotEmpty() && palacePositions.size == 6) {
                XiaoLiuRenThreadCanvas(
                    fullPath = state.fullPath,
                    palacePositions = palacePositions,  // 直接传递避免每次重组 .toList() 新建列表
                    animationPhase = state.animationPhase,
                    monthCount = state.calcMonth,
                    dayCount = state.calcDay,
                    hourCount = state.calcHour,
                    onPhaseComplete = onPhaseComplete,
                )
            }

            // 层 3：输入面板（IDLE 阶段显示）
            if (state.animationPhase == AnimationPhase.IDLE) {
                XiaoLiuRenInputPanel(
                    inputMode = state.inputMode,
                    month = state.month,
                    day = state.day,
                    shiChen = state.shiChen,
                    number1 = state.number1,
                    number2 = state.number2,
                    number3 = state.number3,
                    onInputModeChanged = onInputModeChanged,
                    onMonthChanged = onMonthChanged,
                    onDayChanged = onDayChanged,
                    onShiChenChanged = onShiChenChanged,
                    onNumber1Changed = onNumber1Changed,
                    onNumber2Changed = onNumber2Changed,
                    onNumber3Changed = onNumber3Changed,
                    onStartClick = onStartClick,
                    enabled = !state.isAnimating,
                )
            }

            // 层 4：结果面板（COMPLETE 阶段显示）
            if (state.animationPhase == AnimationPhase.COMPLETE && state.resultPalace != null) {
                XiaoLiuRenResultPanel(
                    result = state.resultPalace!!,
                    onRetry = onRetry,
                )
            }
        }
    }
}
