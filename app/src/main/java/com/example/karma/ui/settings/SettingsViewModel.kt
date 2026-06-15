package com.example.karma.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.repository.KarmaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel。
 * 读取当前设置 → 在内存中编辑 → 用户点击「保存」时统一写 Room。
 */
class SettingsViewModel(
    private val repository: KarmaRepository,
) : ViewModel() {

    private val _original = MutableStateFlow(KarmaSettingsEntity())  // 数据库中的原始值
    private val _draft = MutableStateFlow(KarmaSettingsEntity())     // 当前编辑中的副本

    val draft: StateFlow<KarmaSettingsEntity> = _draft.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _original.value = settings
                _draft.value = settings   // 初始时 draft = original
            }
        }
    }

    // ===== 通用更新方法 =====
    fun updateDraft(transform: KarmaSettingsEntity.() -> KarmaSettingsEntity) {
        _draft.value = _draft.value.transform()
    }

    // ===== 便捷更新方法 =====
    fun updateScoreAxisFontSize(v: Float) {
        _draft.value = _draft.value.copy(scoreAxisFontSize = v)
    }
    fun updateScoreAxisRange(v: Float) {
        _draft.value = _draft.value.copy(scoreAxisRange = v)
    }

    // ★ 中间刻度区域
    fun updateAxisLabelColor(v: Long) {
        _draft.value = _draft.value.copy(axisLabelColor = v)
    }
    fun updateAxisTickThickness(v: Float) {
        _draft.value = _draft.value.copy(axisTickThickness = v)
    }
    fun updateAxisLabelFontSize(v: Float) {
        _draft.value = _draft.value.copy(axisLabelFontSize = v)
    }
    fun updateAxisDisplayRange(v: Float) {
        _draft.value = _draft.value.copy(axisDisplayRange = v)
    }
    fun updateShowNearbyTicks(v: Boolean) {
        _draft.value = _draft.value.copy(showNearbyTicks = v)
    }
    fun updateNearbyTickRange(v: Float) {
        _draft.value = _draft.value.copy(nearbyTickRange = v)
    }
    fun updateAxisQuarterValue(v: Float) {
        _draft.value = _draft.value.copy(axisQuarterValue = v)
    }

    // 阶位颜色
    fun updateRankColor(index: Int, color: Long) {
        val colors = _draft.value.rankColors.toMutableList()
        if (index in colors.indices) {
            colors[index] = color
            _draft.value = _draft.value.copy(rankColors = colors)
        }
    }

    // ★ 事件列表（来自多行文本框）
    fun updateEventPresets(lines: String) {
        val events = lines.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        _draft.value = _draft.value.copy(eventPresets = events)
    }

    // ★ 历史记录
    fun updateHistoryLineThickness(v: Float) {
        _draft.value = _draft.value.copy(historyLineThickness = v)
    }
    fun updateHistoryDotRadius(v: Float) {
        _draft.value = _draft.value.copy(historyDotRadius = v)
    }

    // ★ 业力衰减
    fun updateDecayEnabled(v: Boolean) {
        _draft.value = _draft.value.copy(decayEnabled = v)
    }
    fun updateDecayTime(hour: Int, minute: Int) {
        _draft.value = _draft.value.copy(decayHour = hour, decayMinute = minute)
    }
    fun updateRankDecayAmount(index: Int, amount: Float) {
        val amounts = _draft.value.rankDecayAmounts.toMutableList()
        if (index in amounts.indices) {
            amounts[index] = amount.coerceIn(0f, 10f)
            _draft.value = _draft.value.copy(rankDecayAmounts = amounts)
        }
    }

    // ===== 保存 / 重置 =====
    fun save() {
        viewModelScope.launch {
            repository.updateAllSettings(_draft.value)
        }
    }

    fun resetToDefaults() {
        _draft.value = KarmaSettingsEntity()
    }

    fun hasChanges(): Boolean = _draft.value != _original.value

    class Factory(private val repository: KarmaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
