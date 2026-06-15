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

    val draft: StateFlow<KarmaSettingsEntity> = _draft.asStateFlow()
    val original: StateFlow<KarmaSettingsEntity> = _original.asStateFlow()

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
    fun updateEventPresets(lines: String) {
        val events = lines.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        setDraft(_draft.value.copy(eventPresets = events))
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
        ))
    }

    fun hasChanges(): Boolean = _draft.value != _original.value

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
