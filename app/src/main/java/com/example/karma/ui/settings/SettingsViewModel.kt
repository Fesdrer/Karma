package com.example.karma.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.repository.KarmaRepository
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    init {
        // 异步加载初始值，不阻塞主线程
        viewModelScope.launch {
            val initial = repository.settings.first()
            _original.value = initial
            // 仅当用户尚未编辑时才覆盖 _draft，防止编辑丢失
            if (!_userEdited) {
                _draft.value = initial
            } else {
                // 用户已在 DB 加载前编辑过，保留编辑但修复关键字段（totalScore 等）
                // 否则 _draft 的 totalScore 仍是 KarmaSettingsEntity() 的 0
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

    // ★ 中间指引线
    fun updateGuideLineWidth(v: Float) {
        setDraft(_draft.value.copy(guideLineWidth = v))
    }
    fun updateGuideLineColor(v: Long) {
        setDraft(_draft.value.copy(guideLineColor = v))
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
            val cal = Calendar.getInstance()
            val today = String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH))
            setDraft(_draft.value.copy(decayEnabled = true, lastDecayDate = today))
            // 函数 F：打开衰减开关时也执行衰减检查
            viewModelScope.launch {
                repository.updateAllSettings(_draft.value)
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

    fun updateRankNames(lines: String) {
        val inputNames = lines.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val names = _draft.value.rankNames.toMutableList()
        for (i in 0 until names.size) {
            val name = inputNames.getOrElse(i) { names.getOrElse(i) { "?" } }
            if (i < names.size) names[i] = name
        }
        setDraft(_draft.value.copy(rankNames = names))
    }

    // ===== 保存 / 重置 =====
    fun save() {
        viewModelScope.launch {
            repository.updateAllSettings(_draft.value)
            _original.value = _draft.value  // 同步 original，hasChanges 恢复正常
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
        ))
    }

    // ===== 阶位增删 =====

    fun addRank() {
        val d = _draft.value
        val newNames = d.rankNames + "新阶位"
        val newColors = d.rankColors + (d.rankColors.lastOrNull() ?: 0xFFFFFFFF)
        val newDecays = d.rankDecayAmounts + (d.rankDecayAmounts.lastOrNull() ?: 3f)
        val lastThreshold = d.rankThresholds.lastOrNull() ?: 360f
        val newThresholds = d.rankThresholds + (lastThreshold + 50f)
        setDraft(d.copy(
            rankNames = newNames,
            rankColors = newColors,
            rankDecayAmounts = newDecays,
            rankThresholds = newThresholds,
        ))
    }

    fun deleteRank(index: Int) {
        val d = _draft.value
        if (d.rankNames.size <= 1) return
        val newNames = d.rankNames.toMutableList().apply { removeAt(index) }
        val newColors = d.rankColors.toMutableList().apply { removeAt(index) }
        val newDecays = d.rankDecayAmounts.toMutableList().apply { removeAt(index) }
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
            rankThresholds = newThresholds,
        ))
        if (newNames.size <= 1) _deleteMode.value = false
    }

    fun toggleDeleteMode() {
        if (_draft.value.rankNames.size > 1) {
            _deleteMode.value = !_deleteMode.value
        } else {
            _deleteMode.value = false
        }
    }

    fun hasChanges(): Boolean = _draft.value != _original.value

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
