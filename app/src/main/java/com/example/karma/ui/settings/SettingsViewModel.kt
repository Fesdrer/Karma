package com.example.karma.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.local.entity.DailyMustDoDeed
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.repository.KarmaRepository
import java.util.Calendar
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val _addMode = MutableStateFlow(false)
    val addMode: StateFlow<Boolean> = _addMode.asStateFlow()

    // 负阶增删模式（独立于正阶；放 ViewModel 以便切换分类页后保持状态）
    private val _negativeAddMode = MutableStateFlow(false)
    val negativeAddMode: StateFlow<Boolean> = _negativeAddMode.asStateFlow()
    private val _negativeDeleteMode = MutableStateFlow(false)
    val negativeDeleteMode: StateFlow<Boolean> = _negativeDeleteMode.asStateFlow()

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

    // ★ 事件列表（来自多行文本框）
    fun updateGoodDeedPresets(lines: String) {
        val events = lines.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        setDraft(_draft.value.copy(goodDeedPresets = events))
    }

    fun updateBadDeedPresets(lines: String) {
        val events = lines.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        setDraft(_draft.value.copy(badDeedPresets = events))
    }

    fun updateGoodResultPresets(lines: String) {
        val events = lines.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        setDraft(_draft.value.copy(goodResultPresets = events))
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
                _original.value = _draft.value  // 同步 original，hasChanges 恢复正常
            }
        }
    }

    fun resetToDefaults() {
        // 保留 totalScore 和 lastDecayDate 不清零，其余恢复默认
        val current = _draft.value
        setDraft(KarmaSettingsEntity().copy(
            totalScore = current.totalScore,
            lastDecayDate = current.lastDecayDate,
            // 保留三个事件列表，不被默认值覆盖
            goodDeedPresets = current.goodDeedPresets,
            badDeedPresets = current.badDeedPresets,
            goodResultPresets = current.goodResultPresets,
            // 保留每日必做设置
            dailyMustDoDeeds = current.dailyMustDoDeeds,
        ))
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
