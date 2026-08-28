package com.example.karma.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.local.entity.DailyMustDoDeed
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.repository.KarmaRepository
import java.util.Calendar
import kotlin.math.abs
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 选择性重置的分组：设置页各分类 + 独立数据项（历史记录在保存时清空，不属于设置草稿）。 */
enum class ResetGroup(val label: String) {
    APPEARANCE("外观与图表"),
    POSITIVE_RANKS("正阶位体系"),
    NEGATIVE_RANKS("负阶位体系"),
    EVENTS("事件管理"),
    DAILY_MUST_DO("每日必做"),
    MECHANICS("业力机制"),
    SPLASH("启动画面"),
    BETS("誓约"),
    TOTAL_SCORE("业力总分"),
    HISTORY("历史记录"),
    TIMER("计时状态"),
    DIVINATION("今日占卜次数"),
}

/**
 * 设置页 ViewModel。
 * 读取当前设置 → 在内存中编辑 → 用户点击「保存」时统一写 Room。
 */
class SettingsViewModel(
    private val repository: KarmaRepository,
) : ViewModel() {

    private val _original = MutableStateFlow(KarmaSettingsEntity())  // 上次保存时的值（用于 hasChanges）
    private val _draft = MutableStateFlow(KarmaSettingsEntity())     // 当前编辑中的副本
    private var _userEdited = false  // 标记用户是否已编辑过，防止加载覆盖
    private val _deleteMode = MutableStateFlow(false)

    val draft: StateFlow<KarmaSettingsEntity> = _draft.asStateFlow()
    val original: StateFlow<KarmaSettingsEntity> = _original.asStateFlow()
    val deleteMode: StateFlow<Boolean> = _deleteMode.asStateFlow()

    // 选择性重置：勾选了「历史记录」分组时置 true，保存时清空历史表（历史不在设置草稿里）
    private val _pendingClearHistory = MutableStateFlow(false)
    val pendingClearHistory: StateFlow<Boolean> = _pendingClearHistory.asStateFlow()

    private val _addMode = MutableStateFlow(false)
    val addMode: StateFlow<Boolean> = _addMode.asStateFlow()

    // 负阶增删模式（独立于正阶；放 ViewModel 以便切换分类页后保持状态）
    private val _negativeAddMode = MutableStateFlow(false)
    val negativeAddMode: StateFlow<Boolean> = _negativeAddMode.asStateFlow()
    private val _negativeDeleteMode = MutableStateFlow(false)
    val negativeDeleteMode: StateFlow<Boolean> = _negativeDeleteMode.asStateFlow()

    // 事件（善业/恶业/善果）增删与排序模式（各分类独立，切换分类页后保持）
    private val _goodDeedAddMode = MutableStateFlow(false)
    val goodDeedAddMode: StateFlow<Boolean> = _goodDeedAddMode.asStateFlow()
    private val _goodDeedDeleteMode = MutableStateFlow(false)
    val goodDeedDeleteMode: StateFlow<Boolean> = _goodDeedDeleteMode.asStateFlow()

    private val _badDeedAddMode = MutableStateFlow(false)
    val badDeedAddMode: StateFlow<Boolean> = _badDeedAddMode.asStateFlow()
    private val _badDeedDeleteMode = MutableStateFlow(false)
    val badDeedDeleteMode: StateFlow<Boolean> = _badDeedDeleteMode.asStateFlow()

    private val _goodResultAddMode = MutableStateFlow(false)
    val goodResultAddMode: StateFlow<Boolean> = _goodResultAddMode.asStateFlow()
    private val _goodResultDeleteMode = MutableStateFlow(false)
    val goodResultDeleteMode: StateFlow<Boolean> = _goodResultDeleteMode.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = repository.settings.first()
            _original.value = initial
            if (!_userEdited) {
                _draft.value = initial
            } else {
                setDraft(_draft.value.copy(
                    totalScore = initial.totalScore,
                    lastDecayDate = initial.lastDecayDate,
                ))
            }
        }
    }

    // ===== 通用更新方法 =====
    /** 标记编辑并写 _draft，防止异步初始加载覆盖用户编辑 */
    private fun setDraft(value: KarmaSettingsEntity) {
        _userEdited = true
        _draft.value = value
    }

    fun updateDraft(transform: KarmaSettingsEntity.() -> KarmaSettingsEntity) {
        setDraft(_draft.value.transform())
    }

    // ===== 便捷更新方法 =====
    fun updateScoreAxisFontSize(v: Float) {
        setDraft(_draft.value.copy(scoreAxisFontSize = v))
    }
    fun updateScoreAxisRangeMin(v: Float) {
        setDraft(_draft.value.copy(scoreAxisRangeMin = v))
    }
    fun updateScoreAxisRangeMax(v: Float) {
        setDraft(_draft.value.copy(scoreAxisRangeMax = v))
    }

    // ★ 中间刻度区域
    fun updateAxisLabelColor(v: Long) {
        setDraft(_draft.value.copy(axisLabelColor = v))
    }
    fun updateAxisTickThickness(v: Float) {
        setDraft(_draft.value.copy(axisTickThickness = v))
    }
    fun updateAxisLabelFontSize(v: Float) {
        setDraft(_draft.value.copy(axisLabelFontSize = v))
    }
    fun updateAxisDisplayRange(v: Float) {
        setDraft(_draft.value.copy(axisDisplayRange = v))
    }
    fun updateShowNearbyTicks(v: Boolean) {
        setDraft(_draft.value.copy(showNearbyTicks = v))
    }
    fun updateNearbyTickRange(v: Float) {
        setDraft(_draft.value.copy(nearbyTickRange = v))
    }
    fun updateAxisQuarterValue(v: Float) {
        setDraft(_draft.value.copy(axisQuarterValue = v))
    }

    // 阶位颜色
    fun updateRankColor(index: Int, color: Long) {
        val colors = _draft.value.rankColors.toMutableList()
        if (index in colors.indices) {
            colors[index] = color
            setDraft(_draft.value.copy(rankColors = colors))
        }
    }

    // ★ 事件列表（v3.13 起：逐条编辑，替代原多行文本框）
    // 名称修改；善业改名时同步每日必做条目（dailyMustDoDeeds 按名称匹配）
    fun updateGoodDeedName(index: Int, name: String) {
        val names = _draft.value.goodDeedPresets.toMutableList()
        if (index !in names.indices) return
        val oldName = names[index]
        names[index] = name
        val deeds = _draft.value.dailyMustDoDeeds.map {
            if (it.name == oldName) it.copy(name = name) else it
        }
        setDraft(_draft.value.copy(goodDeedPresets = names, dailyMustDoDeeds = deeds))
    }

    fun updateBadDeedName(index: Int, name: String) {
        val names = _draft.value.badDeedPresets.toMutableList()
        if (index !in names.indices) return
        names[index] = name
        setDraft(_draft.value.copy(badDeedPresets = names))
    }

    fun updateGoodResultName(index: Int, name: String) {
        val names = _draft.value.goodResultPresets.toMutableList()
        if (index !in names.indices) return
        names[index] = name
        setDraft(_draft.value.copy(goodResultPresets = names))
    }

    // 默认分数：设置页输入正数，善业存正分，恶业/善果存负分（实际扣/加分）
    fun updateGoodDeedDefaultScore(index: Int, value: Float) {
        val scores = _draft.value.goodDeedDefaultScores.toMutableList()
        if (index in scores.indices) {
            scores[index] = abs(value)
            setDraft(_draft.value.copy(goodDeedDefaultScores = scores))
        }
    }

    fun updateBadDeedDefaultScore(index: Int, value: Float) {
        val scores = _draft.value.badDeedDefaultScores.toMutableList()
        if (index in scores.indices) {
            scores[index] = -abs(value)
            setDraft(_draft.value.copy(badDeedDefaultScores = scores))
        }
    }

    fun updateGoodResultDefaultScore(index: Int, value: Float) {
        val scores = _draft.value.goodResultDefaultScores.toMutableList()
        if (index in scores.indices) {
            scores[index] = -abs(value)
            setDraft(_draft.value.copy(goodResultDefaultScores = scores))
        }
    }

    // ===== 事件增删（同阶位：+ 模式每行出现 +，− 模式每行出现 ×） =====

    private fun addEventAfter(
        names: List<String>,
        scores: List<Float>,
        name: String,
        score: Float,
        index: Int,
    ): Pair<List<String>, List<Float>> {
        val newNames = names.toMutableList().apply { add(index + 1, name) }
        val newScores = scores.toMutableList().apply { add(index + 1, score) }
        return newNames to newScores
    }

    fun addGoodDeedAfter(index: Int) {
        val d = _draft.value
        val (names, scores) = addEventAfter(d.goodDeedPresets, d.goodDeedDefaultScores, "新事件", 1f, index)
        setDraft(d.copy(goodDeedPresets = names, goodDeedDefaultScores = scores))
    }

    fun addBadDeedAfter(index: Int) {
        val d = _draft.value
        val (names, scores) = addEventAfter(d.badDeedPresets, d.badDeedDefaultScores, "新事件", -1f, index)
        setDraft(d.copy(badDeedPresets = names, badDeedDefaultScores = scores))
    }

    fun addGoodResultAfter(index: Int) {
        val d = _draft.value
        val (names, scores) = addEventAfter(d.goodResultPresets, d.goodResultDefaultScores, "新事件", -1f, index)
        setDraft(d.copy(goodResultPresets = names, goodResultDefaultScores = scores))
    }

    /** 表头（+）在列表最上面插入一个事件（解决"不能加在最上面/删光后无法添加"）。 */
    fun addGoodDeedAtTop() {
        val d = _draft.value
        setDraft(d.copy(
            goodDeedPresets = listOf("新事件") + d.goodDeedPresets,
            goodDeedDefaultScores = listOf(1f) + d.goodDeedDefaultScores,
        ))
    }

    fun addBadDeedAtTop() {
        val d = _draft.value
        setDraft(d.copy(
            badDeedPresets = listOf("新事件") + d.badDeedPresets,
            badDeedDefaultScores = listOf(-1f) + d.badDeedDefaultScores,
        ))
    }

    fun addGoodResultAtTop() {
        val d = _draft.value
        setDraft(d.copy(
            goodResultPresets = listOf("新事件") + d.goodResultPresets,
            goodResultDefaultScores = listOf(-1f) + d.goodResultDefaultScores,
        ))
    }

    /** 删除善业事件；同步删除同名每日必做条目（applyDecay 直接遍历 dailyMustDoDeeds，不校验事件是否存在）。 */
    fun deleteGoodDeed(index: Int) {
        val d = _draft.value
        if (index !in d.goodDeedPresets.indices) return
        val name = d.goodDeedPresets[index]
        val newNames = d.goodDeedPresets.toMutableList().apply { removeAt(index) }
        val newScores = d.goodDeedDefaultScores.toMutableList().apply { removeAt(index) }
        setDraft(d.copy(
            goodDeedPresets = newNames,
            goodDeedDefaultScores = newScores,
            dailyMustDoDeeds = d.dailyMustDoDeeds.filterNot { it.name == name },
        ))
        if (newNames.isEmpty()) _goodDeedDeleteMode.value = false
    }

    fun deleteBadDeed(index: Int) {
        val d = _draft.value
        if (index !in d.badDeedPresets.indices) return
        val newNames = d.badDeedPresets.toMutableList().apply { removeAt(index) }
        val newScores = d.badDeedDefaultScores.toMutableList().apply { removeAt(index) }
        setDraft(d.copy(badDeedPresets = newNames, badDeedDefaultScores = newScores))
        if (newNames.isEmpty()) _badDeedDeleteMode.value = false
    }

    fun deleteGoodResult(index: Int) {
        val d = _draft.value
        if (index !in d.goodResultPresets.indices) return
        val newNames = d.goodResultPresets.toMutableList().apply { removeAt(index) }
        val newScores = d.goodResultDefaultScores.toMutableList().apply { removeAt(index) }
        setDraft(d.copy(goodResultPresets = newNames, goodResultDefaultScores = newScores))
        if (newNames.isEmpty()) _goodResultDeleteMode.value = false
    }

    // ===== 事件拖拽排序（名称与默认分数成对移动） =====

    private fun moveEvent(
        names: List<String>,
        scores: List<Float>,
        from: Int,
        to: Int,
    ): Pair<List<String>, List<Float>> {
        if (from !in names.indices || to !in names.indices || from == to) return names to scores
        val newNames = names.toMutableList().apply { add(to, removeAt(from)) }
        val newScores = scores.toMutableList().apply { add(to, removeAt(from)) }
        return newNames to newScores
    }

    fun moveGoodDeed(from: Int, to: Int) {
        val d = _draft.value
        val (names, scores) = moveEvent(d.goodDeedPresets, d.goodDeedDefaultScores, from, to)
        setDraft(d.copy(goodDeedPresets = names, goodDeedDefaultScores = scores))
    }

    fun moveBadDeed(from: Int, to: Int) {
        val d = _draft.value
        val (names, scores) = moveEvent(d.badDeedPresets, d.badDeedDefaultScores, from, to)
        setDraft(d.copy(badDeedPresets = names, badDeedDefaultScores = scores))
    }

    fun moveGoodResult(from: Int, to: Int) {
        val d = _draft.value
        val (names, scores) = moveEvent(d.goodResultPresets, d.goodResultDefaultScores, from, to)
        setDraft(d.copy(goodResultPresets = names, goodResultDefaultScores = scores))
    }

    // ===== 事件加/减模式切换（同阶位：互斥） =====

    fun toggleGoodDeedAddMode() {
        _goodDeedAddMode.value = !_goodDeedAddMode.value
        if (_goodDeedAddMode.value) _goodDeedDeleteMode.value = false
    }

    fun toggleGoodDeedDeleteMode() {
        _goodDeedDeleteMode.value = !_goodDeedDeleteMode.value
        if (_goodDeedDeleteMode.value) _goodDeedAddMode.value = false
    }

    fun toggleBadDeedAddMode() {
        _badDeedAddMode.value = !_badDeedAddMode.value
        if (_badDeedAddMode.value) _badDeedDeleteMode.value = false
    }

    fun toggleBadDeedDeleteMode() {
        _badDeedDeleteMode.value = !_badDeedDeleteMode.value
        if (_badDeedDeleteMode.value) _badDeedAddMode.value = false
    }

    fun toggleGoodResultAddMode() {
        _goodResultAddMode.value = !_goodResultAddMode.value
        if (_goodResultAddMode.value) _goodResultDeleteMode.value = false
    }

    fun toggleGoodResultDeleteMode() {
        _goodResultDeleteMode.value = !_goodResultDeleteMode.value
        if (_goodResultDeleteMode.value) _goodResultAddMode.value = false
    }

    // ★ 中间光点
    fun updateDotColor(v: Long) {
        setDraft(_draft.value.copy(dotColor = v))
    }

    // ★ 历史记录
    fun updateHistoryLineThickness(v: Float) {
        setDraft(_draft.value.copy(historyLineThickness = v))
    }
    fun updateHistoryDotRadius(v: Float) {
        setDraft(_draft.value.copy(historyDotRadius = v))
    }

    // ★ 业力衰减
    fun updateDecayEnabled(v: Boolean) {
        if (v) {
            // 日期A = 今天（每次打开开关都从今天开始）
            val today = KarmaRepository.formatDate(System.currentTimeMillis())
            setDraft(_draft.value.copy(decayEnabled = true, lastDecayDate = today))
            // 函数 F：打开衰减开关时也执行衰减检查。
            // 只持久化衰减字段（updateDecayFields），不写整个草稿——
            // 否则未点「保存设置」的阶位增删/事件编辑等修改会被意外写入 DB。
            viewModelScope.launch {
                repository.updateDecayFields(decayEnabled = true, lastDecayDate = today)
                val deducted = repository.applyDecay()
                val updated = repository.settings.first()
                _draft.value = _draft.value.copy(
                    totalScore = updated.totalScore,
                    // F 扣了分 → DB 中的日期A已被推进到日期B
                    // F 没扣分 → 保留今天（忽略 DB 可能有的旧值）
                    lastDecayDate = if (deducted > 0f) updated.lastDecayDate else today,
                )
            }
        } else {
            setDraft(_draft.value.copy(decayEnabled = v))
        }
    }
    fun updateDecayTime(hour: Int, minute: Int) {
        setDraft(_draft.value.copy(decayHour = hour, decayMinute = minute))
    }
    fun updateRankDecayAmount(index: Int, amount: Float) {
        val amounts = _draft.value.rankDecayAmounts.toMutableList()
        if (index in amounts.indices) {
            amounts[index] = amount.coerceIn(0f, 10f)
            setDraft(_draft.value.copy(rankDecayAmounts = amounts))
        }
    }

    // ★ 每阶占卜次数（正阶 / 负阶）
    fun updateRankDivinationLimit(index: Int, value: Int) {
        val limits = _draft.value.rankDivinationLimits.toMutableList()
        if (index in limits.indices) {
            limits[index] = value.coerceIn(0, 99)
            setDraft(_draft.value.copy(rankDivinationLimits = limits))
        }
    }
    fun updateNegativeRankDivinationLimit(index: Int, value: Int) {
        val limits = _draft.value.negativeRankDivinationLimits.toMutableList()
        if (index in limits.indices) {
            limits[index] = value.coerceIn(0, 99)
            setDraft(_draft.value.copy(negativeRankDivinationLimits = limits))
        }
    }
    fun updateNegativeRankColor(index: Int, color: Long) {
        val colors = _draft.value.negativeRankColors.toMutableList()
        if (index in colors.indices) {
            colors[index] = color
            setDraft(_draft.value.copy(negativeRankColors = colors))
        }
    }

    fun updateRankThreshold(index: Int, value: Float) {
        val thresholds = _draft.value.rankThresholds.toMutableList()
        if (index in thresholds.indices) {
            thresholds[index] = value
            setDraft(_draft.value.copy(rankThresholds = thresholds))
        }
    }

    fun updateRankName(index: Int, name: String) {
        val names = _draft.value.rankNames.toMutableList()
        if (index in names.indices) {
            names[index] = name
            setDraft(_draft.value.copy(rankNames = names))
        }
    }

    // ★ 负数阶位
    fun updateNegativeRankThreshold(index: Int, value: Float) {
        val thresholds = _draft.value.negativeRankThresholds.toMutableList()
        if (index in thresholds.indices) {
            thresholds[index] = value
            setDraft(_draft.value.copy(negativeRankThresholds = thresholds))
        }
    }

    fun updateNegativeRankName(index: Int, name: String) {
        val names = _draft.value.negativeRankNames.toMutableList()
        if (index in names.indices) {
            names[index] = name
            setDraft(_draft.value.copy(negativeRankNames = names))
        }
    }

    fun updateRankNames(lines: String) {
        val inputNames = lines.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val names = _draft.value.rankNames.toMutableList()
        for (i in 0 until names.size) {
            val name = inputNames.getOrElse(i) { names.getOrElse(i) { "?" } }
            if (i < names.size) names[i] = name
        }
        setDraft(_draft.value.copy(rankNames = names))
    }

    // ===== 运气增幅 =====
    fun updateLuckEnabled(v: Boolean) {
        setDraft(_draft.value.copy(luckEnabled = v))
    }
    fun updateLuckT(v: Float) {
        setDraft(_draft.value.copy(luckT = v))
    }
    fun updateLuckB(v: Float) {
        setDraft(_draft.value.copy(luckB = v))
    }
    fun updateLuckW(v: Float) {
        setDraft(_draft.value.copy(luckW = v))
    }

    // ===== 背景渐变 =====
    fun updateThemeGradientBaseColor(v: Long) {
        setDraft(_draft.value.copy(themeGradientBaseColor = v))
    }
    fun updateThemeGradientAccentColor(v: Long) {
        setDraft(_draft.value.copy(themeGradientAccentColor = v))
    }

    // ===== 每日必做 =====
    fun toggleDailyMustDo(deedName: String, enabled: Boolean) {
        val currentDeeds = _draft.value.dailyMustDoDeeds.toMutableList()
        if (enabled) {
            currentDeeds.add(DailyMustDoDeed(name = deedName, penalty = 1f, vis = 0))
        } else {
            currentDeeds.removeAll { it.name == deedName }
        }
        setDraft(_draft.value.copy(dailyMustDoDeeds = currentDeeds))
    }

    fun updateDailyMustDoPenalty(deedName: String, penalty: Float) {
        val deeds = _draft.value.dailyMustDoDeeds.map { deed ->
            if (deed.name == deedName) deed.copy(penalty = penalty) else deed
        }
        setDraft(_draft.value.copy(dailyMustDoDeeds = deeds))
    }

    fun isDailyMustDo(deedName: String): Boolean =
        _draft.value.dailyMustDoDeeds.any { it.name == deedName }

    fun getDailyMustDoPenalty(deedName: String): Float {
        return _draft.value.dailyMustDoDeeds.find { it.name == deedName }?.penalty ?: 0f
    }

    // ===== 启动经文 =====
    fun updateSplashScripture(text: String) {
        setDraft(_draft.value.copy(splashScripture = text))
    }
    fun updateSplashDuration(sec: Int) {
        setDraft(_draft.value.copy(splashDurationSec = sec.coerceIn(1, 30)))
    }

    // ===== 保存 / 重置 =====
    fun save() {
        viewModelScope.launch {
            // 保存后屏幕会立即 popBackStack → viewModelScope 被取消；
            // 用 NonCancellable 保证写库完整执行，避免写一半被取消导致设置丢失。
            withContext(NonCancellable) {
                repository.updateAllSettings(_draft.value)
                // step 1：如果每日必做 deeds 有变动 → 所有 vis 重置为 0（未做）
                if (_draft.value.dailyMustDoDeeds != _original.value.dailyMustDoDeeds) {
                    repository.resetDailyMustDoVis()
                }
                // step 2：选择性重置勾选了「历史记录」→ 保存时清空历史表
                if (_pendingClearHistory.value) {
                    repository.clearAllHistory()
                    _pendingClearHistory.value = false
                }
                _original.value = _draft.value  // 同步 original，hasChanges 恢复正常
            }
        }
    }

    /**
     * 选择性重置：只把选中的分组恢复默认（仍走草稿机制，点「保存设置」才写库）。
     * 历史记录不在设置草稿里，勾选后置 _pendingClearHistory，保存时由 save() 清空。
     */
    fun resetGroups(groups: Set<ResetGroup>) {
        if (groups.isEmpty()) return
        val current = _draft.value
        val defaults = KarmaSettingsEntity()
        var next = current
        if (ResetGroup.APPEARANCE in groups) next = next.copy(
            scoreAxisFontSize = defaults.scoreAxisFontSize,
            scoreAxisRangeMin = defaults.scoreAxisRangeMin,
            scoreAxisRangeMax = defaults.scoreAxisRangeMax,
            axisLabelColor = defaults.axisLabelColor,
            axisTickThickness = defaults.axisTickThickness,
            axisLabelFontSize = defaults.axisLabelFontSize,
            axisDisplayRange = defaults.axisDisplayRange,
            showNearbyTicks = defaults.showNearbyTicks,
            nearbyTickRange = defaults.nearbyTickRange,
            axisQuarterValue = defaults.axisQuarterValue,
            guideLineWidth = defaults.guideLineWidth,
            dotColor = defaults.dotColor,
            historyLineThickness = defaults.historyLineThickness,
            historyDotRadius = defaults.historyDotRadius,
            themeGradientBaseColor = defaults.themeGradientBaseColor,
            themeGradientAccentColor = defaults.themeGradientAccentColor,
            scorePresets = defaults.scorePresets,
        )
        if (ResetGroup.POSITIVE_RANKS in groups) next = next.copy(
            rankColors = defaults.rankColors,
            rankThresholds = defaults.rankThresholds,
            rankNames = defaults.rankNames,
            rankDecayAmounts = defaults.rankDecayAmounts,
            rankDivinationLimits = defaults.rankDivinationLimits,
        )
        if (ResetGroup.NEGATIVE_RANKS in groups) next = next.copy(
            negativeRankNames = defaults.negativeRankNames,
            negativeRankThresholds = defaults.negativeRankThresholds,
            negativeRankColors = defaults.negativeRankColors,
            negativeRankDivinationLimits = defaults.negativeRankDivinationLimits,
        )
        if (ResetGroup.EVENTS in groups) next = next.copy(
            goodDeedPresets = defaults.goodDeedPresets,
            badDeedPresets = defaults.badDeedPresets,
            goodResultPresets = defaults.goodResultPresets,
            goodDeedDefaultScores = defaults.goodDeedDefaultScores,
            badDeedDefaultScores = defaults.badDeedDefaultScores,
            goodResultDefaultScores = defaults.goodResultDefaultScores,
        )
        if (ResetGroup.DAILY_MUST_DO in groups) next = next.copy(
            dailyMustDoDeedNames = defaults.dailyMustDoDeedNames,
            dailyMustDoDeedPenalties = defaults.dailyMustDoDeedPenalties,
            dailyMustDoLastDate = defaults.dailyMustDoLastDate,
            dailyMustDoDoneSet = defaults.dailyMustDoDoneSet,
            dailyMustDoDeeds = defaults.dailyMustDoDeeds,
        )
        if (ResetGroup.MECHANICS in groups) next = next.copy(
            decayEnabled = defaults.decayEnabled,
            decayHour = defaults.decayHour,
            decayMinute = defaults.decayMinute,
            lastDecayDate = defaults.lastDecayDate,
            luckEnabled = defaults.luckEnabled,
            luckT = defaults.luckT,
            luckB = defaults.luckB,
            luckW = defaults.luckW,
        )
        if (ResetGroup.SPLASH in groups) next = next.copy(
            splashScripture = defaults.splashScripture,
            splashDurationSec = defaults.splashDurationSec,
        )
        if (ResetGroup.BETS in groups) next = next.copy(bets = defaults.bets)
        if (ResetGroup.TOTAL_SCORE in groups) next = next.copy(totalScore = defaults.totalScore)
        if (ResetGroup.TIMER in groups) next = next.copy(
            timerStatus = defaults.timerStatus,
            timerStartElapsed = defaults.timerStartElapsed,
            timerResumeElapsed = defaults.timerResumeElapsed,
            timerAccumulatedMs = defaults.timerAccumulatedMs,
            timerSelectedScore = defaults.timerSelectedScore,
            timerSelectedEvent = defaults.timerSelectedEvent,
        )
        if (ResetGroup.DIVINATION in groups) next = next.copy(
            divinationDate = defaults.divinationDate,
            divinationCount = defaults.divinationCount,
        )
        setDraft(next)
        if (ResetGroup.HISTORY in groups) {
            _pendingClearHistory.value = true
        }
    }

    // ===== 阶位增删 =====

    fun addRank() {
        val d = _draft.value
        val newNames = d.rankNames + "新阶位"
        val newColors = d.rankColors + (d.rankColors.lastOrNull() ?: 0xFFFFFFFF)
        val newDecays = d.rankDecayAmounts + (d.rankDecayAmounts.lastOrNull() ?: 3f)
        val newLimits = d.rankDivinationLimits + (d.rankDivinationLimits.lastOrNull() ?: 2)
        val lastThreshold = d.rankThresholds.lastOrNull() ?: 360f
        val newThresholds = d.rankThresholds + (lastThreshold + 50f)
        setDraft(d.copy(
            rankNames = newNames,
            rankColors = newColors,
            rankDecayAmounts = newDecays,
            rankDivinationLimits = newLimits,
            rankThresholds = newThresholds,
        ))
    }

    fun deleteRank(index: Int) {
        val d = _draft.value
        // index 越界（快速连点时 UI 索引尚未重组、可能已过期）直接忽略，防止 removeAt 越界闪退
        if (d.rankNames.size <= 1 || index !in d.rankNames.indices) return
        val newNames = d.rankNames.toMutableList().apply { removeAt(index) }
        val newColors = d.rankColors.toMutableList().apply { removeAt(index) }
        val newDecays = d.rankDecayAmounts.toMutableList().apply { removeAt(index) }
        val newLimits = d.rankDivinationLimits.toMutableList().apply { removeAt(index) }
        val newThresholds = d.rankThresholds.toMutableList()
        // 删除第 index 个阶位，对应的阈值也需要调整
        if (index < newThresholds.size) {
            newThresholds.removeAt(index)
        } else if (newThresholds.isNotEmpty()) {
            newThresholds.removeAt(newThresholds.lastIndex)
        }
        setDraft(d.copy(
            rankNames = newNames,
            rankColors = newColors,
            rankDecayAmounts = newDecays,
            rankDivinationLimits = newLimits,
            rankThresholds = newThresholds,
        ))
        if (newNames.size <= 1) _deleteMode.value = false
    }

    /** 按下 + 号，切换「插入模式」：每个阶位右上角出现 +，点击可在该阶位下方插入新阶位。 */
    fun toggleAddMode() {
        _addMode.value = !_addMode.value
        if (_addMode.value) _deleteMode.value = false
    }

    /** 在 index 阶位下方插入一个新阶位。 */
    fun addRankAfter(index: Int) {
        val d = _draft.value
        val newNames = d.rankNames.toMutableList().apply { add(index + 1, "新阶位") }
        val newColors = d.rankColors.toMutableList().apply { add(index + 1, d.rankColors.getOrElse(index) { 0xFFFFFFFF }) }
        val newDecays = d.rankDecayAmounts.toMutableList().apply { add(index + 1, d.rankDecayAmounts.getOrElse(index) { 3f }) }
        val newLimits = d.rankDivinationLimits.toMutableList().apply { add(index + 1, d.rankDivinationLimits.getOrElse(index) { 2 }) }
        val newThresholds = d.rankThresholds.toMutableList()
        val insertPos = index + 1
        if (insertPos < d.rankThresholds.size) {
            // 在两个已有阈值之间插入中点
            val prev = d.rankThresholds[insertPos - 1]
            val next = d.rankThresholds[insertPos]
            newThresholds.add(insertPos, (prev + next) / 2f)
        } else {
            // 在最后一个阈值之后追加：新边界永远在列表末尾（追加）。
            // 不能用 add(insertPos, ...)：在最后一个阶位下方插入时 insertPos = 阶位数，
            // 而阈值列表只有 阶位数-1 个元素，add(阶位数, ...) 会 IndexOutOfBoundsException（闪退）。
            val prev = d.rankThresholds.lastOrNull() ?: 50f
            newThresholds.add(prev + 50f)
        }
        setDraft(d.copy(
            rankNames = newNames,
            rankColors = newColors,
            rankDecayAmounts = newDecays,
            rankDivinationLimits = newLimits,
            rankThresholds = newThresholds,
        ))
    }

    // ===== 负数阶位增删（与正阶同构：阈值 ts[i] 为 -(i+1) 级下限，降序） =====

    /** 在最深一层下方追加新负阶。 */
    fun addNegativeRank() {
        val d = _draft.value
        val lastThreshold = d.negativeRankThresholds.lastOrNull() ?: -10f
        setDraft(d.copy(
            negativeRankNames = d.negativeRankNames + "新负阶",
            negativeRankColors = d.negativeRankColors + 0xFF000000L,
            negativeRankDivinationLimits = d.negativeRankDivinationLimits + 0,
            negativeRankThresholds = d.negativeRankThresholds + (lastThreshold - 10f),
        ))
    }

    /** 在 index 负阶下方（更深处）插入新负阶。 */
    fun addNegativeRankAfter(index: Int) {
        val d = _draft.value
        val newNames = d.negativeRankNames.toMutableList().apply { add(index + 1, "新负阶") }
        val newColors = d.negativeRankColors.toMutableList().apply {
            add(index + 1, d.negativeRankColors.getOrElse(index) { 0xFF000000L })
        }
        val newLimits = d.negativeRankDivinationLimits.toMutableList().apply { add(index + 1, 0) }
        val newThresholds = d.negativeRankThresholds.toMutableList()
        val insertPos = index + 1
        if (insertPos < d.negativeRankThresholds.size) {
            // 在两个已有阈值之间插入中点（均为负数，中点即更细分级）
            val prev = d.negativeRankThresholds[insertPos - 1]
            val next = d.negativeRankThresholds[insertPos]
            newThresholds.add(insertPos, (prev + next) / 2f)
        } else {
            // 在最后一个阈值之后追加（比最深更负 10 分）。
            // 不能用 add(insertPos, ...)：在最后一个负阶下方插入时 insertPos = 负阶数，
            // 而阈值列表只有 负阶数-1 个元素，add(负阶数, ...) 会 IndexOutOfBoundsException（闪退）。
            val prev = d.negativeRankThresholds.lastOrNull() ?: -10f
            newThresholds.add(prev - 10f)
        }
        setDraft(d.copy(
            negativeRankNames = newNames,
            negativeRankColors = newColors,
            negativeRankDivinationLimits = newLimits,
            negativeRankThresholds = newThresholds,
        ))
    }

    fun deleteNegativeRank(index: Int) {
        val d = _draft.value
        // index 越界（快速连点时 UI 索引尚未重组、可能已过期）直接忽略，防止 removeAt 越界闪退
        if (d.negativeRankNames.size <= 1 || index !in d.negativeRankNames.indices) return
        val newNames = d.negativeRankNames.toMutableList().apply { removeAt(index) }
        val newColors = d.negativeRankColors.toMutableList().apply { removeAt(index) }
        val newLimits = d.negativeRankDivinationLimits.toMutableList().apply { removeAt(index) }
        val newThresholds = d.negativeRankThresholds.toMutableList()
        // 删除第 index 个负阶，对应下限（阈值）同步移除
        if (index < newThresholds.size) {
            newThresholds.removeAt(index)
        } else if (newThresholds.isNotEmpty()) {
            newThresholds.removeAt(newThresholds.lastIndex)
        }
        setDraft(d.copy(
            negativeRankNames = newNames,
            negativeRankColors = newColors,
            negativeRankDivinationLimits = newLimits,
            negativeRankThresholds = newThresholds,
        ))
    }

    fun toggleDeleteMode() {
        if (_draft.value.rankNames.size > 1) {
            _deleteMode.value = !_deleteMode.value
            if (_deleteMode.value) _addMode.value = false
        } else {
            _deleteMode.value = false
        }
    }

    /** 负阶 + 模式切换（与正阶 addMode 同构，互斥于负阶删除模式）。 */
    fun toggleNegativeAddMode() {
        _negativeAddMode.value = !_negativeAddMode.value
        if (_negativeAddMode.value) _negativeDeleteMode.value = false
    }

    /** 负阶 − 模式切换（与正阶 deleteMode 同构，互斥于负阶添加模式）。 */
    fun toggleNegativeDeleteMode() {
        _negativeDeleteMode.value = !_negativeDeleteMode.value
        if (_negativeDeleteMode.value) _negativeAddMode.value = false
    }

    fun hasChanges(): Boolean = _draft.value != _original.value

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
