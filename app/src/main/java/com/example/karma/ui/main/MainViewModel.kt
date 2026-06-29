package com.example.karma.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.model.Rank
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val totalScore: Float = 0f,
    val rank: Rank? = null,
    val scorePresets: List<Float> = emptyList(),
    val goodDeedPresets: List<String> = emptyList(),
    val badDeedPresets: List<String> = emptyList(),
    val goodResultPresets: List<String> = emptyList(),
    val selectedScore: Float? = null,
    val effectiveScore: Float? = null,
    val selectedEvent: String? = null,
    // ===== 以下为 settings 透传字段 =====
    val scoreAxisFontSize: Float = 22f,
    val scoreAxisRangeMin: Float = -6f,
    val scoreAxisRangeMax: Float = 6f,
    val axisLabelColor: Long = 0x80FFFFFF.toLong(),
    val axisTickThickness: Float = 1f,
    val axisLabelFontSize: Float = 19f,
    val axisDisplayRange: Float = 100f,
    val showNearbyTicks: Boolean = true,
    val nearbyTickRange: Float = 10f,
    val axisQuarterValue: Float = 15f,
    val ranks: List<Rank> = emptyList(),
    val historyLineThickness: Float = 2f,
    val historyDotRadius: Float = 3.5f,
    val guideLineWidth: Float = 6f,
    val guideLineColor: Long = 0xFFFFD700L,
    // ===== 消息 =====
    val message: String? = null,
    // ===== 计时可用（直接判断源 flow，绕过 combine 链延迟） =====
    val hasScoreAndEvent: Boolean = false,
)

