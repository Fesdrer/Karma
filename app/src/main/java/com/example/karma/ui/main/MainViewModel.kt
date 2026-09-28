package com.example.karma.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.local.entity.Bet
import com.example.karma.data.local.entity.DailyMustDoDeed
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.model.Fraction
import com.example.karma.data.model.ProofEngine
import com.example.karma.data.model.Rank
import com.example.karma.data.repository.KarmaRepository
import com.example.karma.util.LuckAmplifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class MainUiState(
    val totalScore: Float = 0f,
    val rank: Rank? = null,
    val scorePresets: List<Float> = emptyList(),
    val goodDeedPresets: List<String> = emptyList(),
    val badDeedPresets: List<String> = emptyList(),
    val goodResultPresets: List<String> = emptyList(),
    val multiplierPresets: List<Fraction> = emptyList(),
    val selectedScore: Float = 0f,
    val effectiveScore: Float = 0f,
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
    // 负数阶位色带（数轴负数区域），与正阶 ranks 分开（Header 的 totalRanks 只统计正阶）
    val negativeRanks: List<Rank> = emptyList(),
    val historyLineThickness: Float = 2f,
    val historyDotRadius: Float = 3.5f,
    val dotColor: Long = 0xFFFF0000L,
    // ===== 消息 =====
    val message: String? = null,
    // ===== 每日必做 =====
    val dailyMustDoDeeds: List<DailyMustDoDeed> = emptyList(),
    // ===== 计时可用（直接判断源 flow，绕过 combine 链延迟） =====
    val hasScoreAndEvent: Boolean = false,
    // ===== 运气增幅 =====
    val luckValue: Float? = null,
    // ===== 阶位自证（v4.0） =====
    val proofEnabled: Boolean = false,
    val proofActive: Boolean = false,
    val proofStartRankLevel: Int = 0,      // 徽章停留阶位
    val proofEndTime: Long = 0L,           // 自证结束时间戳（倒计时用）
    val proofLineColor: Long = 0xFFFFD700L,
    val proofGlowColor: Long = 0xFFFFFFFFL,
    val proofCountdownBg: Long = 0xFF8B0000L,
    val proofCountdownText: Long = 0xFFFFFFFFL,
    val proofSuccessColor: Long = 0xFF69f0aeL,
    val proofFailColor: Long = 0xFFff5252L,
    val canStartProof: Boolean = false,    // 距离下一正阶位 <10 分且未在自证中
)

/** 被动自证弹窗数据：加分跨入正阶位瞬间。 */
data class PassiveProofPrompt(
    val oldLevel: Int,   // 加分前阶位（起点 x，徽章停留）
    val newLevel: Int,   // 已达阶位（仅展示）
)

/** 自证模式。 */
enum class ProofMode { ACTIVE, PASSIVE }

/** 自证设置请求（弹窗确认后 startProof 用）。 */
data class ProofSetup(
    val mode: ProofMode,
    val startRank: Int,   // 起点阶位 x（主动=当前阶位；被动=加分前阶位）
    val target: Int,      // 展示用目标阶位（主动=下一正阶位；被动=已达阶位）
    val guard: Int,       // 降级守卫 = x（= startRank）
)

/** 自证结果（成功/失败弹窗用）。成功时 targetRank=结束时的实际阶位 y。 */
data class ProofResult(
    val success: Boolean,
    val startRank: Int,
    val targetRank: Int,   // 结束实际阶位 y（成功时恭喜登上 y）
    val reward: Float,
    val penalty: Float,
)

