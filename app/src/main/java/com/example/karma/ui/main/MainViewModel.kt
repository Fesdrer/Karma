package com.example.karma.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.model.Rank
import com.example.karma.data.repository.KarmaRepository
import com.example.karma.util.LuckAmplifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val dotColor: Long = 0xFFFF0000L,
    // ===== 消息 =====
    val message: String? = null,
    // ===== 计时可用（直接判断源 flow，绕过 combine 链延迟） =====
    val hasScoreAndEvent: Boolean = false,
    // ===== 运气增幅 =====
    val luckValue: Float? = null,
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
    /** 独立状态：是否有选中的分数+事件。每次变更时手动同步，不依赖 combine 链。 */
    private val _timerEnabled = MutableStateFlow(false)

    // 缓存当前善果预设列表，用于 onConfirm 时判断是否加前缀
    private var _currentGoodResultPresets: List<String> = emptyList()

    /** 独立的选择状态流：ScorePanel/EventPanel 直接读此流，绕过 combine 链。
     *  拖动滑条时不会触发 MainScreen 整体重组。 */
    private val _effectiveScoreState = MutableStateFlow<Float?>(null)
    val effectiveScoreState: StateFlow<Float?> = _effectiveScoreState
    private val _effectiveEventState = MutableStateFlow<String?>(null)
    val effectiveEventState: StateFlow<String?> = _effectiveEventState

    // 合并 message + timerEnabled
    private val _msgTimer = combine(_message, _timerEnabled) { m, t -> Pair(m, t) }

    // settings + history + luck：合并为单一 flow，避免 Room 双重订阅。
    private val _settingsLuck = combine(
        repository.settings, repository.allHistory,
    ) { s, h ->
        val luck = if (s.luckEnabled) {
            LuckAmplifier.computeLuckAmplification(s.totalScore, h, s.luckT, s.luckB, s.luckW)
        } else null
        Triple(s, h, luck)
    }

    // 缓存 ranks 列表，仅在 rank 相关设置变更时重建。
    private var _cachedRankSettings: List<Any> = emptyList()
    private var _cachedRanks: List<Rank> = emptyList()

    /** uiState 初始为 null，首帧不渲染。combine 首次发射后一次性显示全部内容。 */
    private val _uiState = MutableStateFlow<MainUiState?>(null)
    val uiState: StateFlow<MainUiState?> = _uiState

    init {
        viewModelScope.launch {
            combine(
                _settingsLuck, _msgTimer,
            ) { (settings, history, luckValue), (msg, timerEnabled) ->
                _currentGoodResultPresets = settings.goodResultPresets
                val rankKey = listOf(settings.rankThresholds, settings.rankNames, settings.rankColors)
                if (rankKey != _cachedRankSettings) {
                    _cachedRankSettings = rankKey
                    _cachedRanks = Rank.listFrom(settings.rankThresholds, settings.rankNames, settings.rankColors)
                }
                MainUiState(
                    totalScore = settings.totalScore,
                    rank = repository.getRank(settings.totalScore, settings),
                    scorePresets = settings.scorePresets,
                    goodDeedPresets = settings.goodDeedPresets,
                    badDeedPresets = settings.badDeedPresets,
                    goodResultPresets = settings.goodResultPresets,
                    // 选择状态由 ScorePanel/EventPanel 直接从 ViewModel 读取，这里不需要
                    selectedScore = null,
                    effectiveScore = null,
                    selectedEvent = null,
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
                    ranks = _cachedRanks,
                    historyLineThickness = settings.historyLineThickness,
                    historyDotRadius = settings.historyDotRadius,
                    dotColor = settings.dotColor,
                    message = msg,
                    hasScoreAndEvent = timerEnabled,
                    luckValue = luckValue,
                )
            }.collect { _uiState.value = it }
        }
    }

    // ---- Actions ----

    fun selectScore(score: Float) {
        _selectedScore.value = score
        _customScore.value = null
        _effectiveScoreState.value = score
        updateTimerEnabled()
    }

    fun selectEvent(event: String) {
        _selectedEvent.value = event
        _customGoodDeedEvent.value = null
        _customBadDeedEvent.value = null
        _customGoodResultEvent.value = null
        _effectiveEventState.value = event
        updateTimerEnabled()
    }

    fun onCustomScoreChanged(text: String) {
        val v = text.toFloatOrNull()
        if (v != null) {
            _customScore.value = v
            _selectedScore.value = null
            _effectiveScoreState.value = v
        } else {
            _customScore.value = null
            _effectiveScoreState.value = _selectedScore.value
        }
        updateTimerEnabled()
    }

    fun onCustomGoodDeedEventChanged(text: String) {
        val v = text.trim()
        if (v.isNotEmpty()) {
            _customGoodDeedEvent.value = v
            _selectedEvent.value = null
            _customBadDeedEvent.value = null
            _customGoodResultEvent.value = null
            _effectiveEventState.value = v
        } else {
            _customGoodDeedEvent.value = null
            _effectiveEventState.value = _selectedEvent.value
        }
        updateTimerEnabled()
    }

    fun onCustomBadDeedEventChanged(text: String) {
        val v = text.trim()
        if (v.isNotEmpty()) {
            _customBadDeedEvent.value = v
            _selectedEvent.value = null
            _customGoodDeedEvent.value = null
            _customGoodResultEvent.value = null
            _effectiveEventState.value = v
        } else {
            _customBadDeedEvent.value = null
            _effectiveEventState.value = _selectedEvent.value
        }
        updateTimerEnabled()
    }

    fun onCustomGoodResultEventChanged(text: String) {
        val v = text.trim()
        if (v.isNotEmpty()) {
            _customGoodResultEvent.value = v
            _selectedEvent.value = null
            _customGoodDeedEvent.value = null
            _customBadDeedEvent.value = null
            _effectiveEventState.value = v
        } else {
            _customGoodResultEvent.value = null
            _effectiveEventState.value = _selectedEvent.value
        }
        updateTimerEnabled()
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

        // 同步清除所有选择（即时禁用按钮，不等 launch）
        _selectedScore.value = null
        _selectedEvent.value = null
        _customScore.value = null
        _customGoodDeedEvent.value = null
        _customBadDeedEvent.value = null
        _customGoodResultEvent.value = null
        _effectiveScoreState.value = null
        _effectiveEventState.value = null
        updateTimerEnabled()

        viewModelScope.launch {
            repository.addHistoryEntry(score, event, "record")
        }
    }

    /** 同步更新 _timerEnabled，每次修改选择后调用。值未变时跳过发射。 */
    private fun updateTimerEnabled() {
        val hasScore = _customScore.value != null || _selectedScore.value != null
        val hasEvent = _customGoodDeedEvent.value != null || _customBadDeedEvent.value != null ||
                _customGoodResultEvent.value != null || _selectedEvent.value != null
        val newValue = hasScore && hasEvent
        if (newValue != _timerEnabled.value) {
            _timerEnabled.value = newValue
        }
    }

    /** 直接读源 StateFlow，获取当前选中分数 */
    fun getSelectedScore(): Float? = _customScore.value ?: _selectedScore.value

    /** 直接读源 StateFlow，获取当前选中事件 */
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
