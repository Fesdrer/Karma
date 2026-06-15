package com.example.karma.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
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

    // Chart viewport state
    val viewport = remember { ChartViewport() }

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
                // Back button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                        .clickable { onBack() }
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

            // View mode buttons (no horizontal scroll)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewMode.entries.forEach { mode ->
                    val isActive = state.viewMode == mode
                    val label = when (mode) {
                        ViewMode.DAY -> "日"
                        ViewMode.WEEK -> "周"
                        ViewMode.MONTH -> "月"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (isActive) Modifier.background(Color(0xFF4a90d9))
                                else Modifier.background(Color(0xFF1a1a3e))
                            )
                            .border(
                                1.dp,
                                if (isActive) Color(0xFF4a90d9) else Color(0xFF334444),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setViewMode(mode) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) Color.White else Color(0xFFa0c4ff),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Row 2: Export / Import buttons (right-aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1a1a3e))
                    .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                    .clickable { exportCsvLauncher.launch("karma_data.csv") }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("CSV", fontSize = 13.sp, color = Color(0xFFa0c4ff))
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1a1a3e))
                    .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                    .clickable { exportJsonLauncher.launch("karma_data.json") }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("JSON", fontSize = 13.sp, color = Color(0xFFa0c4ff))
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1a1a3e))
                    .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                    .clickable { importLauncher.launch(arrayOf("application/json", "text/csv")) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text("导入", fontSize = 13.sp, color = Color(0xFFa0c4ff))
            }
        }

        Spacer(Modifier.height(12.dp))

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
                onPointClicked = { point, screenX, screenY ->
                    if (point != null) {
                        tooltipPoint = point
                        tooltipX = screenX
                        tooltipY = screenY
                        showTooltip = true
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

                // Estimated tooltip size for bounds checking
                val tooltipWidth = 200.dp
                val tooltipHeight = 120.dp

                // Position: slightly right and above the clicked point
                val rawX = tooltipDpX + 12.dp
                val rawY = tooltipDpY - 10.dp

                // Clamp X to stay within parent bounds
                val clampedX = maxOf(4.dp, minOf(rawX, parentWidth - tooltipWidth - 4.dp))
                // If above the top edge, show below the point instead
                val clampedY = if (rawY < 0.dp) tooltipDpY + 10.dp else rawY
                // Clamp Y to stay within parent bounds
                val finalY = minOf(maxOf(4.dp, clampedY), parentHeight - tooltipHeight - 4.dp)

                Box(
                    modifier = Modifier
                        .offset(x = clampedX, y = finalY),
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
