package com.example.karma.ui.prayer

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.ui.components.DialogEntranceContainer
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.ScoreBtnBg

@Composable
fun PrayerScreen(
    appContainer: AppContainer,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: PrayerViewModel = viewModel(
        factory = PrayerViewModel.Factory(appContainer.repository)
    )
    val state by viewModel.uiState.collectAsState()

    // Show error as toast
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Particle canvas (full screen, no pointer events)
        if (state.isAnimating) {
            ParticleEngineCanvas(
                amount = state.amount.toFloatOrNull() ?: 0f,
                purpose = state.purpose,
                rankLevel = state.rankLevel,
                totalRanks = state.totalRanks,
                onComplete = {
                    onComplete()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Prayer form (centered)
        if (state.showForm) {
            DialogEntranceContainer(maskAlpha = 0f) {
                // 窗口高度不超过屏幕，内容超高时窗口内部滚动，确认祈福按钮始终可达
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1A1A1A))
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp)
                        .fillMaxWidth(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "🙏 祈福",
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFffd700),
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "诚心祈福，扣减业力值",
                        fontFamily = FontFamily.Serif,
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                    )
                    Spacer(Modifier.height(20.dp))

                    // Amount input
                    Text(
                        text = "扣减分数",
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = { viewModel.onAmountChanged(it) },
                        placeholder = { Text("输入正数...", fontFamily = FontFamily.Serif, color = Color(0xFF666666)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        textStyle = TextStyle(color = Color(0xFFe0e0e0), fontFamily = FontFamily.Serif, fontSize = 16.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFffd700),
                            unfocusedBorderColor = BorderSubtle,
                            cursorColor = Color(0xFFffd700),
                            focusedContainerColor = ScoreBtnBg,
                            unfocusedContainerColor = ScoreBtnBg,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))

                    // Purpose input with bracket decoration
                    Text(
                        text = "祈福目的",
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    // 括号对角布局：「左上角 」右下角
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .background(ScoreBtnBg),
                    ) {
                        Text(
                            text = "「",
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFff0000),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 8.dp, top = 4.dp),
                        )
                        OutlinedTextField(
                            value = state.purpose,
                            onValueChange = { viewModel.onPurposeChanged(it) },
                            placeholder = { Text("输入祈福内容...", fontFamily = FontFamily.Serif, color = Color(0xFF666666)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            textStyle = TextStyle(color = Color(0xFFff0000), fontFamily = FontFamily.Serif, fontSize = 16.sp),
                            minLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Color(0xFFff0000),
                                focusedTextColor = Color(0xFFff0000),
                                unfocusedTextColor = Color(0xFFff0000),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                        )
                        Text(
                            text = "」",
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFff0000),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 8.dp, bottom = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // ———— 神明切换 ————
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "添加神明",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFffd700),
                        )
                        Switch(
                            checked = state.showDeityInput,
                            onCheckedChange = { viewModel.onToggleDeityInput() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFffd700),
                                checkedTrackColor = Color(0xFFffd700).copy(alpha = 0.3f),
                            ),
                        )
                    }

                    if (state.showDeityInput) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "神明",
                            fontFamily = FontFamily.Serif,
                            fontSize = 13.sp,
                            color = Color(0xFF888888),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.deity,
                            onValueChange = { viewModel.onDeityChanged(it) },
                            placeholder = { Text("输入神明名称...", fontFamily = FontFamily.Serif, color = Color(0xFF666666)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            textStyle = TextStyle(color = Color(0xFFff0000), fontFamily = FontFamily.Serif, fontSize = 16.sp),
                            minLines = 1,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFffd700),
                                unfocusedBorderColor = BorderSubtle,
                                cursorColor = Color(0xFFff0000),
                                focusedContainerColor = ScoreBtnBg,
                                unfocusedContainerColor = ScoreBtnBg,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // ———— 神秘学符号画板（v4.2）：仿「添加神明」开关，画作仅自赏不入记录 ————
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "绘制神秘符号",
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFffd700),
                        )
                        Switch(
                            checked = state.showSymbolBoard,
                            onCheckedChange = { viewModel.toggleSymbolBoard() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFffd700),
                                checkedTrackColor = Color(0xFFffd700).copy(alpha = 0.3f),
                            ),
                        )
                    }

                    if (state.showSymbolBoard) {
                        Spacer(Modifier.height(12.dp))
                        SymbolDrawingBoard(
                            strokes = state.symbolStrokes,
                            currentStroke = state.currentStroke,
                            onStrokeStart = { viewModel.startSymbolStroke(it) },
                            onStrokeMove = { viewModel.addSymbolPoint(it) },
                            onStrokeEnd = { viewModel.endSymbolStroke() },
                            onClear = { viewModel.clearSymbolStrokes() },
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onComplete,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF333333),
                                contentColor = Color(0xFF888888),
                            ),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text("取消", fontFamily = FontFamily.Serif, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.confirmPrayer() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFb8860b),
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text("确认祈福", fontFamily = FontFamily.Serif, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// SymbolDrawingBoard — 神秘学符号手绘画板（v4.2）
// 笔画坐标已由调用方归一化到 0~1；此处按画板实际尺寸等比缩放绘制。
// 画作仅供本次祈福自赏：不入事件记录、不持久化。
// ============================================================

@Composable
private fun SymbolDrawingBoard(
    strokes: List<List<Offset>>,
    currentStroke: List<Offset>,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 画板高度 ≈ 弹窗可视区一半以上（弹窗整体可滚动，超高时内部滚动）
    val boardHeight = with(LocalConfiguration.current) { (screenHeightDp * 0.5f).dp }
    val hasContent = strokes.isNotEmpty() || currentStroke.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(boardHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(ScoreBtnBg)
                .border(1.dp, Color(0xFFffd700).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    // pointerInput scope 自带 size（画板实际像素尺寸），把手指坐标归一化到 0~1
                    fun normalize(x: Float, y: Float): Offset {
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        return Offset(x / w, y / h)
                    }
                    detectDragGestures(
                        onDragStart = { off -> onStrokeStart(normalize(off.x, off.y)) },
                        onDrag = { change, _ -> onStrokeMove(normalize(change.position.x, change.position.y)) },
                        onDragEnd = { onStrokeEnd() },
                        onDragCancel = { onStrokeEnd() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas
                val strokeWidth = (minOf(w, h) * 0.012f).coerceIn(3f, 14f)
                val penColor = Color(0xFFFF0000)

                fun drawStrokePath(pts: List<Offset>) {
                    if (pts.size < 2) return
                    val path = Path()
                    pts.forEachIndexed { i, p ->
                        val x = p.x * w
                        val y = p.y * h
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = penColor,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
                strokes.forEach { drawStrokePath(it) }
                drawStrokePath(currentStroke)
            }

            if (!hasContent) {
                Text(
                    text = "（手指在此绘制神秘符号）",
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 清空按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onClear) {
                Text("清空", fontFamily = FontFamily.Serif, fontSize = 13.sp, color = Color(0xFFffd700))
            }
        }
    }
}
