package com.example.karma.ui.divination

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.ui.divination.components.PalacePosition
import com.example.karma.ui.divination.components.YarrowCanvas
import com.example.karma.ui.divination.components.YarrowResultPanel
import com.example.karma.ui.divination.components.XiaoLiuRenInputPanel
import com.example.karma.ui.divination.components.XiaoLiuRenPillarCanvas
import com.example.karma.ui.divination.components.XiaoLiuRenResultPanel
import com.example.karma.ui.divination.components.XiaoLiuRenThreadCanvas
import com.example.karma.ui.divination.model.ShiChen
import kotlin.random.Random

@Composable
fun DivinationScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 气运测试状态（tab 0）
    var luckResult by remember { mutableStateOf<Int?>(null) }
    var backHandled by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // 小六壬 ViewModel（tab 2）
    val xlrViewModel: DivinationViewModel = viewModel(
        factory = DivinationViewModel.Factory()
    )
    val xlrState by xlrViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 大衍筮法 ViewModel（tab 1）
    val yarrowViewModel: YarrowViewModel = viewModel(
        factory = YarrowViewModel.Factory()
    )
    val yarrowState by yarrowViewModel.uiState.collectAsState()

    // 小六壬错误 Toast
    LaunchedEffect(xlrState.errorMessage) {
        xlrState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            xlrViewModel.clearError()
        }
    }

    val tabs = listOf("气运测试", "大衍筮法", "小六壬")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 32.dp)
                .clickable {
                    if (!backHandled) {
                        backHandled = true
                        onBack()
                    }
                },
        ) {
            Text(
                text = "← 返回",
                fontSize = 18.sp,
                color = Color(0xFFa0c4ff),
                modifier = Modifier.padding(12.dp),
            )
        }

        // Center content — varies by tab
        when (selectedTab) {
            0 -> {
                // 气运测试 — 保持原样
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = luckResult?.toString() ?: "?",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFffd700),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            var cnt = 0
                            for (i in 1..1000) {
                                if (Random.nextInt(1, 1001) <= 490) {
                                    cnt++
                                }
                            }
                            luckResult = cnt
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4a90d9),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = if (luckResult == null) "开始" else "再来一次",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            1 -> {
                // 大衍筮法
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    YarrowCanvas(
                        state = yarrowState,
                        onUserTap = yarrowViewModel::onSplitTap,
                        onPhaseComplete = yarrowViewModel::advancePhase,
                        modifier = Modifier.fillMaxSize().padding(bottom = 52.dp),
                    )
                    if (yarrowState.showResult && yarrowState.result != null) {
                        YarrowResultPanel(
                            result = yarrowState.result!!,
                            onRetry = yarrowViewModel::reset,
                        )
                    }
                }
            }
            2 -> {
                // 小六壬 — 完整功能
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
                    onRetry = xlrViewModel::reset,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 52.dp),
                )
            }
        }

        // Bottom tab bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF16213e))
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
                            if (isSelected) Color(0xFF4a90d9)
                            else Color(0xFF1a1a3e)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF4a90d9) else Color(0xFF334444),
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFFa0c4ff),
                        textAlign = TextAlign.Center,
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

    Box(modifier = modifier.background(Color.Black)) {

        // 层 1：三柱六宫 Canvas（始终显示在背景）
        XiaoLiuRenPillarCanvas(
            onPalacePositionsReady = { positions ->
                palacePositions.clear()
                palacePositions.addAll(positions)
            },
        )

        // 层 2：金线动画 Canvas（动画阶段显示）
        if (state.isAnimating && state.fullPath.isNotEmpty() && palacePositions.size == 6) {
            XiaoLiuRenThreadCanvas(
                fullPath = state.fullPath,
                palacePositions = palacePositions.toList(),
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
