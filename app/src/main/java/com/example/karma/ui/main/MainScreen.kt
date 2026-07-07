package com.example.karma.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.ui.main.components.AxisCanvas
import com.example.karma.ui.main.components.EventPanel
import com.example.karma.ui.main.components.Footer
import com.example.karma.ui.main.components.Header
import com.example.karma.ui.main.components.ScorePanel
import com.example.karma.ui.timer.TimerService
import kotlin.math.round
import kotlinx.coroutines.launch

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
    val context = LocalContext.current

    // 数据就绪时从纯黑渐变到主页面
    Crossfade(targetState = state != null, animationSpec = tween(300)) { ready ->
        if (!ready) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            return@Crossfade
        }
        val s = state!!

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
                    selectedScore = s.effectiveScore,
                    dailyMustDoDeedNames = s.dailyMustDoDeedNames,
                    dailyMustDoDoneToday = s.dailyMustDoDoneToday,
                    onStopTimer = { elapsedMs ->
                        val ts = TimerService.timerState.value
                        val totMin = elapsedMs / 60000.0
                        val delta = round(totMin / 60.0 * ts.selectedScore * 2.0) / 2.0
                        viewModel.viewModelScope.launch {
                            appContainer.repository.addHistoryEntry(delta.toFloat(), ts.selectedEvent, "record")
                        }
                        TimerService.stop(context)
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
}
