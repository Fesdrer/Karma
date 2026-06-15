package com.example.karma.ui.history

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.local.entity.HistoryEntryEntity
import com.example.karma.data.model.ViewMode
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HistoryUiState(
    val entries: List<HistoryEntryEntity> = emptyList(),
    val viewMode: ViewMode = ViewMode.DAY,
    val aggregatedPoints: List<AggregatedPoint> = emptyList(),
    val historyLineThickness: Float = 2f,
    val historyDotRadius: Float = 3.5f,
    val message: String? = null,
)

data class AggregatedPoint(
    val timestamp: Long,
    val delta: Float,
    val event: String,
    val totalAfter: Float,
)

class HistoryViewModel(
    private val repository: KarmaRepository,
    private val application: Application,
) : ViewModel() {

    private val _viewMode = MutableStateFlow(ViewMode.DAY)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allHistory,
        repository.settings,
        _viewMode.asStateFlow(),
        _message.asStateFlow(),
    ) { entries, settings, mode, msg ->
        HistoryUiState(
            entries = entries,
            viewMode = mode,
            aggregatedPoints = aggregate(entries, mode),
            historyLineThickness = settings.historyLineThickness,
            historyDotRadius = settings.historyDotRadius,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun aggregate(
        entries: List<HistoryEntryEntity>,
        mode: ViewMode,
    ): List<AggregatedPoint> {
        if (entries.isEmpty()) return emptyList()
        if (mode == ViewMode.DAY) {
            return entries.map { it.toAggregated() }
        }

        // Group by week or month, taking the last entry per group
        val groups = mutableMapOf<String, HistoryEntryEntity>()
        for (entry in entries) {
            val cal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
            val key = when (mode) {
                ViewMode.WEEK -> {
                    val year = cal.get(Calendar.YEAR)
                    val week = cal.get(Calendar.WEEK_OF_YEAR)
                    "$year-W${week.toString().padStart(2, '0')}"
                }
                ViewMode.MONTH -> {
                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH) + 1
                    "$year-${month.toString().padStart(2, '0')}"
                }
                else -> ""
            }
            // Keep the last entry in each group (data is sorted ASC)
            groups[key] = entry
        }
        return groups.entries
            .sortedBy { it.key }
            .map { it.value.toAggregated() }
    }

    // ---- Export ----

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val csv = repository.exportCsv()
                val bom = "﻿"
                application.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write((bom + csv).toByteArray(Charsets.UTF_8))
                }
                _message.value = "CSV 导出成功"
            } catch (e: Exception) {
                _message.value = "导出失败: ${e.message}"
            }
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = repository.exportJson()
                application.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                _message.value = "JSON 导出成功"
            } catch (e: Exception) {
                _message.value = "导出失败: ${e.message}"
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = application.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: return@launch
                val format = if (uri.lastPathSegment?.endsWith(".json") == true) "json" else "csv"
                val ok = repository.importData(text, format)
                _message.value = if (ok) "导入成功" else "导入失败，文件格式不正确"
            } catch (e: Exception) {
                _message.value = "导入失败: ${e.message}"
            }
        }
    }

    // ---- Tooltip data ----

    private var lastHoveredPoint: AggregatedPoint? = null

    fun onHoveredPoint(point: AggregatedPoint?) {
        lastHoveredPoint = point
    }

    fun getTooltipData(): AggregatedPoint? = lastHoveredPoint

    class Factory(
        private val repository: KarmaRepository,
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository, application) as T
        }
    }
}

private fun HistoryEntryEntity.toAggregated() = AggregatedPoint(
    timestamp = timestamp,
    delta = delta,
    event = event,
    totalAfter = totalAfter,
)
