package com.example.karma.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.ui.components.EventEditModal
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
    modifier: Modifier = Modifier,
) {
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.Factory(appContainer.repository)
    )
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
    ) {
        // Header
        Header(
            totalScore = state.totalScore,
            rank = state.rank,
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
                selectedScore = state.selectedScore,
                onScoreSelected = { viewModel.selectScore(it) },
                onCustomScoreChanged = { viewModel.onCustomScoreChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )

            Spacer(Modifier.width(6.dp))

            // Center panel: Axis
            AxisCanvas(
                totalScore = state.totalScore,
                modifier = Modifier
                    .width(75.dp)
                    .fillMaxHeight(),
            )

            Spacer(Modifier.width(6.dp))

            // Right panel: Events
            EventPanel(
                eventPresets = state.eventPresets,
                selectedEvent = state.selectedEvent,
                onEventSelected = { viewModel.selectEvent(it) },
                onEditClick = { viewModel.openEventEdit() },
                onCustomEventChanged = { viewModel.onCustomEventChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Footer
        Footer(
            confirmEnabled = state.selectedScore != null && state.selectedEvent != null,
            prayerEnabled = state.totalScore >= 30f,
            onConfirm = { viewModel.onConfirm() },
            onPrayer = onNavigateToPrayer,
            onHistory = onNavigateToHistory,
        )
    }

    // Event Edit Modal
    if (state.eventEditMode) {
        EventEditModal(
            initialPresets = state.eventPresets,
            onDismiss = { viewModel.closeEventEdit() },
            onSave = { viewModel.saveEventPresets(it) },
        )
    }
}