class MainViewModel(
    private val repository: KarmaRepository,
) : ViewModel() {

    private val _selectedScore = MutableStateFlow<Float?>(null)
    private val _selectedEvent = MutableStateFlow<String?>(null)
    private val _customScore = MutableStateFlow<Float?>(null)
    private val _customGoodDeedEvent = MutableStateFlow<String?>(null)
    private val _customBadDeedEvent = MutableStateFlow<String?>(null)
    private val _customGoodResultEvent = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)

    // 缓存当前善果预设列表，用于 onConfirm 时判断是否加前缀
    private var _currentGoodResultPresets: List<String> = emptyList()

    private val _effectiveScore = combine(
        _selectedScore, _customScore
    ) { presetScore, custom -> custom ?: presetScore }

    private val _effectiveEvent = combine(
        _selectedEvent, _customGoodDeedEvent, _customBadDeedEvent, _customGoodResultEvent
    ) { presetEvent, customGood, customBad, customResult ->
        customGood ?: customBad ?: customResult ?: presetEvent
    }

    private val _selectedPair = combine(
        _effectiveScore, _effectiveEvent
    ) { score, event -> Pair(score, event) }

    val uiState: StateFlow<MainUiState> = combine(
        repository.settings,
        _selectedPair,
        _message,
    ) { settings, selection, msg ->
        // 缓存善果列表供 onConfirm 使用
        _currentGoodResultPresets = settings.goodResultPresets
        MainUiState(
            totalScore = settings.totalScore,
            rank = repository.getRank(settings.totalScore, settings),
            scorePresets = settings.scorePresets,
            goodDeedPresets = settings.goodDeedPresets,
            badDeedPresets = settings.badDeedPresets,
            goodResultPresets = settings.goodResultPresets,
            selectedScore = selection.first,
            effectiveScore = selection.first,
            selectedEvent = selection.second,
            // ★ 视觉参数透传
            scoreAxisFontSize = settings.scoreAxisFontSize,
            scoreAxisRangeMin = settings.scoreAxisRangeMin,
            scoreAxisRangeMax = settings.scoreAxisRangeMax,
            axisLabelColor = settings.axisLabelColor,
            axisTickThickness = settings.axisTickThickness,
            axisLabelFontSize = settings.axisLabelFontSize,
            axisDisplayRange = settings.axisDisplayRange,
            showNearbyTicks = settings.showNearbyTicks,
            nearbyTickRange = settings.nearbyTickRange,
            axisQuarterValue = settings.axisQuarterValue,
            ranks = Rank.listFrom(settings.rankThresholds, settings.rankNames, settings.rankColors),
            historyLineThickness = settings.historyLineThickness,
            historyDotRadius = settings.historyDotRadius,
            guideLineWidth = settings.guideLineWidth,
            guideLineColor = settings.guideLineColor,
            message = msg,
            // 直接读源 StateFlow 值，避免 combine 链延迟导致 UI 判断滞后
            hasScoreAndEvent = (_customScore.value != null || _selectedScore.value != null) &&
                    (_customGoodDeedEvent.value != null || _customBadDeedEvent.value != null ||
                    _customGoodResultEvent.value != null || _selectedEvent.value != null),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    // ---- Actions ----

    fun selectScore(score: Float) {
        _selectedScore.value = score
        _customScore.value = null
    }

    fun selectEvent(event: String) {
        _selectedEvent.value = event
        _customGoodDeedEvent.value = null
        _customBadDeedEvent.value = null
        _customGoodResultEvent.value = null
    }

    fun onCustomScoreChanged(text: String) {
        val v = text.toFloatOrNull()
        if (v != null) {
            _customScore.value = v
            _selectedScore.value = null
        } else {
            _customScore.value = null
        }
    }

    fun onCustomGoodDeedEventChanged(text: String) {
        val v = text.trim()
        if (v.isNotEmpty()) {
            _customGoodDeedEvent.value = v
            _selectedEvent.value = null
            _customBadDeedEvent.value = null
            _customGoodResultEvent.value = null
        } else {
            _customGoodDeedEvent.value = null
        }
    }

    fun onCustomBadDeedEventChanged(text: String) {
        val v = text.trim()
        if (v.isNotEmpty()) {
            _customBadDeedEvent.value = v
            _selectedEvent.value = null
            _customGoodDeedEvent.value = null
            _customGoodResultEvent.value = null
        } else {
            _customBadDeedEvent.value = null
        }
    }

    fun onCustomGoodResultEventChanged(text: String) {
        val v = text.trim()
        if (v.isNotEmpty()) {
            _customGoodResultEvent.value = v
            _selectedEvent.value = null
            _customGoodDeedEvent.value = null
            _customBadDeedEvent.value = null
        } else {
            _customGoodResultEvent.value = null
        }
    }

    fun onConfirm() {
        val score = _customScore.value ?: _selectedScore.value ?: return
        val rawEvent = _customGoodDeedEvent.value
            ?: _customBadDeedEvent.value
            ?: _customGoodResultEvent.value
            ?: _selectedEvent.value
            ?: return

        // 善果事件自动加"善果："前缀（仿祈福前缀模式）
        val isGoodResult = _selectedEvent.value in _currentGoodResultPresets
                || _customGoodResultEvent.value != null
        val event = if (isGoodResult) "善果：$rawEvent" else rawEvent

        viewModelScope.launch {
            repository.addHistoryEntry(score, event, "record")
            _selectedScore.value = null
            _selectedEvent.value = null
            _customScore.value = null
            _customGoodDeedEvent.value = null
            _customBadDeedEvent.value = null
            _customGoodResultEvent.value = null
        }
    }

    /** 直接读源 StateFlow，获取当前选中分数（绕过 combine 链延迟） */
    fun getSelectedScore(): Float? = _customScore.value ?: _selectedScore.value

    /** 直接读源 StateFlow，获取当前选中事件（绕过 combine 链延迟） */
    fun getSelectedEvent(): String? =
        _customGoodDeedEvent.value ?: _customBadDeedEvent.value
            ?: _customGoodResultEvent.value ?: _selectedEvent.value

    // ★ 清除消息
    fun clearMessage() {
        _message.value = null
    }

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
