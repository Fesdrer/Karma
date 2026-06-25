package com.example.karma.ui.divination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.karma.ui.divination.model.HexagramLine
import com.example.karma.ui.divination.model.OneChange
import com.example.karma.ui.divination.model.YarrowResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class YarrowPhase {
    INTRO,          // 初始动画：50根→取1根旋转→移到顶部
    IDLE,           // 等待用户点击
    SPLITTING,      // 分二动画
    HANGING_ONE,    // 挂一动画
    COUNTING_FOURS, // 揲四动画
    COLLECTING,     // 归奇动画
    MERGING,        // 爻完成后剩余策合并
    LINE_RESULT,    // 短暂显示爻结果
    COMPLETE,       // 六爻完成
}

data class YarrowUiState(
    val changeNumber: Int = 1,
    val totalSticks: Int = 49,
    val leftCount: Int = 0,
    val rightCount: Int = 0,
    val hangOne: Boolean = false,
    val leftRem: Int = 0,
    val rightRem: Int = 0,
    val collectedCount: Int = 0,
    val collectedHistory: List<Int> = emptyList(),
    val remainingSticks: Int = 49,
    val lines: List<HexagramLine> = emptyList(),
    val phase: YarrowPhase = YarrowPhase.INTRO,
    val animProgress: Float = 0f,
    val showResult: Boolean = false,
    val result: YarrowResult? = null,
)

class YarrowViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(YarrowUiState())
    val uiState: StateFlow<YarrowUiState> = _uiState.asStateFlow()

    /** 用户点击 → 用点击的 x 比例作为分界线，执行一整变 */
    fun onSplitTap(xRatio: Float) {
        val state = _uiState.value
        if (state.phase != YarrowPhase.IDLE) return

        val result = performChange(state.totalSticks, xRatio.coerceIn(0.1f, 0.9f))
        _uiState.update {
            it.copy(
                leftCount = result.leftCount,
                rightCount = result.rightCount,
                hangOne = true,
                leftRem = result.leftRem,
                rightRem = result.rightRem,
                collectedCount = result.collected,
                remainingSticks = result.remaining,
                phase = YarrowPhase.SPLITTING,
                animProgress = 0f,
            )
        }
    }

    /** 动画阶段完成 → 推进到下一阶段 */
    fun advancePhase() {
        _uiState.update { s ->
            when (s.phase) {
                YarrowPhase.INTRO -> s.copy(phase = YarrowPhase.IDLE, animProgress = 0f)
                YarrowPhase.SPLITTING -> s.copy(phase = YarrowPhase.HANGING_ONE, animProgress = 0f)
                YarrowPhase.HANGING_ONE -> s.copy(phase = YarrowPhase.COUNTING_FOURS, animProgress = 0f)
                YarrowPhase.COUNTING_FOURS -> s.copy(phase = YarrowPhase.COLLECTING, animProgress = 0f)
                YarrowPhase.COLLECTING -> {
                    // 三变完成？
                    val changesInLine = s.changeNumber % 3
                    if (changesInLine == 0) {
                        // 三变完成 → 算一爻
                        val v = s.remainingSticks / 4 // 6/7/8/9
                        val line = HexagramLine(
                            value = v,
                            isYang = v % 2 != 0,
                            isChanging = v == 6 || v == 9,
                        )
                        val newLines = s.lines + line
                        if (newLines.size >= 6) {
                            // 六爻全 → 完成
                            s.copy(
                                lines = newLines,
                                collectedHistory = s.collectedHistory + s.collectedCount,
                                phase = YarrowPhase.COMPLETE,
                                animProgress = 0f,
                                showResult = true,
                                result = YarrowResult(newLines),
                            )
                        } else {
                            // 还有下一爻 → 合并
                            s.copy(
                                lines = newLines,
                                collectedHistory = s.collectedHistory + s.collectedCount,
                                phase = YarrowPhase.MERGING,
                                animProgress = 0f,
                            )
                        }
                    } else {
                        // 这一爻还有变 → 回到等待
                        s.copy(
                            collectedHistory = s.collectedHistory + s.collectedCount,
                            totalSticks = s.remainingSticks,
                            leftCount = 0, rightCount = 0,
                            hangOne = false, leftRem = 0, rightRem = 0,
                            collectedCount = 0,
                            phase = YarrowPhase.IDLE,
                            animProgress = 0f,
                            changeNumber = s.changeNumber + 1,
                        )
                    }
                }
                YarrowPhase.MERGING -> {
                    s.copy(
                        totalSticks = 49,  // 新爻重新从49开始
                        leftCount = 0, rightCount = 0,
                        hangOne = false, leftRem = 0, rightRem = 0,
                        collectedCount = 0, collectedHistory = emptyList(),
                        phase = YarrowPhase.IDLE, animProgress = 0f,
                        changeNumber = s.changeNumber + 1,
                    )
                }
                else -> s
            }
        }
    }

    fun reset() {
        _uiState.value = YarrowUiState(phase = YarrowPhase.INTRO)
    }

    // ====== 算法 ======

    private fun performChange(total: Int, splitRatio: Float): OneChange {
        val left = (total * splitRatio).toInt().coerceIn(1, total - 2)
        val right = total - left
        val hang = 1
        val ra = right - 1
        val lr0 = left % 4;  val lr = if (lr0 == 0) 4 else lr0
        val rr0 = ra % 4;    val rr = if (rr0 == 0) 4 else rr0
        val col = hang + lr + rr
        return OneChange(left, right, hang, lr, rr, col, total - col)
    }

    // ====== Factory ======

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = YarrowViewModel() as T
    }
}
