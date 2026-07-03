package com.example.karma.ui.main

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.ui.main.components.AxisCanvas
import com.example.karma.ui.main.components.EventPanel
import com.example.karma.ui.main.components.Footer
import com.example.karma.ui.main.components.Header
import com.example.karma.ui.main.components.ScorePanel

@Composable
fun MainScreen(
    appContainer: AppContainer,
    onNavigateToHistory: () -> Unit,
    onNavigateToPrayer: () -> Unit,
    onNavigateToDivination: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTimer: (score: Float, event: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(appContainer.repository)
    )
    val state by viewModel.uiState.collectAsState()

    // 数据就绪前不渲染任何内容，避免元素逐个出现的跳变
    val s = state ?: return

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
                    selectedScore = s.effectiveScore,
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
                    guideLineWidth = s.guideLineWidth,
                    guideLineColor = s.guideLineColor,
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
                    selectedEvent = s.selectedEvent,
                    onEventSelected = { viewModel.selectEvent(it) },
                    onCustomGoodDeedChanged = { viewModel.onCustomGoodDeedEventChanged(it) },
                    onCustomBadDeedChanged = { viewModel.onCustomBadDeedEventChanged(it) },
                    onCustomGoodResultChanged = { viewModel.onCustomGoodResultEventChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    timerEnabled = s.hasScoreAndEvent,
                    selectedScore = s.selectedScore,
                    onStartTimer = {
                        val s = viewModel.getSelectedScore() ?: return@EventPanel
                        val e = viewModel.getSelectedEvent() ?: return@EventPanel
                        onNavigateToTimer(s, e)
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
