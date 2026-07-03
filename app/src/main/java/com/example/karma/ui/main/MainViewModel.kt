package com.example.karma.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.model.Rank
import com.example.karma.data.repository.KarmaRepository
import com.example.karma.util.LuckAmplifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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

    /** LuckAmplifier 脱耦：debounce 后异步计算，结果缓存。 */
    private val _luckValue = MutableStateFlow<Float?>(null)

    // 合并 selection 相关流（嵌套 combine 避免 6-flow 类型推断失败）
    private val _selectionPair = combine(
        combine(_selectedScore, _customScore) { preset, custom -> custom ?: preset },
        combine(
            _selectedEvent, _customGoodDeedEvent, _customBadDeedEvent, _customGoodResultEvent,
        ) { preset, cg, cb, cr -> cg ?: cb ?: cr ?: preset },
    ) { score, event -> Pair(score, event) }

    // 合并 message + timerEnabled 以减少 combine 参数数量
    private val _msgTimer = combine(_message, _timerEnabled) { m, t -> Pair(m, t) }

    init {
        // LuckAmplifier：debounce(300ms) 避免 slider 拖动等高频变更时反复重算
        viewModelScope.launch {
            combine(repository.settings, repository.allHistory) { s, h -> Pair(s, h) }
                .debounce(300)
                .collect { (settings, history) ->
                    _luckValue.value = if (settings.luckEnabled) {
                        LuckAmplifier.computeLuckAmplification(
                            totalScore = settings.totalScore,
                            historyEntries = history,
                            T = settings.luckT,
                            b = settings.luckB,
                            W = settings.luckW,
                        )
                    } else null
                }
        }
    }

    // 嵌套 combine 避免 5-arg overload 类型推断问题
    private val _settingsHistory = combine(
        repository.settings, repository.allHistory,
    ) { s, h -> Pair(s, h) }

    private val _selectionLuckMsg = combine(
        _selectionPair, _luckValue, _msgTimer,
    ) { sel, luck, mt -> Triple(sel, luck, mt) }

    val uiState: StateFlow<MainUiState> = combine(
        _settingsHistory, _selectionLuckMsg,
    ) { (settings, history), (selection, derivedLuck, msgTimer) ->
        val (msg, timerEnabled) = msgTimer
        val (effectiveScore, effectiveEvent) = selection
        // 缓存善果列表供 onConfirm 使用
        _currentGoodResultPresets = settings.goodResultPresets
        MainUiState(
            totalScore = settings.totalScore,
            rank = repository.getRank(settings.totalScore, settings),
            scorePresets = settings.scorePresets,
            goodDeedPresets = settings.goodDeedPresets,
            badDeedPresets = settings.badDeedPresets,
            goodResultPresets = settings.goodResultPresets,
            selectedScore = effectiveScore,
            effectiveScore = effectiveScore,
            selectedEvent = effectiveEvent,
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
            // _timerEnabled 在每次修改选择时同步更新，是 combine 的独立输入源
            hasScoreAndEvent = timerEnabled,
            // 运气增幅（debounce 后异步计算，不阻塞主 combine）
            luckValue = derivedLuck,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    // ---- Actions ----

    fun selectScore(score: Float) {
        _selectedScore.value = score
        _customScore.value = null
        updateTimerEnabled()
    }

    fun selectEvent(event: String) {
        _selectedEvent.value = event
        _customGoodDeedEvent.value = null
        _customBadDeedEvent.value = null
        _customGoodResultEvent.value = null
        updateTimerEnabled()
    }

    fun onCustomScoreChanged(text: String) {
        val v = text.toFloatOrNull()
        if (v != null) {
            _customScore.value = v
            _selectedScore.value = null
        } else {
            _customScore.value = null
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
        } else {
            _customGoodDeedEvent.value = null
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
        } else {
            _customBadDeedEvent.value = null
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
        } else {
            _customGoodResultEvent.value = null
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
