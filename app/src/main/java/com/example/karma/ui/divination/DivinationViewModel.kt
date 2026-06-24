package com.example.karma.ui.divination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.ui.divination.model.FortuneLevel
import com.example.karma.ui.divination.model.PalaceIndex
import com.example.karma.ui.divination.model.PalaceRevelation
import com.example.karma.ui.divination.model.ShiChen
import com.example.karma.ui.divination.model.XiaoLiuRenPalaces
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 输入模式 */
enum class InputMode { TRADITIONAL, ARBITRARY }

/** 动画阶段 */
enum class AnimationPhase {
    IDLE,
    COUNTING_MONTH,
    MONTH_PAUSE,
    COUNTING_DAY,
    DAY_PAUSE,
    COUNTING_HOUR,
    RESULT_GLOW,
    COMPLETE,
}

/** UI 状态 */
data class DivinationUiState(
    // 输入
    val inputMode: InputMode = InputMode.TRADITIONAL,
    val month: Int = 1,
    val day: Int = 1,
    val shiChen: ShiChen = ShiChen.ZI,
    val number1: String = "",
    val number2: String = "",
    val number3: String = "",
    // 动画
    val animationPhase: AnimationPhase = AnimationPhase.IDLE,
    val isAnimating: Boolean = false,
    // 推算结果
    val resultPalace: PalaceRevelation? = null,
    val fullPath: List<Int> = emptyList(),
    // 错误
    val errorMessage: String? = null,
)

/** 推算结果（内部） */
data class CalculationResult(
    val finalPalace: PalaceRevelation,
    val fullPath: List<Int>,
    val monthEnd: Int,
    val dayEnd: Int,
    val hourEnd: Int,
)

class DivinationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DivinationUiState())
    val uiState: StateFlow<DivinationUiState> = _uiState.asStateFlow()

    // ====== 输入更新 ======

    fun setInputMode(mode: InputMode) {
        _uiState.update { it.copy(inputMode = mode) }
    }

    fun setMonth(month: Int) {
        _uiState.update { it.copy(month = month.coerceIn(1, 12)) }
    }

    fun setDay(day: Int) {
        _uiState.update { it.copy(day = day.coerceIn(1, 30)) }
    }

    fun setShiChen(sc: ShiChen) {
        _uiState.update { it.copy(shiChen = sc) }
    }

    fun setNumber1(value: String) {
        _uiState.update { it.copy(number1 = value.filter { c -> c.isDigit() }) }
    }

    fun setNumber2(value: String) {
        _uiState.update { it.copy(number2 = value.filter { c -> c.isDigit() }) }
    }

    fun setNumber3(value: String) {
        _uiState.update { it.copy(number3 = value.filter { c -> c.isDigit() }) }
    }

    // ====== 核心推算 ======

    fun startDivination() {
        val state = _uiState.value
        val month: Int
        val day: Int
        val hour: Int

        when (state.inputMode) {
            InputMode.TRADITIONAL -> {
                month = state.month
                day = state.day
                hour = state.shiChen.index
            }
            InputMode.ARBITRARY -> {
                val n1 = state.number1.toIntOrNull()
                val n2 = state.number2.toIntOrNull()
                val n3 = state.number3.toIntOrNull()
                if (n1 == null || n2 == null || n3 == null || n1 <= 0 || n2 <= 0 || n3 <= 0) {
                    _uiState.update { it.copy(errorMessage = "请输入三个正整数") }
                    return
                }
                month = n1
                day = n2
                hour = n3
            }
        }

        val result = calculate(month, day, hour)
        _uiState.update {
            it.copy(
                resultPalace = result.finalPalace,
                fullPath = result.fullPath,
                animationPhase = AnimationPhase.COUNTING_MONTH,
                isAnimating = true,
                errorMessage = null,
            )
        }
    }

    /**
     * 小六壬核心算法
     *
     * 从大安(索引0)起正月，顺时针数至目标月
     * 从月落点起初一，顺时针数至目标日
     * 从日落点起子时，顺时针数至目标时辰
     */
    private fun calculate(month: Int, day: Int, hour: Int): CalculationResult {
        val afterMonth = (month - 1) % 6
        val monthPath = generatePath(0, month)

        val afterDay = (afterMonth + day - 1) % 6
        val dayPath = generatePath(afterMonth, day)

        val afterHour = (afterDay + hour - 1) % 6
        val hourPath = generatePath(afterDay, hour)

        // 将日步和时步的路径偏移到月步之后（避免重复起始点）
        val shiftedDayPath = if (dayPath.isNotEmpty()) dayPath.drop(1) else emptyList()
        val shiftedHourPath = if (hourPath.isNotEmpty()) hourPath.drop(1) else emptyList()

        return CalculationResult(
            finalPalace = XiaoLiuRenPalaces.getPalaceByIndex(afterHour),
            fullPath = monthPath + shiftedDayPath + shiftedHourPath,
            monthEnd = afterMonth,
            dayEnd = afterDay,
            hourEnd = afterHour,
        )
    }

    /**
     * 生成从 startIndex 开始、顺时针移动 steps 步的宫索引序列
     */
    private fun generatePath(startIndex: Int, steps: Int): List<Int> {
        if (steps <= 0) return listOf(startIndex)
        return (0 until steps).map { (startIndex + it) % 6 }
    }

    // ====== 动画回调 ======

    fun onPhaseComplete(phase: AnimationPhase) {
        _uiState.update {
            when (phase) {
                AnimationPhase.COUNTING_MONTH -> it.copy(animationPhase = AnimationPhase.MONTH_PAUSE)
                AnimationPhase.COUNTING_DAY -> it.copy(animationPhase = AnimationPhase.DAY_PAUSE)
                AnimationPhase.COUNTING_HOUR -> it.copy(animationPhase = AnimationPhase.RESULT_GLOW)
                AnimationPhase.RESULT_GLOW -> it.copy(
                    animationPhase = AnimationPhase.COMPLETE,
                    isAnimating = false,
                )
                else -> it
            }
        }
    }

    // ====== 重置 ======

    fun reset() {
        _uiState.value = DivinationUiState()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ====== Factory ======

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DivinationViewModel() as T
        }
    }
}
