package com.example.karma.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.data.model.ViewMode
import com.example.karma.di.AppContainer
import com.example.karma.ui.components.ChartTooltip

@Composable
fun HistoryScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(appContainer.repository, context.applicationContext as android.app.Application)
    )
    val state by viewModel.uiState.collectAsState()

    // Tooltip state
    var tooltipPoint by remember { mutableStateOf<AggregatedPoint?>(null) }
    var showTooltip by remember { mutableStateOf(false) }
    var tooltipX by remember { mutableStateOf(0f) }
    var tooltipY by remember { mutableStateOf(0f) }

    // File picker launchers
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? -> uri?.let { viewModel.exportCsv(it) } }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { viewModel.exportJson(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importData(it) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        // Row 1: Back + Title (left) | View mode buttons (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button (with debounce to prevent rapid double-pop)
                var backHandled by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                        .clickable {
                            if (!backHandled) {
                                backHandled = true
                                onBack()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text("← 返回", fontSize = 14.sp, color = Color(0xFF888888))
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "历史记录",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFffd700),
                )
            }

            // View mode dropdown（日/周/月/全部）
            var viewModeExpanded by remember { mutableStateOf(false) }
            val currentViewLabel = when (state.viewMode) {
                ViewMode.DAY -> "日"
                ViewMode.WEEK -> "周"
                ViewMode.MONTH -> "月"
                ViewMode.ALL -> "全部"
            }
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1a1a3e))
                        .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                        .clickable { viewModeExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text(
                        "$currentViewLabel ▼",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFa0c4ff),
                    )
                }

                DropdownMenu(
                    expanded = viewModeExpanded,
                    onDismissRequest = { viewModeExpanded = false },
                ) {
                    ViewMode.entries.forEach { mode ->
                        val itemLabel = when (mode) {
                            ViewMode.DAY -> "日"
                            ViewMode.WEEK -> "周"
                            ViewMode.MONTH -> "月"
                            ViewMode.ALL -> "全部"
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    itemLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (mode == state.viewMode) FontWeight.Bold else FontWeight.Normal,
                                    color = if (mode == state.viewMode) Color(0xFF4a90d9) else Color.White,
                                )
                            },
                            onClick = {
                                viewModel.setViewMode(mode)
                                viewModeExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Row 2: 导航栏 (左) + 导出/导入 (右)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── 左半：导航控件 ──
            if (state.viewMode != ViewMode.ALL) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ◀ 按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1a1a3e))
                            .border(1.dp, Color(0xFF334444), RoundedCornerShape(6.dp))
                            .clickable { viewModel.navigatePrevious() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) { Text("<", fontSize = 14.sp, color = Color(0xFFa0c4ff)) }

                    Spacer(Modifier.width(6.dp))

                    // 日期标签
                    Text(
                        text = state.dateLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    Spacer(Modifier.width(6.dp))

                    // ▶ 按钮
                    val forwardBg = if (state.canGoForward) Color(0xFF1a1a3e)
                    else Color(0xFF1a1a3e).copy(alpha = 0.4f)
                    val forwardBorder = if (state.canGoForward) Color(0xFF334444)
                    else Color(0xFF334444).copy(alpha = 0.2f)
                    val forwardColor = if (state.canGoForward) Color(0xFFa0c4ff)
                    else Color(0xFF666666)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(forwardBg)
                            .border(1.dp, forwardBorder, RoundedCornerShape(6.dp))
                            .clickable(enabled = state.canGoForward) { viewModel.navigateNext() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) { Text(">", fontSize = 14.sp, color = forwardColor) }

                    Spacer(Modifier.width(6.dp))

                    // [今天] 按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1a1a3e))
                            .border(1.dp, Color(0xFF334444), RoundedCornerShape(6.dp))
                            .clickable { viewModel.resetFocusToToday() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) { Text("今天", fontSize = 11.sp, color = Color(0xFFa0c4ff)) }

                    Spacer(Modifier.width(8.dp))

                    // [放大] 开关
                    val zoomDisabled = state.viewMode == ViewMode.DAY
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (state.isZoomEnabled) Color(0xFF4a90d9) else Color(0xFF1a1a3e)
                            )
                            .border(
                                1.dp,
                                when {
                                    state.isZoomEnabled -> Color(0xFF4a90d9)
                                    zoomDisabled -> Color(0xFF334444).copy(alpha = 0.2f)
                                    else -> Color(0xFF334444)
                                },
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(enabled = !zoomDisabled) { viewModel.toggleZoom() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "放大", fontSize = 11.sp,
                            color = when {
                                zoomDisabled -> Color(0xFF666666)
                                state.isZoomEnabled -> Color.White
                                else -> Color(0xFFa0c4ff)
                            },
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // [缩小] 按钮
                    val zoomOutDisabled = state.viewMode == ViewMode.ALL
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1a1a3e))
                            .border(
                                1.dp,
                                if (zoomOutDisabled) Color(0xFF334444).copy(alpha = 0.2f)
                                else Color(0xFF334444),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(enabled = !zoomOutDisabled) { viewModel.zoomOut() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "缩小", fontSize = 11.sp,
                            color = if (zoomOutDisabled) Color(0xFF666666) else Color(0xFFa0c4ff),
                        )
                    }
                }
            } else {
                // ALL 模式：仅有放大按钮，缩小禁用
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (state.isZoomEnabled) Color(0xFF4a90d9)
                                else Color(0xFF1a1a3e)
                            )
                            .border(
                                1.dp,
                                if (state.isZoomEnabled) Color(0xFF4a90d9) else Color(0xFF334444),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable { viewModel.toggleZoom() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "放大", fontSize = 11.sp,
                            color = if (state.isZoomEnabled) Color.White else Color(0xFFa0c4ff),
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1a1a3e))
                            .border(
                                1.dp,
                                Color(0xFF334444).copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text("缩小", fontSize = 11.sp, color = Color(0xFF666666))
                    }
                }
            }

            // ── 右半：导出/导入下拉菜单 ──
            var exportExpanded by remember { mutableStateOf(false) }
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1a1a3e))
                        .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                        .clickable { exportExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                ) {
                    Text("导出 ▼", fontSize = 13.sp, color = Color(0xFFa0c4ff))
                }

                DropdownMenu(
                    expanded = exportExpanded,
                    onDismissRequest = { exportExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("CSV", fontSize = 13.sp, color = Color.White) },
                        onClick = {
                            exportExpanded = false
                            exportCsvLauncher.launch("karma_data.csv")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("JSON", fontSize = 13.sp, color = Color.White) },
                        onClick = {
                            exportExpanded = false
                            exportJsonLauncher.launch("karma_data.json")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("导入", fontSize = 13.sp, color = Color.White) },
                        onClick = {
                            exportExpanded = false
                            importLauncher.launch(arrayOf("application/json", "text/csv"))
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Chart viewport state — 每个 viewMode 独立实例
        val viewport = remember(state.viewMode, state.focusDate) { ChartViewport() }

        // Chart
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0d1b2a)),
        ) {
            val parentWidth = maxWidth
            val parentHeight = maxHeight

            HistoryChartCanvas(
                points = state.aggregatedPoints,
                viewport = viewport,
                lineThickness = state.historyLineThickness,
                dotRadius = state.historyDotRadius,
                rankColors = state.rankColors,
                onPointClicked = { point, screenX, screenY ->
                    if (point != null) {
                        if (state.isZoomEnabled && state.viewMode != ViewMode.DAY) {
                            // 缩放模式：下钻，不显示 tooltip
                            viewModel.zoomToPoint(point.timestamp)
                            showTooltip = false
                            tooltipPoint = null
                        } else {
                            // 普通模式：显示 tooltip
                            tooltipPoint = point
                            tooltipX = screenX
                            tooltipY = screenY
                            showTooltip = true
                        }
                    } else {
                        showTooltip = false
                        tooltipPoint = null
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Tooltip overlay - positioned near the clicked point with bounds checking
            if (showTooltip && tooltipPoint != null) {
                val density = LocalDensity.current
                val tooltipDpX = with(density) { tooltipX.toDp() }
                val tooltipDpY = with(density) { tooltipY.toDp() }

                // Fixed tooltip max width (matches ChartTooltip's widthIn)
                val tooltipMaxWidth = 220.dp
                val tooltipMaxHeight = 200.dp

                // Try placing right of the point; if it overflows, place left instead
                val rawX = tooltipDpX + 12.dp
                val finalX = if (rawX + tooltipMaxWidth > parentWidth) {
                    maxOf(4.dp, tooltipDpX - tooltipMaxWidth - 12.dp)
                } else {
                    maxOf(4.dp, rawX)
                }

                // Try placing above the point; if it overflows, place below instead
                val rawY = tooltipDpY - 10.dp
                val finalY = if (rawY < 4.dp) {
                    minOf(tooltipDpY + 10.dp, parentHeight - tooltipMaxHeight - 4.dp)
                } else {
                    maxOf(4.dp, minOf(rawY, parentHeight - tooltipMaxHeight - 4.dp))
                }

                Box(
                    modifier = Modifier
                        .offset(x = finalX, y = finalY),
                ) {
                    ChartTooltip(point = tooltipPoint)
                }
            }
        }
    }

    // Show message as snackbar placeholder
    LaunchedEffect(state.message) {
        state.message?.let {
            viewModel.clearMessage()
        }
    }

    // Hide tooltip when data changes (e.g., view mode switch)
    LaunchedEffect(state.viewMode) {
        showTooltip = false
        tooltipPoint = null
    }
}
