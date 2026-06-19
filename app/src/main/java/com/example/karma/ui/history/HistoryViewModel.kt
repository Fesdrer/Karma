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
    val focusDate: Long = System.currentTimeMillis(),
    val canGoForward: Boolean = false,
    val dateLabel: String = "",
    val isZoomEnabled: Boolean = false,
    val rankColors: List<Long> = emptyList(),
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
    private val _focusDate = MutableStateFlow(System.currentTimeMillis())
    private val _isZoomEnabled = MutableStateFlow(false)

    // combine 最多支持 5 个类型安全参数，因此先合并 _focusDate + _isZoomEnabled
    private val _focusState = combine(
        _focusDate, _isZoomEnabled
    ) { date, zoom -> Pair(date, zoom) }

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.allHistory,
        repository.settings,
        _viewMode.asStateFlow(),
        _message.asStateFlow(),
        _focusState,
    ) { entries: List<HistoryEntryEntity>, settings: com.example.karma.data.local.entity.KarmaSettingsEntity, mode: ViewMode, msg: String?, focusPair: Pair<Long, Boolean> ->
        val focusDate = focusPair.first
        val zoomEnabled = focusPair.second
        HistoryUiState(
            entries = entries,
            viewMode = mode,
            aggregatedPoints = aggregate(entries, mode, focusDate),
            historyLineThickness = settings.historyLineThickness,
            historyDotRadius = settings.historyDotRadius,
            focusDate = focusDate,
            canGoForward = !isAtNewest(focusDate, mode),
            dateLabel = formatDateLabel(focusDate, mode),
            isZoomEnabled = zoomEnabled,
            rankColors = settings.rankColors,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        // 切换视图模式时保持 focusDate 不变（如 WEEK→DAY 停留同一周）
        // ALL 模式下 focusDate 保留但不用于过滤
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun aggregate(
        entries: List<HistoryEntryEntity>,
        mode: ViewMode,
        focusDate: Long,
    ): List<AggregatedPoint> {
        if (entries.isEmpty()) return emptyList()
        val cal = Calendar.getInstance()

        val (startMs, endMs) = when (mode) {
            ViewMode.DAY -> {
                cal.timeInMillis = focusDate
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                Pair(start, cal.timeInMillis)
            }
            ViewMode.WEEK -> {
                cal.timeInMillis = focusDate
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                Pair(start, cal.timeInMillis)
            }
            ViewMode.MONTH -> {
                cal.timeInMillis = focusDate
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                Pair(start, cal.timeInMillis)
            }
            ViewMode.ALL -> Pair(0L, Long.MAX_VALUE)
        }

        return entries
            .filter { it.timestamp in startMs until endMs }
            .map { it.toAggregated() }
    }

    // ---- Navigation ----

    fun navigatePrevious() {
        if (_viewMode.value == ViewMode.ALL) return
        val cal = Calendar.getInstance().apply { timeInMillis = _focusDate.value }
        when (_viewMode.value) {
            ViewMode.DAY -> cal.add(Calendar.DAY_OF_MONTH, -1)
            ViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            ViewMode.MONTH -> cal.add(Calendar.MONTH, -1)
            ViewMode.ALL -> return
        }
        _focusDate.value = cal.timeInMillis
    }

    fun navigateNext() {
        if (_viewMode.value == ViewMode.ALL) return
        if (isAtNewest(_focusDate.value, _viewMode.value)) return
        val cal = Calendar.getInstance().apply { timeInMillis = _focusDate.value }
        when (_viewMode.value) {
            ViewMode.DAY -> cal.add(Calendar.DAY_OF_MONTH, 1)
            ViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            ViewMode.MONTH -> cal.add(Calendar.MONTH, 1)
            ViewMode.ALL -> return
        }
        _focusDate.value = cal.timeInMillis
    }

    fun resetFocusToToday() {
        _focusDate.value = System.currentTimeMillis()
    }

    fun toggleZoom() {
        _isZoomEnabled.value = !_isZoomEnabled.value
    }

    fun zoomToPoint(timestamp: Long) {
        when (_viewMode.value) {
            ViewMode.DAY -> { /* 已是最细粒度 */ }
            ViewMode.WEEK -> {
                _focusDate.value = timestamp
                _viewMode.value = ViewMode.DAY
            }
            ViewMode.MONTH -> {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                _focusDate.value = cal.timeInMillis
                _viewMode.value = ViewMode.WEEK
            }
            ViewMode.ALL -> {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                _focusDate.value = cal.timeInMillis
                _viewMode.value = ViewMode.MONTH
            }
        }
    }

    // ---- Helper methods ----

    /** 判断 focusDate 是否在当前最新周期内 */
    private fun isAtNewest(focusDate: Long, mode: ViewMode): Boolean {
        if (mode == ViewMode.ALL) return true
        val now = Calendar.getInstance()
        val focus = Calendar.getInstance().apply { timeInMillis = focusDate }
        return when (mode) {
            ViewMode.DAY -> focus.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    && focus.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            ViewMode.WEEK -> focus.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                    && focus.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            ViewMode.MONTH -> focus.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                    && focus.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            ViewMode.ALL -> true
        }
    }

    /** 格式化日期标签 */
    private fun formatDateLabel(focusDate: Long, mode: ViewMode): String {
        if (mode == ViewMode.ALL) return "全部记录"
        val cal = Calendar.getInstance().apply { timeInMillis = focusDate }
        return when (mode) {
            ViewMode.DAY -> String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            ViewMode.WEEK -> {
                val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
                val monCal = cal.clone() as Calendar
                monCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val sunCal = cal.clone() as Calendar
                sunCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                "第${weekNum}周 (${monCal.get(Calendar.MONTH) + 1}/${monCal.get(Calendar.DAY_OF_MONTH)}-${sunCal.get(Calendar.MONTH) + 1}/${sunCal.get(Calendar.DAY_OF_MONTH)})"
            }
            ViewMode.MONTH -> String.format("%04d年%02d月",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            ViewMode.ALL -> "全部记录"
        }
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