class MainViewModel(
    private val repository: KarmaRepository,
) : ViewModel() {

    private val _selectedScore = MutableStateFlow<Float>(0f)   // 初始 0：分数始终有值（无 null 状态）
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

    /** 缓存当前每日必做 deeds 列表（每个 deed 有自己的 name/penalty/vis），用于 onConfirm 判断 */
    private var _currentDailyMustDoDeeds: List<DailyMustDoDeed> = emptyList()

    // ===== 事件默认分数缓存（v3.13）：选择预设事件时左侧分数栏联动移动 =====
    private var _currentGoodDeedPresets: List<String> = emptyList()
    private var _currentGoodDeedDefaultScores: List<Float> = emptyList()
    private var _currentBadDeedPresets: List<String> = emptyList()
    private var _currentBadDeedDefaultScores: List<Float> = emptyList()
    private var _currentGoodResultDefaultScores: List<Float> = emptyList()
    private var _currentScoreAxisRangeMin: Float = -6f
    private var _currentScoreAxisRangeMax: Float = 6f

    /** 独立的选择状态流：ScorePanel/EventPanel 直接读此流，绕过 combine 链。
     *  拖动滑条时不会触发 MainScreen 整体重组。 */
    private val _effectiveScoreState = MutableStateFlow<Float>(0f)
    val effectiveScoreState: StateFlow<Float> = _effectiveScoreState
    private val _effectiveEventState = MutableStateFlow<String?>(null)
    val effectiveEventState: StateFlow<String?> = _effectiveEventState

    /** 乘法分配律（v4.1）：已选中的乘数按钮下标集合。选中时分数不变，确定时一次性应用。 */
    private val _selectedMultipliers = MutableStateFlow<Set<Int>>(emptySet())
    val selectedMultipliersState: StateFlow<Set<Int>> = _selectedMultipliers

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
    private var _cachedNegativeRankSettings: List<Any> = emptyList()
    private var _cachedNegativeRanks: List<Rank> = emptyList()

    // ===== 阶位自证状态 =====
    /** 最新 settings 快照（主动按钮/校验用）。 */
    private var _latestSettings: KarmaSettingsEntity? = null
    /** 上一次观察的分数与阶位（被动触发比较用）。 */
    private var _lastTotal = 0f
    private var _lastLevel = 0
    /** 被动自证基线是否已建立：首个 settings 发射只记录基线，
     *  避免把启动时已有的总分误判成"本次加分"而误弹被动窗。 */
    private var _proofBaselineReady = false
    /** 一次性豁免：自证结算自身的奖励加分不触发新的被动自证（避免无限连锁）。 */
    private var _suppressPassiveProofOnce = false
    /** 桌面组件点击待处理的 deed 名称队列（v4.3）：等自证基线就绪后再写库（见 drainWidgetDeedQueue）。 */
    private val widgetDeedQueue = ArrayDeque<String>()
    /** 出队写库防重入：正在写库时新请求只入队，由循环续处理。 */
    private var widgetDeedDraining = false
    /** 防重入：finishProof 同时被检查与超时协程触发时只执行一次。 */
    private var _proofFinishing = false
    /** 被动自证弹窗（加分跨入正阶位时置入，UI 处理后清空）。 */
    private val _pendingPassiveProof = MutableStateFlow<PassiveProofPrompt?>(null)
    val pendingPassiveProof: StateFlow<PassiveProofPrompt?> = _pendingPassiveProof.asStateFlow()
    /** 自证设置弹窗请求（主动按钮 / 被动确认后置入）。 */
    private val _proofSetup = MutableStateFlow<ProofSetup?>(null)
    val proofSetup: StateFlow<ProofSetup?> = _proofSetup.asStateFlow()
    /** 自证结果（成功/失败弹窗）。 */
    private val _proofResult = MutableStateFlow<ProofResult?>(null)
    val proofResult: StateFlow<ProofResult?> = _proofResult.asStateFlow()
    /** 誓约到期弹窗（v4.3：与衰减/每日必做一起判定；UI 处理完成后清空）。 */
    private val _expiredBetPrompt = MutableStateFlow<Bet?>(null)
    val expiredBetPrompt: StateFlow<Bet?> = _expiredBetPrompt.asStateFlow()

    /** uiState 初始为 null，首帧不渲染。combine 首次发射后一次性显示全部内容。 */
    private val _uiState = MutableStateFlow<MainUiState?>(null)
    val uiState: StateFlow<MainUiState?> = _uiState

    init {
        viewModelScope.launch {
            combine(
                _settingsLuck, _msgTimer,
            ) { (settings, history, luckValue), (msg, timerEnabled) ->
                _currentGoodResultPresets = settings.goodResultPresets
                _currentDailyMustDoDeeds = settings.dailyMustDoDeeds
                // 事件默认分数缓存（v3.13）
                _currentGoodDeedPresets = settings.goodDeedPresets
                _currentGoodDeedDefaultScores = settings.goodDeedDefaultScores
                _currentBadDeedPresets = settings.badDeedPresets
                _currentBadDeedDefaultScores = settings.badDeedDefaultScores
                _currentGoodResultDefaultScores = settings.goodResultDefaultScores
                _currentScoreAxisRangeMin = settings.scoreAxisRangeMin
                _currentScoreAxisRangeMax = settings.scoreAxisRangeMax
                val rankKey = listOf(settings.rankThresholds, settings.rankNames, settings.rankColors)
                if (rankKey != _cachedRankSettings) {
                    _cachedRankSettings = rankKey
                    _cachedRanks = Rank.listFrom(settings.rankThresholds, settings.rankNames, settings.rankColors)
                }
                val negativeRankKey = listOf(
                    settings.negativeRankThresholds, settings.negativeRankNames, settings.negativeRankColors,
                )
                if (negativeRankKey != _cachedNegativeRankSettings) {
                    _cachedNegativeRankSettings = negativeRankKey
                    _cachedNegativeRanks = repository.buildNegativeRanks(settings)
                }
                _latestSettings = settings
                val actualRank = repository.getRank(settings.totalScore, settings)
                // 自证期间徽章停留在开始自证时的阶位
                val displayRank = if (settings.proofActive) {
                    (_cachedRanks + _cachedNegativeRanks).find { it.level == settings.proofStartRankLevel }
                        ?: actualRank
                } else {
                    actualRank
                }
                val currentLevel = actualRank?.level ?: 1
                MainUiState(
                    totalScore = settings.totalScore,
                    rank = displayRank,
                    scorePresets = settings.scorePresets,
                    goodDeedPresets = settings.goodDeedPresets,
                    badDeedPresets = settings.badDeedPresets,
                    goodResultPresets = settings.goodResultPresets,
                    multiplierPresets = settings.multiplierPresets,
                    // 选择状态由 ScorePanel/EventPanel 直接从 ViewModel 读取，这里不需要
                    selectedScore = 0f,
                    effectiveScore = 0f,
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
                    negativeRanks = _cachedNegativeRanks,
                    historyLineThickness = settings.historyLineThickness,
                    historyDotRadius = settings.historyDotRadius,
                    dotColor = settings.dotColor,
                    message = msg,
                    hasScoreAndEvent = timerEnabled,
                    luckValue = luckValue,
                    dailyMustDoDeeds = settings.dailyMustDoDeeds,
                    proofEnabled = settings.proofEnabled,
                    proofActive = settings.proofActive,
                    proofStartRankLevel = settings.proofStartRankLevel,
                    proofEndTime = settings.proofStartTime + settings.proofDurationMs,
                    proofLineColor = settings.proofLineColor,
                    proofGlowColor = settings.proofGlowColor,
                    proofCountdownBg = settings.proofCountdownBg,
                    proofCountdownText = settings.proofCountdownText,
                    proofSuccessColor = settings.proofSuccessColor,
                    proofFailColor = settings.proofFailColor,
                    canStartProof = settings.proofEnabled && !settings.proofActive &&
                        ProofEngine.distanceToNextPositiveRank(settings.totalScore, currentLevel, settings) < 10f,
                )
            }.collect { _uiState.value = it }
        }
        // 自证检测：每次 settings 变化时判定（跨阶触发/降级/达标/超时/开关关闭）
        // 之后紧接着处理桌面组件请求：checkProof 已在首个发射建立基线，此时写库才能被正确判定为「本次加分」
        viewModelScope.launch {
            repository.settings.collect { s ->
                checkProof(s)
                drainWidgetDeedQueue()
            }
        }
        // 自证超时兜底：settings 长时间不变时（无操作），每秒检查一次是否到结束时刻
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val s = repository.settings.first()
                if (s.proofActive && System.currentTimeMillis() >= s.proofStartTime + s.proofDurationMs) {
                    // 时长结束：当前阶位 y > 起点阶位 x 才成功
                    finishProof(s, ProofEngine.isEndSuccess(s.totalScore, s, s.proofStartRankLevel))
                }
            }
        }
        // 业力衰减 & 每日必做：2 秒周期判定（v4.2-改5）
        // 应用前台/计时/自证期间持续运行，跨过衰减时刻即自动补扣 + 每日必做判定；
        // 进程被杀后由 KarmaApplication 启动补扣兜底（维持非 AlarmManager 决策）。
        // applyDecay 幂等：未到判定时刻直接返回 0，不重复扣分。
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2000)
                try {
                    repository.applyDecay()
                    checkExpiredBets()
                } catch (e: Exception) {
                    // 单次失败不中断轮询（下轮重试）；记录日志便于定位
                    android.util.Log.e("KarmaDecay", "周期衰减判定失败", e)
                }
            }
        }
        // 誓约到期：打开应用时立即判定一次（v4.3）
        viewModelScope.launch {
            try {
                checkExpiredBets()
            } catch (e: Exception) {
                android.util.Log.e("KarmaBet", "誓约到期判定失败", e)
            }
        }
    }

    /**
     * 誓约到期判定（v4.3）：与业力衰减/每日必做一起在 2 秒周期内判定。
     * 已有弹窗在显示时跳过，避免覆盖用户正在处理的誓约或叠出多个弹窗
     * （处理完下一轮再弹下一条）。
     */
    private suspend fun checkExpiredBets() {
        if (_expiredBetPrompt.value != null) return
        if (_pendingPassiveProof.value != null || _proofSetup.value != null || _proofResult.value != null) return
        val s = repository.settings.first()
        repository.getExpiredBets(s).firstOrNull()?.let { _expiredBetPrompt.value = it }
    }

    /** 誓约到期：选择「完成」→ +成功分并移除该誓约。 */
    fun onExpiredBetComplete() {
        val bet = _expiredBetPrompt.value ?: return
        _expiredBetPrompt.value = null
        viewModelScope.launch {
            try {
                repository.resolveBet(bet, true)
            } catch (e: Exception) {
                android.util.Log.e("KarmaBet", "誓约结算失败", e)
            }
        }
    }

    /** 誓约到期：选择「未完成」→ −失败分并移除该誓约。 */
    fun onExpiredBetFail() {
        val bet = _expiredBetPrompt.value ?: return
        _expiredBetPrompt.value = null
        viewModelScope.launch {
            try {
                repository.resolveBet(bet, false)
            } catch (e: Exception) {
                android.util.Log.e("KarmaBet", "誓约结算失败", e)
            }
        }
    }

    // ---- Actions ----

    fun selectScore(score: Float) {
        _selectedScore.value = score
        _customScore.value = null
        _effectiveScoreState.value = score
        updateTimerEnabled()
    }

    /**
     * 重置当前选择（v4.3，左栏分数面板底部「重置」按钮）：
     * 选中的加减分归 0、事件取消选中、乘法按钮取消选中。不影响业力总分。
     */
    fun resetSelection() {
        _selectedScore.value = 0f
        _customScore.value = null
        _selectedEvent.value = null
        _customGoodDeedEvent.value = null
        _customBadDeedEvent.value = null
        _customGoodResultEvent.value = null
        _selectedMultipliers.value = emptySet()
        _effectiveScoreState.value = 0f
        _effectiveEventState.value = null
        updateTimerEnabled()
    }

    fun selectEvent(event: String) {
        _selectedEvent.value = event
        _customGoodDeedEvent.value = null
        _customBadDeedEvent.value = null
        _customGoodResultEvent.value = null
        _effectiveEventState.value = event
        // 事件默认分数联动：仅当当前分数为 0 时，才把左侧分数设为该事件的默认分数；
        // 否则保持用户已选的分数不变。自定义输入（不在预设列表）不联动。
        val currentScore = _customScore.value ?: _selectedScore.value
        if (currentScore == 0f) {
            defaultScoreFor(event)?.let { score ->
                val clamped = score.coerceIn(_currentScoreAxisRangeMin, _currentScoreAxisRangeMax)
                _customScore.value = null
                _selectedScore.value = clamped
                _effectiveScoreState.value = clamped
            }
        }
        updateTimerEnabled()
    }

    // ===== 乘法分配律（v4.1）：点击乘数仅切换选中（分数不变），主页面「确认」时一次性应用 =====

    /** 切换某个乘数按钮的选中状态（不能重复选中同一乘数，再按一次取消）。 */
    fun toggleMultiplier(index: Int) {
        val set = _selectedMultipliers.value
        _selectedMultipliers.value = if (index in set) set - index else set + index
    }

    /** 查找预设事件的默认分数（带符号：善业正、恶业/善果负）；未找到返回 null。 */
    private fun defaultScoreFor(event: String): Float? {
        val goodIdx = _currentGoodDeedPresets.indexOf(event)
        if (goodIdx >= 0) return _currentGoodDeedDefaultScores.getOrElse(goodIdx) { 1f }
        val badIdx = _currentBadDeedPresets.indexOf(event)
        if (badIdx >= 0) return _currentBadDeedDefaultScores.getOrElse(badIdx) { -1f }
        val resultIdx = _currentGoodResultPresets.indexOf(event)
        if (resultIdx >= 0) return _currentGoodResultDefaultScores.getOrElse(resultIdx) { -1f }
        return null
    }

    fun onCustomScoreChanged(text: String) {
        val v = text.toFloatOrNull()
        if (v != null) {
            _customScore.value = v
            _selectedScore.value = 0f
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
        val base = _customScore.value ?: _selectedScore.value
        val rawEvent = _customGoodDeedEvent.value
            ?: _customBadDeedEvent.value
            ?: _customGoodResultEvent.value
            ?: _selectedEvent.value
            ?: return

        // 乘法分配律（v4.1）：本次记录分数 = 当前分数 × Σ(选中乘数)，向 0.5 四舍五入。
        // 例如当前分数 +3，选中 ×1 与 ×1/3 → 3 × (1 + 1/3) = 4。未选乘数时分数不变。
        var score = base
        val selectedMultipliers = _selectedMultipliers.value
        if (selectedMultipliers.isNotEmpty()) {
            val presets = _latestSettings?.multiplierPresets
            if (presets != null) {
                val sum = selectedMultipliers.fold(0f) { acc, i ->
                    acc + (presets.getOrNull(i)?.value ?: 0f)
                }
                score = (base * sum * 2f).roundToInt() / 2f
            }
        }

        // 善果事件自动加"善果："前缀（仿祈福前缀模式）
        val isGoodResult = _selectedEvent.value in _currentGoodResultPresets
                || _customGoodResultEvent.value != null
        val event = if (isGoodResult) "善果：$rawEvent" else rawEvent

        // 同步清除所有选择（即时禁用按钮，不等 launch）
        _selectedScore.value = 0f
        _selectedEvent.value = null
        _customScore.value = null
        _customGoodDeedEvent.value = null
        _customBadDeedEvent.value = null
        _customGoodResultEvent.value = null
        _selectedMultipliers.value = emptySet()
        _effectiveScoreState.value = 0f
        _effectiveEventState.value = null
        updateTimerEnabled()

        // 写库（加分跨阶的被动自证由 checkProof 按总分增加统一判定，无需额外标记）
        // 在单个协程中顺序执行，避免两个并发读写互覆盖：
        // markDeedDone 读 settings→改 vis→写；addHistoryEntry 读 settings→改 totalScore→写，
        // 并发时后写入的会覆盖前一个的改动（vis 或 totalScore 丢失）。
        viewModelScope.launch {
            // 如果是每日必做善业（且 vis=0 未做），先标记为今日已做
            val completedDeed = _currentDailyMustDoDeeds.find { it.name == rawEvent }
            if (completedDeed != null && completedDeed.vis == 0) {
                repository.markDeedDone(rawEvent)
            }
            repository.addHistoryEntry(score, event, "record")
        }
    }

    /** 同步更新 _timerEnabled，每次修改选择后调用。值未变时跳过发射。
     *  分数始终有值（初始 0，选中事件时 0 分会自动填默认分），所以只需事件即可开始计时/确认。 */
    private fun updateTimerEnabled() {
        val hasEvent = _customGoodDeedEvent.value != null || _customBadDeedEvent.value != null ||
                _customGoodResultEvent.value != null || _selectedEvent.value != null
        if (hasEvent != _timerEnabled.value) {
            _timerEnabled.value = hasEvent
        }
    }

    /** 直接读源 StateFlow，获取当前选中分数 */
    fun getSelectedScore(): Float = _customScore.value ?: _selectedScore.value

    /** 直接读源 StateFlow，获取当前选中事件 */
    fun getSelectedEvent(): String? =
        _customGoodDeedEvent.value ?: _customBadDeedEvent.value
            ?: _customGoodResultEvent.value ?: _selectedEvent.value

    // ★ 清除消息
    fun clearMessage() {
        _message.value = null
    }

    // ★ 每日必做：标记某善业为今日已做（该 deed 的 vis→1，持久化供衰减算法读取）
    fun markDailyMustDoDone(eventName: String) {
        viewModelScope.launch {
            repository.markDeedDone(eventName)
        }
    }

    // ===== 桌面组件：点圆圈标记完成（v4.3）=====

    /**
     * 桌面组件点击某行圆圈 → 完成该每日必做（标记 vis=1 + 按善业默认分加分 + 写历史）。
     *
     * 写库路径与首页 onConfirm 一致（先 markDeedDone 再 addHistoryEntry，同一协程顺序执行，
     * 避免两个「读 settings→改→写」并发互相覆盖），因此自证由既有 checkProof 按「总分增加」正常判定。
     */
    fun completeDailyMustDoFromWidget(deedName: String) {
        widgetDeedQueue.addLast(deedName)
        drainWidgetDeedQueue()
    }

    /**
     * 出队写库。**必须等 `_proofBaselineReady` 就绪**再执行：
     * 组件点击常发生在冷启动瞬间，若抢在首个 settings 发射之前写库，
     * 那一发会被当作基线（分数已加过），跨阶永远检测不到 → 不弹自证。
     */
    private fun drainWidgetDeedQueue() {
        if (!_proofBaselineReady || widgetDeedDraining || widgetDeedQueue.isEmpty()) return
        widgetDeedDraining = true
        viewModelScope.launch {
            try {
                while (widgetDeedQueue.isNotEmpty()) {
                    val deedName = widgetDeedQueue.removeFirst()
                    val settings = repository.settings.first()
                    val deed = settings.dailyMustDoDeeds.find { it.name == deedName }
                    // 非每日必做 / 已完成 → 忽略：保证重复点击不重复加分（幂等）
                    if (deed == null || deed.vis != 0) continue

                    // 分数取该善业的默认分（每日必做与善业预设同步增删改，正常都能查到；查不到按 +1 兜底）
                    val index = settings.goodDeedPresets.indexOf(deedName)
                    val score = settings.goodDeedDefaultScores.getOrElse(index) { 1f }

                    repository.markDeedDone(deedName)
                    repository.addHistoryEntry(score, deedName, "record")
                }
            } finally {
                widgetDeedDraining = false
            }
        }
    }

    // ===== 阶位自证 =====

    /** 主动开启：距离下一正阶位 <10 分且未在自证中 → 弹设置窗。 */
    fun requestActiveProof() {
        val s = _latestSettings ?: return
        if (!s.proofEnabled || s.proofActive) return
        val level = ProofEngine.rankLevelOf(s.totalScore, s)
        if (ProofEngine.distanceToNextPositiveRank(s.totalScore, level, s) >= 10f) return
        _proofSetup.value = ProofSetup(
            mode = ProofMode.ACTIVE,
            startRank = level,
            target = ProofEngine.nextPositiveRankLevel(level),
            guard = level,
        )
    }

    /** 被动弹窗选「设置自证」：先关闭询问窗，再转设置窗（起点 x=加分前阶位）。 */
    fun onPassiveProofSetup() {
        val p = _pendingPassiveProof.value ?: return
        _pendingPassiveProof.value = null
        _proofSetup.value = ProofSetup(
            mode = ProofMode.PASSIVE,
            startRank = p.oldLevel,   // x = 加分前阶位
            target = p.newLevel,      // 已达阶位（仅展示）
            guard = p.oldLevel,       // 降级守卫 = x = 加分前阶位
        )
    }

    /** 被动弹窗选「取消」：逃避自证，扣分到最高新阶位阈值-1。 */
    fun onPassiveProofEscape() {
        val p = _pendingPassiveProof.value ?: return
        _pendingPassiveProof.value = null
        viewModelScope.launch {
            val s = repository.settings.first()
            val threshold = ProofEngine.thresholdOf(p.newLevel, s)
            val targetScore = threshold - 1f
            val delta = (s.totalScore - targetScore).coerceAtLeast(0.1f)
            repository.addHistoryEntry(-delta, "逃避自证", "record")
        }
    }

    /** 设置窗确认：开始自证（时长/奖励/惩罚）。 */
    fun startProof(reward: Float, penalty: Float, durationMs: Long) {
        val setup = _proofSetup.value ?: return
        _proofSetup.value = null
        _pendingPassiveProof.value = null
        viewModelScope.launch {
            val s = repository.settings.first()
            repository.updateAllSettings(s.copy(
                proofActive = true,
                proofStartTime = System.currentTimeMillis(),
                proofDurationMs = durationMs,
                proofStartRankLevel = setup.startRank,
                proofTargetLevel = setup.target,
                proofGuardLevel = setup.guard,
                proofReward = reward,
                proofPenalty = penalty,
            ))
        }
    }

    /** 关闭设置窗（不开始）。 */
    fun clearProofSetup() {
        _proofSetup.value = null
        _pendingPassiveProof.value = null
    }

    /** 关闭自证结果弹窗。 */
    fun clearProofResult() {
        _proofResult.value = null
    }

    /**
     * 自证判定（每次 settings 变化时调用）：
     * - 自证中：开关关闭 → 强制失败；阶位低于起点 x → 立即失败。
     *   成功只发生在时长结束（超时协程判定 y > x），时长中不提前判成功。
     * - 非自证中：**任何总分增加**（普通记录、计时结算、誓约了结加分等）导致跨入更高的正阶位时弹被动窗。
     *   自证结算自身的奖励加分用一次性豁免跳过，避免"成功即再触发"的无限连锁。
     */
    private fun checkProof(s: KarmaSettingsEntity) {
        // 首个发射只建立基线：启动时既有总分不能当作"本次加分"
        if (!_proofBaselineReady) {
            _proofBaselineReady = true
            _lastTotal = s.totalScore
            _lastLevel = ProofEngine.rankLevelOf(s.totalScore, s)
            // 启动时若自证仍在进行：开关已关或阶位已跌破起点 → 直接判失败
            if (s.proofActive && (!s.proofEnabled ||
                    ProofEngine.isDowngraded(s.totalScore, s, s.proofStartRankLevel))) {
                finishProof(s, success = false)
            }
            return
        }
        if (s.proofActive) {
            // 自证进行中不弹被动窗：加分行为由自证判定处理（降级/时长结束）
            if (!s.proofEnabled) {
                finishProof(s, success = false)
                return
            }
            // 整个时长内，任何时刻阶位低于起点 x → 立即失败
            if (ProofEngine.isDowngraded(s.totalScore, s, s.proofStartRankLevel)) {
                finishProof(s, success = false)
                return
            }
            // 注意：不在此处判成功——达标必须撑到时长结束（超时协程判定），
            // 否则"登上了就成功"会绕过保持能力的考验。
        } else if (s.totalScore > _lastTotal) {
            // 任何加分都判定是否跨入更高正阶位
            if (_suppressPassiveProofOnce) {
                _suppressPassiveProofOnce = false   // 本次加分来自自证奖励，豁免一次
            } else {
                val newLevel = ProofEngine.rankLevelOf(s.totalScore, s)
                if (s.proofEnabled && newLevel >= 1 && newLevel > _lastLevel) {
                    _pendingPassiveProof.value = PassiveProofPrompt(_lastLevel, newLevel)
                }
            }
        }
        _lastTotal = s.totalScore
        _lastLevel = ProofEngine.rankLevelOf(s.totalScore, s)
    }

    /** 结束自证（成功/失败）：重置状态 + 加/扣分 + 弹结果窗（防重入）。
     *  成功时 endRank=结束时的实际阶位 y（恭喜登上 y）；失败时无意义。 */
    private fun finishProof(s: KarmaSettingsEntity, success: Boolean) {
        if (_proofFinishing) return
        _proofFinishing = true
        // 结算本身的奖励加分做一次性豁免：写库后总分增加，若不豁免会立刻再弹一次被动自证
        val delta = if (success) s.proofReward else -s.proofPenalty
        if (delta > 0f) _suppressPassiveProofOnce = true
        val endRank = ProofEngine.rankLevelOf(s.totalScore, s)
        _proofResult.value = ProofResult(success, s.proofStartRankLevel, endRank, s.proofReward, s.proofPenalty)
        viewModelScope.launch {
            try {
                repository.updateAllSettings(s.copy(
                    proofActive = false,
                    proofStartTime = 0L,
                    proofDurationMs = 0L,
                    proofStartRankLevel = 0,
                    proofTargetLevel = 0,
                    proofGuardLevel = 0,
                    proofReward = 0f,
                    proofPenalty = 0f,
                ))
                if (delta != 0f) {
                    repository.addHistoryEntry(delta, if (success) "自证成功" else "自证失败", "record")
                }
            } finally {
                _proofFinishing = false
            }
        }
    }

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
